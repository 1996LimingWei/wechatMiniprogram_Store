package com.shop.module.trade.service.provider;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 根据发货信息生成稳定轨迹，便于在无物流账号时验收页面。 */
@Component
public class MockTradeLogisticsProvider implements TradeLogisticsProvider {

    @Override
    public String type() {
        return "mock";
    }

    @Override
    public List<LogisticsTrace> query(LogisticsQuery query) {
        if (query.deliveryTime() == null) {
            return List.of();
        }
        LocalDateTime deliveryTime = query.deliveryTime();
        List<LogisticsTrace> traces = new ArrayList<>();
        if (query.orderStatus() != null && query.orderStatus() == 3) {
            traces.add(new LogisticsTrace(deliveryTime.plusDays(2), "包裹已签收"));
        }
        traces.add(new LogisticsTrace(deliveryTime.plusHours(6), "包裹运输中，正在前往下一站"));
        traces.add(new LogisticsTrace(deliveryTime.plusHours(1),
                query.logisticsCompany() + "已揽收包裹"));
        traces.add(new LogisticsTrace(deliveryTime,
                "商家已发货，物流单号：" + query.logisticsNo()));
        return List.copyOf(traces);
    }
}
