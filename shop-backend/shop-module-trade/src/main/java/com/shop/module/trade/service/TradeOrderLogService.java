package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogDO;
import com.shop.module.trade.dal.mysql.TradeOrderLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeOrderLogService {

    public static final String OPERATOR_USER = "user";
    public static final String OPERATOR_SYSTEM = "system";
    public static final String OPERATOR_ADMIN = "admin";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TradeOrderLogMapper tradeOrderLogMapper;

    public void recordCreated(TradeOrderDO order) {
        record(order, OPERATOR_USER, order.getUserId(), "CREATE_ORDER", null, order.getStatus(),
                null, order.getPayStatus(), "用户提交订单");
    }

    public void recordStatusChanged(TradeOrderDO order, String operatorType, Long operatorId, String action,
                                    Integer fromStatus, Integer toStatus, String remark) {
        record(order, operatorType, operatorId, action, fromStatus, toStatus,
                order.getPayStatus(), order.getPayStatus(), remark);
    }

    public void recordPayChanged(TradeOrderDO order, String operatorType, Long operatorId, String action,
                                 Integer fromStatus, Integer toStatus,
                                 Integer fromPayStatus, Integer toPayStatus, String remark) {
        record(order, operatorType, operatorId, action, fromStatus, toStatus, fromPayStatus, toPayStatus, remark);
    }

    public List<Map<String, Object>> listByOrderId(Long orderId) {
        return tradeOrderLogMapper.selectList(new LambdaQueryWrapper<TradeOrderLogDO>()
                        .eq(TradeOrderLogDO::getOrderId, orderId)
                        .orderByAsc(TradeOrderLogDO::getId))
                .stream()
                .map(this::toResp)
                .toList();
    }

    private void record(TradeOrderDO order, String operatorType, Long operatorId, String action,
                        Integer fromStatus, Integer toStatus,
                        Integer fromPayStatus, Integer toPayStatus, String remark) {
        if (order == null || order.getId() == null) {
            return;
        }
        TradeOrderLogDO log = new TradeOrderLogDO();
        log.setOrderId(order.getId());
        log.setUserId(order.getUserId());
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setFromPayStatus(fromPayStatus);
        log.setToPayStatus(toPayStatus);
        log.setRemark(remark);
        tradeOrderLogMapper.insert(log);
    }

    private Map<String, Object> toResp(TradeOrderLogDO log) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", log.getId());
        result.put("orderId", log.getOrderId());
        result.put("operatorType", log.getOperatorType());
        result.put("operatorId", log.getOperatorId());
        result.put("action", log.getAction());
        result.put("actionText", getActionText(log.getAction()));
        result.put("fromStatus", log.getFromStatus());
        result.put("toStatus", log.getToStatus());
        result.put("fromStatusText", getOrderStatusText(log.getFromStatus()));
        result.put("toStatusText", getOrderStatusText(log.getToStatus()));
        result.put("fromPayStatus", log.getFromPayStatus());
        result.put("toPayStatus", log.getToPayStatus());
        result.put("fromPayStatusText", getPayStatusText(log.getFromPayStatus()));
        result.put("toPayStatusText", getPayStatusText(log.getToPayStatus()));
        result.put("remark", log.getRemark());
        result.put("createTime", log.getCreateTime() == null ? "" : log.getCreateTime().format(TIME_FORMATTER));
        return result;
    }

    private String getActionText(String action) {
        return switch (action == null ? "" : action) {
            case "CREATE_ORDER" -> "提交订单";
            case "PAY_SUCCESS" -> "支付成功";
            case "USER_CANCEL" -> "用户取消";
            case "SYSTEM_CLOSE" -> "系统关闭";
            case "SHIP_ORDER" -> "商家发货";
            case "CONFIRM_RECEIPT" -> "确认收货";
            case "APPLY_AFTER_SALE" -> "申请售后";
            case "REFUND_PROCESSING" -> "退款处理中";
            case "REFUND_SUCCESS" -> "退款完成";
            case "REJECT_AFTER_SALE" -> "拒绝售后";
            case "CANCEL_AFTER_SALE" -> "撤销售后";
            default -> "订单操作";
        };
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "退款中";
            default -> "未知";
        };
    }

    private String getPayStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case TradeOrderPayStatus.UNPAID -> "未支付";
            case TradeOrderPayStatus.PAID -> "已支付";
            case TradeOrderPayStatus.REFUNDED -> "已退款";
            default -> "未知";
        };
    }
}
