package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeAfterSaleExpireJob {

    private final TradeAfterSaleExpireService tradeAfterSaleExpireService;
    private final DistributedJobLockService jobLockService;

    @Value("${trade.refund.return-expire-batch-size:100}")
    private int batchSize = 100;

    @Scheduled(fixedDelayString = "${trade.refund.return-expire-job-fixed-delay:3600000}")
    public void expireOverdueReturns() {
        if (!jobLockService.tryLock("trade-return-expire", Duration.ofMinutes(10))) return;
        try {
            int expired = 0;
            for (Long afterSaleId : tradeAfterSaleExpireService.listOverdueIds(batchSize)) {
                try {
                    if (tradeAfterSaleExpireService.expireOne(afterSaleId)) expired++;
                } catch (Exception exception) {
                    log.warn("超期售后单处理失败, afterSaleId={}, message={}",
                            afterSaleId, exception.getMessage());
                }
            }
            if (expired > 0) log.info("超期未寄回售后关闭完成，本次处理 {} 单", expired);
        } finally {
            jobLockService.release("trade-return-expire");
        }
    }
}
