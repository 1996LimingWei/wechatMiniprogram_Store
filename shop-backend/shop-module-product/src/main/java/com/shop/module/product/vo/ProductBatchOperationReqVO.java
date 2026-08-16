package com.shop.module.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductBatchOperationReqVO {

    private List<Long> ids;
    private Integer confirmCount;
    private Integer status;
    private Long categoryId;
    private Integer sort;
    private String priceAdjustType;
    private BigDecimal priceAdjustValue;
    private Integer stockDelta;
    private String reason;
}
