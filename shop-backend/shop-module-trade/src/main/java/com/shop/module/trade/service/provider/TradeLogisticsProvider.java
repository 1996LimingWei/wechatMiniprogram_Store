package com.shop.module.trade.service.provider;

import java.time.LocalDateTime;
import java.util.List;

/** 物流轨迹统一契约，发货信息仍由本地交易数据库保存。 */
public interface TradeLogisticsProvider {

    String type();

    List<LogisticsTrace> query(LogisticsQuery query);

    record LogisticsQuery(
            Long orderId,
            String logisticsCompany,
            String logisticsNo,
            LocalDateTime deliveryTime,
            Integer orderStatus) {
    }

    record LogisticsTrace(LocalDateTime time, String text) {
    }
}
