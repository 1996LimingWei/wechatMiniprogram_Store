package com.shop.module.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportPreviewRespVO {

    private int totalRows;
    private int validRows;
    private int errorRows;
    private int createdProductCount;
    private int createdSkuCount;
    private boolean dryRun;
    private List<ProductImportRowRespVO> rows = new ArrayList<>();
}
