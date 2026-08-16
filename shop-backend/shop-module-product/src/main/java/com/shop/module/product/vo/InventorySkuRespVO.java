package com.shop.module.product.vo;

import lombok.Data;

@Data
public class InventorySkuRespVO {

    private Long skuId;
    private Long spuId;
    private String skuCode;
    private String productName;
    private String specName;
    private Long categoryId;
    private String categoryName;
    private String picUrl;
    private Integer price;
    private Integer stock;
    private Integer availableStock;
    private Integer lockedStock;
    private Integer warningStock;
    private String stockStatus;
    private String stockStatusName;
    private String createTime;
}
