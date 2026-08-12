package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TradeLogisticsService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, String> LOGISTICS_CODES = Map.ofEntries(
            Map.entry("顺丰速运", "shunfeng"), Map.entry("中通快递", "zhongtong"),
            Map.entry("圆通速递", "yuantong"), Map.entry("韵达快递", "yunda"),
            Map.entry("极兔速递", "jtexpress"), Map.entry("申通快递", "shentong"),
            Map.entry("京东物流", "jd"), Map.entry("邮政 EMS", "ems"));
    private static final Set<String> SUPPORTED_CODES = Set.copyOf(LOGISTICS_CODES.values());

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    private final TradeOrderLogService tradeOrderLogService;
    private final TradeLogisticsProviderService tradeLogisticsProviderService;
    private final ObjectMapper objectMapper;

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
        String logisticsCode = TradeRequestUtils.getString(request, "logisticsCode",
                LOGISTICS_CODES.getOrDefault(company, "")).trim().toLowerCase();
        String logisticsNo = TradeRequestUtils.getString(request, "logisticsNo", "").trim();
        if (allowMockDefaults && logisticsNo.isBlank()) {
            logisticsNo = "SF" + System.currentTimeMillis();
        }
        if (company.length() < 2 || company.length() > 64) {
            throw new ServerException(400, "物流公司长度应为 2 至 64 个字符");
        }
        if (!logisticsNo.matches("[A-Za-z0-9-]{6,32}")) {
            throw new ServerException(400, "物流单号仅支持 6 至 32 位字母、数字或连字符");
        }
        if (!SUPPORTED_CODES.contains(logisticsCode)
                || !logisticsCode.equals(LOGISTICS_CODES.get(company))) {
            throw new ServerException(400, "物流公司与编码不匹配");
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
        logistics.setLogisticsCode(logisticsCode);
        logistics.setLogisticsNo(logisticsNo);
        logistics.setDeliveryTime(LocalDateTime.now());
        logistics.setLastQueryTime(null);
        logistics.setTracesJson(null);
        logistics.setQueryMessage(null);
        if (logistics.getId() == null) {
            tradeOrderLogisticsMapper.insert(logistics);
        } else {
            tradeOrderLogisticsMapper.updateById(logistics);
        }

        order.setStatus(2);
        tradeOrderLogService.recordStatusChanged(order, operatorType, operatorId,
                "SHIP_ORDER", fromStatus, order.getStatus(),
                "物流公司：" + company + "，物流单号：" + logisticsNo);
        return toResp(logistics, order);
    }

    public Map<String, Object> query(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        return toResp(getLogistics(orderId), order);
    }

    public Map<String, Object> adminQuery(Long orderId) {
        return query(null, orderId);
    }

    public Map<String, Object> getOrderLogisticsInfo(Long orderId, Integer orderStatus) {
        return toResp(getLogistics(orderId), getUserOrder(null, orderId));
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

    private Map<String, Object> toResp(TradeOrderLogisticsDO logistics, TradeOrderDO order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasLogistics", logistics != null);
        result.put("orderStatus", order.getStatus());
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
        result.put("logisticsCode", logistics.getLogisticsCode());
        result.put("logisticsNo", logistics.getLogisticsNo());
        result.put("deliveryTime", deliveryTime);
        List<Map<String, String>> cachedTraces = readCachedTraces(logistics.getTracesJson());
        boolean cacheFresh = logistics.getLastQueryTime() != null
                && logistics.getLastQueryTime().isAfter(LocalDateTime.now().minusMinutes(30));
        List<Map<String, String>> traces = cacheFresh && cachedTraces != null
                ? cachedTraces : queryAndCache(logistics, order, cachedTraces);
        result.put("traces", traces);
        result.put("queryMessage", logistics.getQueryMessage() == null ? "" : logistics.getQueryMessage());
        result.put("lastQueryTime", formatTime(logistics.getLastQueryTime()));
        return result;
    }

    private List<Map<String, String>> queryAndCache(
            TradeOrderLogisticsDO logistics, TradeOrderDO order,
            List<Map<String, String>> staleTraces) {
        try {
            List<Map<String, String>> traces = tradeLogisticsProviderService.query(
                            new TradeLogisticsProvider.LogisticsQuery(
                                    logistics.getOrderId(), logistics.getLogisticsCompany(),
                                    logistics.getLogisticsCode(), logistics.getLogisticsNo(),
                                    order.getMobile(), logistics.getDeliveryTime(), order.getStatus()))
                    .stream()
                    .map(trace -> Map.of(
                            "time", trace.time() == null ? "" : trace.time().format(TIME_FORMATTER),
                            "text", trace.text()))
                    .toList();
            LocalDateTime queryTime = LocalDateTime.now();
            String tracesJson = objectMapper.writeValueAsString(traces);
            tradeOrderLogisticsMapper.update(null, new LambdaUpdateWrapper<TradeOrderLogisticsDO>()
                    .eq(TradeOrderLogisticsDO::getId, logistics.getId())
                    .set(TradeOrderLogisticsDO::getLastQueryTime, queryTime)
                    .set(TradeOrderLogisticsDO::getTracesJson, tracesJson)
                    .set(TradeOrderLogisticsDO::getQueryMessage, "查询成功"));
            logistics.setLastQueryTime(queryTime);
            logistics.setTracesJson(tracesJson);
            logistics.setQueryMessage("查询成功");
            return traces;
        } catch (Exception exception) {
            logistics.setQueryMessage(staleTraces == null
                    ? "物流轨迹暂时不可用" : "物流服务暂时不可用，当前显示最近缓存");
            if (staleTraces != null) return staleTraces;
            return List.of();
        }
    }

    private List<Map<String, String>> readCachedTraces(String tracesJson) {
        if (tracesJson == null || tracesJson.isBlank()) return null;
        try {
            return objectMapper.readValue(tracesJson, new TypeReference<>() {});
        } catch (Exception exception) {
            return null;
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }
}
