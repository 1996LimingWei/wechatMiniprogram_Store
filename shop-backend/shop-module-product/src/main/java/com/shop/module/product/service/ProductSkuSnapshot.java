package com.shop.module.product.service;

import lombok.Data;

/** 商品 SKU 的交易快照，供购物车和订单写入不可变商品信息。 */
@Data
public class ProductSkuSnapshot {

    private Long spuId;
    private Long skuId;
    private String name;
    private String picUrl;
    private String specName;
    private Integer price;
    private Integer stock;
}
