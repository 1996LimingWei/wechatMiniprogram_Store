package com.shop.module.trade.vo;

import lombok.Data;

@Data
public class MarketingShippingRuleRespVO {

    private Long id;
    private String name;
    private String freeThreshold;
    private String baseFee;
    private Integer status;
    private String startTime;
    private String endTime;
    private Boolean currentActive;
    private String createTime;
}
