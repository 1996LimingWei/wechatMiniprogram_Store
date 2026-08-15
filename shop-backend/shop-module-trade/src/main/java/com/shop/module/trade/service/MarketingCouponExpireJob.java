package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingCouponExpireJob {

    private static final int EXPIRE_BATCH_SIZE = 200;

    private final MarketingCouponService marketingCouponService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void expireUnusedCoupons() {
        try {
            int expired = marketingCouponService.expireUnusedCoupons(EXPIRE_BATCH_SIZE);
            if (expired > 0) {
                log.info("过期优惠券清理完成，本次处理 {} 张", expired);
            }
        } catch (Exception exception) {
            log.error("过期优惠券清理失败", exception);
        }
    }
}
