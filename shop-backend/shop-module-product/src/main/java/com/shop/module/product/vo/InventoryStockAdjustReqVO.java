package com.shop.module.product.vo;

import lombok.Data;

@Data
public class InventoryStockAdjustReqVO {

    private Long skuId;
    private Integer changeQuantity;
    private String reason;
}
