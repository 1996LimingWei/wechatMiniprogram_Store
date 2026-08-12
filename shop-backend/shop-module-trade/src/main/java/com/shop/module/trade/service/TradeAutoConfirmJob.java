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
public class TradeAutoConfirmJob {

    private final TradeOrderService tradeOrderService;
    private final DistributedJobLockService jobLockService;

    @Value("${trade.order.auto-confirm-days:10}")
    private int confirmDays;

    @Value("${trade.order.auto-confirm-batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${trade.order.auto-confirm-job-fixed-delay:3600000}")
    public void autoConfirmReceipt() {
        if (!jobLockService.tryLock("trade-auto-confirm", Duration.ofHours(2))) return;
        try {
            int confirmed = tradeOrderService.autoConfirmDeliveredOrders(confirmDays, batchSize);
            if (confirmed > 0) log.info("自动确认收货完成，本次处理 {} 单", confirmed);
        } finally {
            jobLockService.release("trade-auto-confirm");
        }
    }
}
