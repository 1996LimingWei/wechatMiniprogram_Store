package com.shop.module.product.vo;

import lombok.Data;

@Data
public class InventoryStockLogRespVO {

    private Long id;
    private Long skuId;
    private Long spuId;
    private String skuCode;
    private String productName;
    private String bizType;
    private String bizNo;
    private Integer changeQuantity;
    private Integer beforeStock;
    private Integer afterStock;
    private String operatorType;
    private Long operatorId;
    private String remark;
    private String createTime;
}
