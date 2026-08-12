package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "trade.refund", name = "sync-job-enabled", havingValue = "true")
public class TradeRefundSyncJob {

    private final TradeRefundExecutionService tradeRefundExecutionService;
    private final DistributedJobLockService jobLockService;

    @Value("${trade.refund.sync-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${trade.refund.sync-job-fixed-delay:60000}")
    public void syncProcessingRefunds() {
        if (!jobLockService.tryLock("trade-refund-sync", Duration.ofMinutes(10))) return;
        try {
            for (Long afterSaleId : tradeRefundExecutionService.listExecutableIds(batchSize)) {
                try {
                    tradeRefundExecutionService.execute(
                            afterSaleId, TradeOrderLogService.OPERATOR_SYSTEM, 0L, false);
                } catch (Exception exception) {
                    log.warn("[TradeRefundSyncJob] 退款状态同步失败, afterSaleId={}, message={}",
                            afterSaleId, exception.getMessage());
                }
            }
        } finally {
            jobLockService.release("trade-refund-sync");
        }
    }
}
