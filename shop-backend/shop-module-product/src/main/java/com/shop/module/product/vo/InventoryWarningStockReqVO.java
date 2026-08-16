package com.shop.module.product.vo;

import lombok.Data;

@Data
public class InventoryWarningStockReqVO {

    private Long skuId;
    private Integer warningStock;
}
