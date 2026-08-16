package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeRefundNotifyService {

    private static final int STATUS_REFUNDED = 1;
    private static final int STATUS_REFUNDING = 4;
    private static final int STATUS_REFUND_FAILED = 5;

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final PayOrderMapper payOrderMapper;
    private final TradeAfterSaleService tradeAfterSaleService;
    private final RefundNotifyAuditService refundNotifyAuditService;

    @Transactional(rollbackFor = Exception.class)
    public void handleWechatRefundNotification(WechatPayService.RefundNotification notification, String rawBody) {
        if (!refundNotifyAuditService.recordReceived(notification, rawBody)) {
            log.info("[handleWechatRefundNotification] 退款重复通知已忽略 notificationId={} afterSaleSn={}",
                    notification.notificationId(), notification.afterSaleSn());
            return;
        }
        TradeAfterSaleDO afterSale = getAfterSale(notification.afterSaleSn());
        PayOrderDO payOrder = getLatestPayOrder(afterSale);
        validateNotification(notification, afterSale, payOrder);

        if (Integer.valueOf(STATUS_REFUNDED).equals(afterSale.getStatus())) {
            refundNotifyAuditService.markHandled(notification.notificationId(), "售后单已退款，重复通知已忽略");
            return;
        }
        if (Integer.valueOf(STATUS_REFUND_FAILED).equals(afterSale.getStatus())
                && !"SUCCESS".equals(notification.refundStatus())) {
            refundNotifyAuditService.markHandled(notification.notificationId(), "售后单已失败，重复失败通知已记录");
            return;
        }
        if (!Integer.valueOf(STATUS_REFUNDING).equals(afterSale.getStatus())) {
            throw new ServerException(409, "售后单不在退款处理中，不能处理微信退款通知");
        }

        TradeRefundProvider.RefundResult result = new TradeRefundProvider.RefundResult(
                notification.providerRefundNo(),
                mapRefundStatus(notification.refundStatus()),
                getNotifyMessage(notification.refundStatus()));
        tradeAfterSaleService.applyRefundResult(afterSale.getId(), result,
                TradeOrderLogService.OPERATOR_SYSTEM, 0L,
                afterSale.getRefundAttemptCount() == null ? 0 : afterSale.getRefundAttemptCount());
        refundNotifyAuditService.markHandled(notification.notificationId(), "微信退款通知已处理");
    }

    private TradeAfterSaleDO getAfterSale(String afterSaleSn) {
        TradeAfterSaleDO afterSale = tradeAfterSaleMapper.selectOne(new LambdaQueryWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getAfterSaleSn, afterSaleSn)
                .eq(TradeAfterSaleDO::getDeleted, false)
                .last("LIMIT 1"));
        if (afterSale == null) {
            throw new ServerException(404, "微信退款通知关联售后单不存在");
        }
        return afterSale;
    }

    private PayOrderDO getLatestPayOrder(TradeAfterSaleDO afterSale) {
        PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, afterSale.getOrderId())
                .eq(PayOrderDO::getUserId, afterSale.getUserId())
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
        if (payOrder == null) {
            throw new ServerException(404, "微信退款通知关联支付单不存在");
        }
        return payOrder;
    }

    private void validateNotification(WechatPayService.RefundNotification notification,
                                      TradeAfterSaleDO afterSale, PayOrderDO payOrder) {
        if (!notification.paySn().equals(payOrder.getPaySn())) {
            throw new ServerException(400, "微信退款通知支付单号不匹配");
        }
        if (afterSale.getRefundAmount() == null || !afterSale.getRefundAmount().equals(notification.amount())) {
            throw new ServerException(400, "微信退款通知金额不匹配");
        }
        if (afterSale.getProviderRefundNo() != null && !afterSale.getProviderRefundNo().isBlank()
                && !afterSale.getProviderRefundNo().equals(notification.providerRefundNo())) {
            throw new ServerException(400, "微信退款通知渠道退款单号不匹配");
        }
    }

    private TradeRefundProvider.RefundStatus mapRefundStatus(String status) {
        return switch (status) {
            case "SUCCESS" -> TradeRefundProvider.RefundStatus.SUCCESS;
            case "PROCESSING" -> TradeRefundProvider.RefundStatus.PROCESSING;
            case "ABNORMAL", "CLOSED" -> TradeRefundProvider.RefundStatus.FAILED;
            default -> throw new ServerException(400, "微信退款通知状态不受支持");
        };
    }

    private String getNotifyMessage(String status) {
        return switch (status) {
            case "SUCCESS" -> "微信退款回调成功";
            case "PROCESSING" -> "微信退款回调处理中";
            case "ABNORMAL" -> "微信退款回调异常";
            case "CLOSED" -> "微信退款回调关闭";
            default -> "微信退款回调状态未知";
        };
    }
}
