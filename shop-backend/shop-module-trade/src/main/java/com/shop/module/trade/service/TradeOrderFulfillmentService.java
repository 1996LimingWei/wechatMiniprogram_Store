package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单履约状态流转：支付成功进入「待备货」，后台按备货 → 揽收 → 发货 → 签收推进，
 * 发货必须提供物流单号，异常状态可在发货后回填。
 */
@Service
@RequiredArgsConstructor
public class TradeOrderFulfillmentService {

    public static final int PENDING = 0;
    public static final int PREPARING = 1;
    public static final int AWAITING_PICKUP = 2;
    public static final int SHIPPED = 3;
    public static final int SIGNED = 4;
    public static final int ABNORMAL = 5;

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    private final TradeOrderLogService tradeOrderLogService;
    private final TradeLogisticsService tradeLogisticsService;

    /** 履约状态文案，后台与小程序共用；未支付或未进入履约流程时返回空串。 */
    public static String statusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case PENDING -> "待备货";
            case PREPARING -> "备货中";
            case AWAITING_PICKUP -> "待揽收";
            case SHIPPED -> "已发货";
            case SIGNED -> "已签收";
            case ABNORMAL -> "物流异常";
            default -> "";
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateStatus(Long adminId, Long orderId, Integer target,
                                            String logisticsCompany, String logisticsNo, String logisticsCode,
                                            String remark) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        if (!Integer.valueOf(TradeOrderPayStatus.PAID).equals(order.getPayStatus())) {
            throw new ServerException(400, "订单未支付，不能更新履约状态");
        }
        if (order.getStatus() != null && (order.getStatus() == 4 || order.getStatus() == 5)) {
            throw new ServerException(400, "订单已取消或退款中，不能更新履约状态");
        }
        if (target == null || target < PENDING || target > ABNORMAL) {
            throw new ServerException(400, "履约状态不正确");
        }

        if (target == SHIPPED) {
            return ship(adminId, order, logisticsCompany, logisticsNo, logisticsCode);
        }

        int current = order.getFulfillmentStatus() == null ? PENDING : order.getFulfillmentStatus();
        if (current == target) {
            return currentState(order);
        }
        assertTransition(current, target);

        if (target == SIGNED && order.getStatus() != null && order.getStatus() == 2) {
            tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                    .eq(TradeOrderDO::getId, order.getId())
                    .eq(TradeOrderDO::getStatus, 2)
                    .set(TradeOrderDO::getStatus, 3)
                    .set(TradeOrderDO::getFinishTime, LocalDateTime.now()));
        }
        updateFulfillment(order, target);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_ADMIN, adminId,
                "FULFILLMENT_" + target, order.getStatus(), order.getStatus(),
                "履约状态更新为「" + statusText(target) + "」" + suffix(remark));
        return currentState(order);
    }

    /**
     * 标记已发货：必须提供物流公司与单号。首次发货走既有发货链路（同步订单状态与物流记录），
     * 已发货过的订单只允许更正物流信息。
     */
    public Map<String, Object> ship(Long adminId, TradeOrderDO order, String company, String logisticsNo,
                                    String logisticsCode) {
        if (order.getFulfillmentStatus() != null && order.getFulfillmentStatus() == SIGNED) {
            throw new ServerException(400, "订单已签收，不能再修改发货信息");
        }
        if (company == null || company.isBlank() || logisticsNo == null || logisticsNo.isBlank()) {
            throw new ServerException(400, "标记已发货需要填写物流公司和物流单号");
        }
        String trimmedCompany = company.trim();
        String trimmedNo = logisticsNo.trim();
        if (trimmedNo.length() < 6 || trimmedNo.length() > 32
                || !trimmedNo.matches("[A-Za-z0-9-]+")) {
            throw new ServerException(400, "物流单号仅支持 6 至 32 位字母、数字或连字符");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("logisticsCompany", trimmedCompany);
        request.put("logisticsNo", trimmedNo);
        if (logisticsCode != null && !logisticsCode.isBlank()) {
            request.put("logisticsCode", logisticsCode.trim());
        }

        if (order.getStatus() != null && order.getStatus() == 1) {
            tradeLogisticsService.adminShip(adminId, order.getId(), request);
        } else if (order.getStatus() != null && order.getStatus() == 2) {
            updateLogistics(order.getId(), request);
            updateFulfillment(order, SHIPPED);
        } else {
            throw new ServerException(400, "当前订单不能发货");
        }

        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_ADMIN, adminId,
                "FULFILLMENT_SHIPPED", order.getStatus(), order.getStatus(),
                "已发货，物流公司 " + trimmedCompany + "，单号 " + trimmedNo);
        return currentState(order);
    }

    private void updateLogistics(Long orderId, Map<String, Object> request) {
        TradeOrderLogisticsDO logistics = tradeOrderLogisticsMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrderLogisticsDO>()
                        .eq(TradeOrderLogisticsDO::getOrderId, orderId)
                        .orderByDesc(TradeOrderLogisticsDO::getId)
                        .last("LIMIT 1"));
        if (logistics == null) {
            logistics = new TradeOrderLogisticsDO();
            logistics.setOrderId(orderId);
            logistics.setDeliveryTime(LocalDateTime.now());
        }
        logistics.setLogisticsCompany(String.valueOf(request.get("logisticsCompany")));
        logistics.setLogisticsNo(String.valueOf(request.get("logisticsNo")));
        if (request.get("logisticsCode") != null) {
            logistics.setLogisticsCode(String.valueOf(request.get("logisticsCode")));
        }
        if (logistics.getId() == null) {
            tradeOrderLogisticsMapper.insert(logistics);
        } else {
            tradeOrderLogisticsMapper.updateById(logistics);
        }
    }

    private void updateFulfillment(TradeOrderDO order, int target) {
        tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .set(TradeOrderDO::getFulfillmentStatus, target)
                .set(TradeOrderDO::getFulfillmentUpdateTime, LocalDateTime.now()));
        order.setFulfillmentStatus(target);
    }

    private void assertTransition(int from, int to) {
        boolean allowed = switch (from) {
            case PENDING -> to == PREPARING || to == AWAITING_PICKUP || to == SHIPPED || to == ABNORMAL;
            case PREPARING -> to == AWAITING_PICKUP || to == SHIPPED || to == ABNORMAL;
            case AWAITING_PICKUP -> to == SHIPPED || to == ABNORMAL;
            case SHIPPED -> to == SIGNED || to == ABNORMAL;
            case ABNORMAL -> to == SIGNED;
            default -> false;
        };
        if (!allowed) {
            throw new ServerException(400,
                    "不能从「" + statusText(from) + "」变更为「" + statusText(to) + "」");
        }
    }

    private Map<String, Object> currentState(TradeOrderDO order) {
        TradeOrderDO latest = tradeOrderMapper.selectById(order.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", order.getId());
        data.put("orderSn", order.getOrderSn());
        data.put("status", latest == null ? order.getStatus() : latest.getStatus());
        data.put("fulfillmentStatus", latest == null ? order.getFulfillmentStatus() : latest.getFulfillmentStatus());
        data.put("fulfillmentStatusText",
                statusText(latest == null ? order.getFulfillmentStatus() : latest.getFulfillmentStatus()));
        return data;
    }

    private String suffix(String remark) {
        return remark == null || remark.isBlank() ? "" : "：" + remark.trim();
    }
}
