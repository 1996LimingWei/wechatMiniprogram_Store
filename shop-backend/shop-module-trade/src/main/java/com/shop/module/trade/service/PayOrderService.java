package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PayOrderService {

    private final PayOrderMapper payOrderMapper;
    private final TradeOrderService tradeOrderService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> prepay(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderService.getUserOrder(userId, orderId);
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            throw new ServerException(400, "订单已支付");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ServerException(400, "当前订单不能支付");
        }
        if (order.getActualPrice() == null || order.getActualPrice() <= 0) {
            throw new ServerException(400, "订单金额异常");
        }
        PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId));
        if (payOrder != null && payOrder.getStatus() != null && payOrder.getStatus() == 1) {
            throw new ServerException(400, "支付单已完成");
        }
        if (payOrder == null) {
            payOrder = new PayOrderDO();
            payOrder.setPaySn(generatePaySn());
            payOrder.setOrderId(orderId);
            payOrder.setUserId(userId);
            payOrder.setAmount(order.getActualPrice());
            payOrder.setChannel("mock");
            payOrder.setStatus(0);
            payOrderMapper.insert(payOrder);
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
        PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId));
        if (payOrder == null) {
            prepay(userId, orderId);
            payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                    .eq(PayOrderDO::getOrderId, orderId)
                    .eq(PayOrderDO::getUserId, userId));
        }
        tradeOrderService.markPaid(userId, orderId);
        payOrder.setStatus(1);
        payOrder.setPayTime(LocalDateTime.now());
        payOrderMapper.updateById(payOrder);
    }

    public Map<String, Object> query(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderService.getUserOrder(userId, orderId);
        String orderStatus = order.getPayStatus() != null && order.getPayStatus() == 2 ? "refunded"
                : order.getPayStatus() != null && order.getPayStatus() == 1 ? "paid" : "unpaid";
        String orderStatusText = switch (orderStatus) {
            case "refunded" -> "已退款";
            case "paid" -> "已支付";
            default -> "未支付";
        };
        return Map.of(
                "orderStatus", orderStatus,
                "orderStatusText", orderStatusText
        );
    }

    private String generatePaySn() {
        return "P" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
