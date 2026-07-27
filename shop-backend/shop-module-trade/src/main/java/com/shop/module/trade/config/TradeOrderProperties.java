package com.shop.module.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "trade.order")
public class TradeOrderProperties {

    /**
     * 待付款订单超时时间，单位：分钟。
     */
    private int unpaidTimeoutMinutes = 30;

    /**
     * 是否启用待付款订单自动关闭任务。
     */
    private boolean expireJobEnabled = true;

    /**
     * 单次扫描最多处理的订单数，避免一次任务占用过长时间。
     */
    private int expireBatchSize = 100;
}
