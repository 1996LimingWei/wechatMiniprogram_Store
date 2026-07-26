package com.shop.module.trade.service;

import com.shop.module.trade.config.TradeOrderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeOrderExpireJob {

    private final TradeOrderProperties tradeOrderProperties;
    private final TradeOrderService tradeOrderService;

    @Scheduled(fixedDelayString = "${trade.order.expire-job-fixed-delay:60000}")
    public void closeExpiredUnpaidOrders() {
        if (!tradeOrderProperties.isExpireJobEnabled()) {
            return;
        }
        int closedCount = tradeOrderService.closeExpiredUnpaidOrders();
        if (closedCount > 0) {
            log.info("自动关闭超时未支付订单完成，本次关闭 {} 单", closedCount);
        }
    }
}
