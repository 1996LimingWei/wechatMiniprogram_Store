package com.shop.module.trade.service;

import lombok.Data;

@Data
public class TradeProductSnapshot {

    private Long spuId;
    private Long skuId;
    private String name;
    private String picUrl;
    private String specName;
    private Integer price;
    private Integer stock;
}
