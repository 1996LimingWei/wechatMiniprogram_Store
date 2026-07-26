package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TradeAfterSaleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PayOrderMapper payOrderMapper;
    private final TradeOrderLogService tradeOrderLogService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Long userId, Long orderId, Map<String, Object> request) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getPayStatus() == null || order.getPayStatus() != 1) {
            throw new ServerException(400, "未支付订单不能申请退款");
        }
        if (order.getStatus() == null || order.getStatus() == 0 || order.getStatus() == 4) {
            throw new ServerException(400, "当前订单不能申请售后");
        }
        if (order.getPayStatus() == 2) {
            throw new ServerException(400, "订单已退款");
        }
        TradeAfterSaleDO existed = getAfterSale(orderId);
        if (existed != null) {
            return toResp(existed);
        }

        String reason = TradeRequestUtils.getString(request, "reason", "用户申请退款");
        String remark = TradeRequestUtils.getString(request, "remark", "");
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setOrderId(order.getId());
        afterSale.setUserId(userId);
        afterSale.setAfterSaleSn(generateAfterSaleSn());
        afterSale.setType(order.getStatus() == 1 ? 1 : 2);
        afterSale.setStatus(0);
        afterSale.setRefundAmount(order.getActualPrice());
        afterSale.setReason(reason);
        afterSale.setApplyRemark(remark);
        afterSale.setApplyTime(LocalDateTime.now());
        tradeAfterSaleMapper.insert(afterSale);
        Integer fromStatus = order.getStatus();
        order.setStatus(5);
        tradeOrderMapper.updateById(order);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "APPLY_AFTER_SALE", fromStatus, order.getStatus(), reason);
        return toResp(afterSale);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mockApprove(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        if (afterSale == null) {
            afterSale = new TradeAfterSaleDO();
            afterSale.setOrderId(order.getId());
            afterSale.setUserId(userId);
            afterSale.setAfterSaleSn(generateAfterSaleSn());
            afterSale.setType(order.getStatus() == 1 ? 1 : 2);
            afterSale.setStatus(0);
            afterSale.setRefundAmount(order.getActualPrice());
            afterSale.setReason("模拟退款");
            afterSale.setApplyRemark("");
            afterSale.setApplyTime(LocalDateTime.now());
            tradeAfterSaleMapper.insert(afterSale);
        }
        if (afterSale.getStatus() == 1) {
            return toResp(afterSale);
        }
        if (order.getPayStatus() == null || order.getPayStatus() != 1) {
            throw new ServerException(400, "当前订单不能退款");
        }

        afterSale.setStatus(1);
        afterSale.setAuditTime(LocalDateTime.now());
        tradeAfterSaleMapper.updateById(afterSale);

        Integer fromStatus = order.getStatus();
        Integer fromPayStatus = order.getPayStatus();
        order.setPayStatus(2);
        order.setStatus(5);
        tradeOrderMapper.updateById(order);
        tradeOrderLogService.recordPayChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "REFUND_SUCCESS", fromStatus, order.getStatus(), fromPayStatus, order.getPayStatus(),
                "Mock 退款审核通过");

        PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId));
        if (payOrder != null) {
            payOrder.setStatus(3);
            payOrderMapper.updateById(payOrder);
        }
        return toResp(afterSale);
    }

    public Map<String, Object> detail(Long userId, Long orderId) {
        getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        return afterSale == null ? emptyResp() : toResp(afterSale);
    }

    public Map<String, Object> getOrderAfterSaleInfo(Long orderId) {
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        return afterSale == null ? emptyResp() : toResp(afterSale);
    }

    private TradeOrderDO getUserOrder(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getId, orderId));
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        return order;
    }

    private TradeAfterSaleDO getAfterSale(Long orderId) {
        return tradeAfterSaleMapper.selectOne(new LambdaQueryWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getOrderId, orderId)
                .orderByDesc(TradeAfterSaleDO::getUpdateTime)
                .last("LIMIT 1"));
    }

    private Map<String, Object> emptyResp() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasAfterSale", false);
        return result;
    }

    private Map<String, Object> toResp(TradeAfterSaleDO afterSale) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasAfterSale", true);
        result.put("id", afterSale.getId());
        result.put("orderId", afterSale.getOrderId());
        result.put("afterSaleSn", afterSale.getAfterSaleSn());
        result.put("type", afterSale.getType());
        result.put("typeText", afterSale.getType() != null && afterSale.getType() == 1 ? "仅退款" : "退货退款");
        result.put("status", afterSale.getStatus());
        result.put("statusText", getStatusText(afterSale.getStatus()));
        result.put("refundAmount", TradeMoneyUtils.formatYuan(afterSale.getRefundAmount()));
        result.put("reason", afterSale.getReason());
        result.put("applyRemark", afterSale.getApplyRemark());
        result.put("applyTime", formatTime(afterSale.getApplyTime()));
        result.put("auditTime", formatTime(afterSale.getAuditTime()));
        return result;
    }

    private String getStatusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 0 -> "退款处理中";
            case 1 -> "已退款";
            case 2 -> "退款已拒绝";
            default -> "未知";
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    private String generateAfterSaleSn() {
        return "R" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
