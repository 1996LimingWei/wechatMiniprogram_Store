package com.shop.module.product.vo;

import lombok.Data;

@Data
public class ProductBatchItemResultRespVO {

    private Long id;
    private String name;
    private boolean success;
    private String message;
    private Integer beforePrice;
    private Integer afterPrice;
    private Integer beforeStock;
    private Integer afterStock;
}
