package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wechat.pay", name = "reconcile-job-enabled", havingValue = "true")
public class WechatPayReconcileJob {

    private final PayOrderService payOrderService;

    @Value("${wechat.pay.reconcile-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${wechat.pay.reconcile-job-fixed-delay:60000}")
    public void reconcilePendingPayments() {
        for (Long payOrderId : payOrderService.listPendingWechatPayOrderIds(batchSize)) {
            try {
                payOrderService.syncPendingWechatPayment(payOrderId);
            } catch (Exception exception) {
                log.warn("[WechatPayReconcileJob] 支付状态同步失败, payOrderId={}, message={}",
                        payOrderId, exception.getMessage());
            }
        }
    }
}
