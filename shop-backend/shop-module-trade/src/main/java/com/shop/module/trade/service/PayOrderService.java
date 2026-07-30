package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PayOrderService {

    private final PayOrderMapper payOrderMapper;
    private final TradeOrderService tradeOrderService;

    @Transactional(rollbackFor = Exception.class)
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
        PayOrderDO payOrder = getPayOrder(userId, orderId);
        if (payOrder == null) {
            payOrder = new PayOrderDO();
            payOrder.setPaySn(generatePaySn());
            payOrder.setOrderId(orderId);
            payOrder.setUserId(userId);
            payOrder.setAmount(order.getActualPrice());
            payOrder.setChannel("mock");
            payOrder.setStatus(PayOrderStatus.PENDING);
            payOrderMapper.insert(payOrder);
        } else {
            validatePendingPayOrder(payOrder, order);
        }

        return Map.of(
                "mockPay", true,
                "orderId", orderId,
                "payOrderId", payOrder.getId(),
                "paySn", payOrder.getPaySn(),
                "amount", TradeMoneyUtils.formatYuan(order.getActualPrice()),
                "timeStamp", String.valueOf(System.currentTimeMillis() / 1000),
                "nonceStr", "mock_nonce",
                "package", "prepay_id=mock_prepay",
                "signType", "MD5",
                "paySign", "mock_sign"
        );
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

    public Map<String, Object> query(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderService.getUserOrder(userId, orderId);
        PayOrderDO payOrder = getPayOrder(userId, orderId);
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

    private PayOrderDO getPayOrder(Long userId, Long orderId) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId)
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
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
        return "P" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
