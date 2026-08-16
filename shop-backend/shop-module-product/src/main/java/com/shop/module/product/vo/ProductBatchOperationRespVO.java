package com.shop.module.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductBatchOperationRespVO {

    private int totalCount;
    private int successCount;
    private int failureCount;
    private boolean dryRun;
    private List<ProductBatchItemResultRespVO> rows = new ArrayList<>();
}
