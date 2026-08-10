package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayNotifyLogDO;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayNotifyLogMapper;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOrderService {

    private final PayOrderMapper payOrderMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;
    private final TradeOrderService tradeOrderService;
    private final WechatPayService wechatPayService;
    private final TradeMockActionGuard tradeMockActionGuard;

    public Map<String, Object> prepay(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderService.getUserOrder(userId, orderId);
        if (order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID) {
            throw new ServerException(400, "订单已支付");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ServerException(400, "当前订单不能支付");
        }
        if (order.getActualPrice() == null || order.getActualPrice() <= 0) {
            throw new ServerException(400, "订单金额异常");
        }
        boolean wechatEnabled = wechatPayService.isEnabled();
        String channel = wechatEnabled ? "wx_lite" : "mock";
        if (!wechatEnabled) {
            tradeMockActionGuard.checkEnabled();
        }

        PayOrderDO payOrder = getPayOrder(userId, orderId);
        if (payOrder == null) {
            payOrder = new PayOrderDO();
            payOrder.setPaySn(generatePaySn());
            payOrder.setOrderId(orderId);
            payOrder.setUserId(userId);
            payOrder.setAmount(order.getActualPrice());
            payOrder.setChannel(channel);
            payOrder.setStatus(PayOrderStatus.PENDING);
            try {
                payOrderMapper.insert(payOrder);
            } catch (DuplicateKeyException exception) {
                payOrder = getPayOrder(userId, orderId);
                if (payOrder == null) {
                    throw exception;
                }
            }
        } else {
            validatePendingPayOrder(payOrder, order);
        }
        if (!channel.equals(payOrder.getChannel())) {
            throw new ServerException(400, "支付渠道已变更，请关闭原支付单后重试");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (wechatEnabled) {
            result.putAll(wechatPayService.createMiniProgramPayment(
                    new WechatPayService.TradeOrderDOView(
                            order.getUserId(), order.getOrderSn(), order.getActualPrice(), order.getExpireTime()),
                    payOrder.getPaySn()));
        } else {
            result.put("mockPay", true);
            result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            result.put("nonceStr", "mock_nonce");
            result.put("package", "prepay_id=mock_prepay");
            result.put("signType", "MD5");
            result.put("paySign", "mock_sign");
        }
        result.put("orderId", orderId);
        result.put("payOrderId", payOrder.getId());
        result.put("paySn", payOrder.getPaySn());
        result.put("amount", TradeMoneyUtils.formatYuan(order.getActualPrice()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void mockSuccess(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderService.getUserOrder(userId, orderId);
        PayOrderDO payOrder = getPayOrder(userId, orderId);
        if (payOrder == null) {
            prepay(userId, orderId);
            payOrder = getPayOrder(userId, orderId);
        }
        if (payOrder == null) {
            throw new ServerException(400, "支付单不存在");
        }
        validatePayOrderAmount(payOrder, order);
        if (payOrder.getStatus() != null && payOrder.getStatus() == PayOrderStatus.PAID) {
            if (order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID) {
                return;
            }
            throw new ServerException(400, "支付单与订单状态不一致");
        }
        validatePendingPayOrder(payOrder, order);

        tradeOrderService.markPaid(userId, orderId);
        int updated = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .eq(PayOrderDO::getStatus, PayOrderStatus.PENDING)
                .set(PayOrderDO::getStatus, PayOrderStatus.PAID)
                .set(PayOrderDO::getPayTime, java.time.LocalDateTime.now()));
        if (updated == 1) {
            return;
        }

        PayOrderDO latest = getPayOrder(userId, orderId);
        if (latest != null && latest.getStatus() != null && latest.getStatus() == PayOrderStatus.PAID) {
            return;
        }
        throw new ServerException(400, "支付单状态已变更，不能确认支付");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> query(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderService.getUserOrder(userId, orderId);
        PayOrderDO payOrder = getPayOrder(userId, orderId);
        syncWechatPaymentIfNeeded(payOrder);
        order = tradeOrderService.getUserOrder(userId, orderId);
        payOrder = getPayOrder(userId, orderId);
        String orderStatus = order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                ? "paid" : order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.REFUNDED
                ? "refunded" : "unpaid";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderStatus", orderStatus);
        result.put("orderStatusText", getOrderStatusText(orderStatus));
        result.put("payStatus", payOrder == null ? null : payOrder.getStatus());
        result.put("payStatusText", payOrder == null ? "" : PayOrderStatus.getText(payOrder.getStatus()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncPendingWechatPayment(Long payOrderId) {
        if (payOrderId == null || payOrderId <= 0) {
            return;
        }
        syncWechatPaymentIfNeeded(payOrderMapper.selectById(payOrderId));
    }

    public List<Long> listPendingWechatPayOrderIds(int limit) {
        return payOrderMapper.selectList(new LambdaQueryWrapper<PayOrderDO>()
                        .eq(PayOrderDO::getStatus, PayOrderStatus.PENDING)
                        .eq(PayOrderDO::getChannel, "wx_lite")
                        .orderByAsc(PayOrderDO::getLastQueryTime)
                        .orderByAsc(PayOrderDO::getId)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(PayOrderDO::getId)
                .toList();
    }

    private void syncWechatPaymentIfNeeded(PayOrderDO payOrder) {
        if (payOrder == null || payOrder.getStatus() == null
                || payOrder.getStatus() != PayOrderStatus.PENDING
                || !"wx_lite".equals(payOrder.getChannel()) || !wechatPayService.isEnabled()) {
            return;
        }
        java.time.LocalDateTime queryTime = java.time.LocalDateTime.now();
        int claimed = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .eq(PayOrderDO::getStatus, PayOrderStatus.PENDING)
                .eq(PayOrderDO::getChannel, "wx_lite")
                .and(wrapper -> wrapper.isNull(PayOrderDO::getLastQueryTime)
                        .or().le(PayOrderDO::getLastQueryTime, queryTime.minusSeconds(5)))
                .set(PayOrderDO::getLastQueryTime, queryTime));
        if (claimed != 1) {
            return;
        }
        WechatPayService.PaymentQueryResult result;
        try {
            result = wechatPayService.queryPayment(payOrder.getPaySn());
        } catch (Exception exception) {
            log.warn("[PayOrderService] 微信支付主动查单失败, payOrderId={}, message={}",
                    payOrder.getId(), exception.getMessage());
            return;
        }
        if (!payOrder.getPaySn().equals(result.paySn())) {
            throw new ServerException(502, "微信支付查单返回的支付单号不匹配");
        }
        if (!"SUCCESS".equals(result.tradeState())) {
            return;
        }
        if (result.transactionId() == null || result.transactionId().isBlank()) {
            throw new ServerException(502, "微信支付查单未返回渠道交易号");
        }
        handleWechatNotification(new WechatPayService.PaymentNotification(
                "QUERY-" + result.transactionId(),
                "TRANSACTION.QUERY",
                result.paySn(),
                result.transactionId(),
                result.tradeState(),
                result.amount(),
                result.successTime()), result.rawBody());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleWechatNotification(WechatPayService.PaymentNotification notification,
                                         String rawBody) {
        if (!"SUCCESS".equals(notification.tradeState())) {
            throw new ServerException(400, "微信支付结果不是成功状态");
        }
        PayNotifyLogDO notifyLog = new PayNotifyLogDO();
        notifyLog.setNotificationId(notification.notificationId());
        notifyLog.setPaySn(notification.paySn());
        notifyLog.setChannelTradeNo(notification.transactionId());
        notifyLog.setEventType(notification.eventType());
        notifyLog.setStatus(0);
        notifyLog.setMessage("已接收");
        notifyLog.setRawBody(rawBody);
        try {
            payNotifyLogMapper.insert(notifyLog);
        } catch (DuplicateKeyException exception) {
            return;
        }

        PayOrderDO payOrder = getPayOrderByPaySn(notification.paySn());
        if (payOrder == null || !"wx_lite".equals(payOrder.getChannel())) {
            throw new ServerException(400, "微信支付单不存在或渠道不匹配");
        }
        TradeOrderDO order = tradeOrderService.getUserOrder(payOrder.getUserId(), payOrder.getOrderId());
        if (!payOrder.getAmount().equals(notification.amount())
                || !order.getActualPrice().equals(notification.amount())) {
            throw new ServerException(400, "微信支付回调金额不匹配");
        }

        if (payOrder.getStatus() != null && payOrder.getStatus() == PayOrderStatus.PAID) {
            if (!notification.transactionId().equals(payOrder.getChannelTradeNo())) {
                throw new ServerException(400, "支付单渠道交易号不一致");
            }
            markNotifyHandled(notifyLog, payOrder, "重复成功通知已忽略");
            return;
        }
        validatePendingPayOrder(payOrder, order);

        int updated = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .eq(PayOrderDO::getStatus, PayOrderStatus.PENDING)
                .set(PayOrderDO::getStatus, PayOrderStatus.PAID)
                .set(PayOrderDO::getChannelTradeNo, notification.transactionId())
                .set(PayOrderDO::getPayTime, notification.successTime()));
        if (updated != 1) {
            throw new ServerException(400, "支付单状态已变更，不能确认支付");
        }
        tradeOrderService.markPaidBySystem(payOrder.getUserId(), payOrder.getOrderId());
        payOrder.setStatus(PayOrderStatus.PAID);
        payOrder.setChannelTradeNo(notification.transactionId());
        payOrder.setPayTime(notification.successTime());
        markNotifyHandled(notifyLog, payOrder, "支付成功通知已处理");
    }

    private PayOrderDO getPayOrder(Long userId, Long orderId) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId)
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
    }

    private PayOrderDO getPayOrderByPaySn(String paySn) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getPaySn, paySn)
                .last("LIMIT 1"));
    }

    private void markNotifyHandled(PayNotifyLogDO notifyLog, PayOrderDO payOrder, String message) {
        int updated = payNotifyLogMapper.update(null, new LambdaUpdateWrapper<PayNotifyLogDO>()
                .eq(PayNotifyLogDO::getId, notifyLog.getId())
                .eq(PayNotifyLogDO::getStatus, 0)
                .set(PayNotifyLogDO::getPayOrderId, payOrder.getId())
                .set(PayNotifyLogDO::getStatus, 1)
                .set(PayNotifyLogDO::getMessage, message));
        if (updated != 1) {
            throw new ServerException(500, "支付通知流水更新失败");
        }
    }

    private void validatePendingPayOrder(PayOrderDO payOrder, TradeOrderDO order) {
        validatePayOrderAmount(payOrder, order);
        if (payOrder.getStatus() == null || payOrder.getStatus() == PayOrderStatus.PENDING) {
            return;
        }
        if (payOrder.getStatus() == PayOrderStatus.PAID) {
            throw new ServerException(400, "支付单已完成");
        }
        if (payOrder.getStatus() == PayOrderStatus.CLOSED) {
            throw new ServerException(400, "支付单已关闭");
        }
        if (payOrder.getStatus() == PayOrderStatus.REFUNDED) {
            throw new ServerException(400, "支付单已退款");
        }
        throw new ServerException(400, "支付单状态异常");
    }

    private void validatePayOrderAmount(PayOrderDO payOrder, TradeOrderDO order) {
        if (payOrder.getAmount() == null || order.getActualPrice() == null
                || !payOrder.getAmount().equals(order.getActualPrice())) {
            throw new ServerException(400, "支付单金额与订单实付金额不一致");
        }
    }

    private String getOrderStatusText(String orderStatus) {
        return switch (orderStatus) {
            case "paid" -> "已支付";
            case "refunded" -> "已退款";
            default -> "未支付";
        };
    }

    private String generatePaySn() {
        return "P" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
