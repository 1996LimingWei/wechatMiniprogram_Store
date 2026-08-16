package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wechat.pay", name = "reconcile-job-enabled", havingValue = "true")
public class WechatPayReconcileJob {

    private final PayExceptionWorkbenchService payExceptionWorkbenchService;
    private final DistributedJobLockService jobLockService;
    private final TradeObservabilityService tradeObservabilityService;

    @Value("${wechat.pay.reconcile-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${wechat.pay.reconcile-job-fixed-delay:60000}")
    public void reconcilePendingPayments() {
        if (!jobLockService.tryLock("wechat-pay-reconcile", Duration.ofMinutes(10))) return;
        try {
            int processed = payExceptionWorkbenchService.scanPendingWechatPayments(batchSize);
            log.debug("[WechatPayReconcileJob] 已扫描微信支付单 {} 个", processed);
            tradeObservabilityService.recordJobResult("wechat-pay-reconcile", true, processed, "微信支付单扫描完成");
        } catch (Exception exception) {
            log.error("[WechatPayReconcileJob] 微信支付单扫描失败", exception);
            tradeObservabilityService.recordJobResult("wechat-pay-reconcile", false, 0, exception.getMessage());
        } finally {
            jobLockService.release("wechat-pay-reconcile");
        }
    }
}
