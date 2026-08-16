package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "trade.reconcile", name = "job-enabled", havingValue = "true")
public class TradeDailyReconcileJob {

    private final ReconciliationWorkbenchService reconciliationWorkbenchService;
    private final DistributedJobLockService jobLockService;

    @Scheduled(cron = "${trade.reconcile.job-cron:0 30 2 * * ?}")
    public void reconcileYesterday() {
        if (!jobLockService.tryLock("trade-daily-reconcile", Duration.ofHours(2))) {
            return;
        }
        try {
            LocalDate date = LocalDate.now().minusDays(1);
            reconciliationWorkbenchService.run(0L, date.toString(), "JOB");
            log.info("[TradeDailyReconcileJob] 日终对账完成 date={}", date);
        } finally {
            jobLockService.release("trade-daily-reconcile");
        }
    }
}
