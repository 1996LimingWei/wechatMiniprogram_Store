package com.shop.module.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "trade")
public class TradeMockActionProperties {

    /** 是否允许会改变交易状态的 Mock 接口。 */
    private boolean mockActionsEnabled;
}
