package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeLogisticsProvider;
import com.shop.module.trade.service.provider.TradeLogisticsProviderService;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeLogisticsService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    private final TradeOrderLogService tradeOrderLogService;
    private final TradeLogisticsProviderService tradeLogisticsProviderService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mockShip(Long userId, Long orderId, Map<String, Object> request) {
        return ship(userId, orderId, request, TradeOrderLogService.OPERATOR_USER, userId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adminShip(Long adminId, Long orderId, Map<String, Object> request) {
        return ship(null, orderId, request, TradeOrderLogService.OPERATOR_ADMIN, adminId, false);
    }

    private Map<String, Object> ship(Long userId, Long orderId, Map<String, Object> request,
                                     String operatorType, Long operatorId, boolean allowMockDefaults) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new ServerException(400, "当前订单不能发货");
        }
        String company = TradeRequestUtils.getString(request, "logisticsCompany",
                allowMockDefaults ? "顺丰速运" : "").trim();
        String logisticsNo = TradeRequestUtils.getString(request, "logisticsNo", "");
        if (allowMockDefaults && logisticsNo.isBlank()) {
            logisticsNo = "SF" + System.currentTimeMillis();
        }
        if (company.length() < 2 || company.length() > 64) {
            throw new ServerException(400, "物流公司长度应为 2 至 64 个字符");
        }
        if (!logisticsNo.matches("[A-Za-z0-9-]{4,64}")) {
            throw new ServerException(400, "物流单号仅支持 4 至 64 位字母、数字或连字符");
        }

        Integer fromStatus = order.getStatus();
        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getStatus, 1)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, 2));
        if (orderUpdated != 1) {
            throw new ServerException(409, "订单状态已变更，不能重复发货");
        }

        TradeOrderLogisticsDO logistics = getLogistics(orderId);
        if (logistics == null) {
            logistics = new TradeOrderLogisticsDO();
            logistics.setOrderId(orderId);
        }
        logistics.setLogisticsCompany(company);
        logistics.setLogisticsNo(logisticsNo);
        logistics.setDeliveryTime(LocalDateTime.now());
        if (logistics.getId() == null) {
            tradeOrderLogisticsMapper.insert(logistics);
        } else {
            tradeOrderLogisticsMapper.updateById(logistics);
        }

        order.setStatus(2);
        tradeOrderLogService.recordStatusChanged(order, operatorType, operatorId,
                "SHIP_ORDER", fromStatus, order.getStatus(),
                "物流公司：" + company + "，物流单号：" + logisticsNo);
        return toResp(logistics, order.getStatus());
    }

    public Map<String, Object> query(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        return toResp(getLogistics(orderId), order.getStatus());
    }

    public Map<String, Object> adminQuery(Long orderId) {
        return query(null, orderId);
    }

    public Map<String, Object> getOrderLogisticsInfo(Long orderId, Integer orderStatus) {
        return toResp(getLogistics(orderId), orderStatus);
    }

    private TradeOrderDO getUserOrder(Long userId, Long orderId) {
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, orderId);
        if (userId != null) {
            wrapper.eq(TradeOrderDO::getUserId, userId);
        }
        TradeOrderDO order = tradeOrderMapper.selectOne(wrapper);
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        return order;
    }

    private TradeOrderLogisticsDO getLogistics(Long orderId) {
        return tradeOrderLogisticsMapper.selectOne(new LambdaQueryWrapper<TradeOrderLogisticsDO>()
                .eq(TradeOrderLogisticsDO::getOrderId, orderId)
                .orderByDesc(TradeOrderLogisticsDO::getUpdateTime)
                .last("LIMIT 1"));
    }

    private Map<String, Object> toResp(TradeOrderLogisticsDO logistics, Integer orderStatus) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasLogistics", logistics != null);
        result.put("orderStatus", orderStatus);
        if (logistics == null) {
            result.put("logisticsCompany", "");
            result.put("logisticsNo", "");
            result.put("deliveryTime", "");
            result.put("traces", List.of());
            return result;
        }

        String deliveryTime = logistics.getDeliveryTime() == null ? "" : logistics.getDeliveryTime().format(TIME_FORMATTER);
        result.put("id", logistics.getId());
        result.put("orderId", logistics.getOrderId());
        result.put("logisticsCompany", logistics.getLogisticsCompany());
        result.put("logisticsNo", logistics.getLogisticsNo());
        result.put("deliveryTime", deliveryTime);
        List<Map<String, String>> traces = tradeLogisticsProviderService.query(
                        new TradeLogisticsProvider.LogisticsQuery(
                                logistics.getOrderId(),
                                logistics.getLogisticsCompany(),
                                logistics.getLogisticsNo(),
                                logistics.getDeliveryTime(),
                                orderStatus))
                .stream()
                .map(trace -> Map.of(
                        "time", trace.time() == null ? "" : trace.time().format(TIME_FORMATTER),
                        "text", trace.text()))
                .toList();
        result.put("traces", traces);
        return result;
    }
}
