package com.shop.module.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InventoryReconcileRespVO {

    private int totalSkuCount;
    private int mismatchCount;
    private List<Row> rows = new ArrayList<>();

    @Data
    public static class Row {
        private Long skuId;
        private Long spuId;
        private String skuCode;
        private String productName;
        private Integer currentStock;
        private Integer ledgerStock;
        private Integer difference;
    }
}
