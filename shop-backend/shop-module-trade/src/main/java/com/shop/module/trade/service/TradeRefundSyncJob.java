package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "trade.refund", name = "sync-job-enabled", havingValue = "true")
public class TradeRefundSyncJob {

    private final TradeAfterSaleService tradeAfterSaleService;

    @Value("${trade.refund.sync-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${trade.refund.sync-job-fixed-delay:60000}")
    public void syncProcessingRefunds() {
        for (Long afterSaleId : tradeAfterSaleService.listProcessingIds(batchSize)) {
            try {
                tradeAfterSaleService.syncProcessingBySystem(afterSaleId);
            } catch (Exception exception) {
                log.warn("[TradeRefundSyncJob] 退款状态同步失败, afterSaleId={}, message={}",
                        afterSaleId, exception.getMessage());
            }
        }
    }
}
