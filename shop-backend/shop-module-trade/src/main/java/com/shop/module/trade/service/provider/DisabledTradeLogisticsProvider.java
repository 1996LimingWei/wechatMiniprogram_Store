package com.shop.module.trade.service.provider;

import org.springframework.stereotype.Component;

import java.util.List;

/** 生产安全默认实现：保留发货信息，但不伪造物流轨迹。 */
@Component
public class DisabledTradeLogisticsProvider implements TradeLogisticsProvider {

    @Override
    public String type() {
        return "disabled";
    }

    @Override
    public List<LogisticsTrace> query(LogisticsQuery query) {
        return List.of();
    }
}
