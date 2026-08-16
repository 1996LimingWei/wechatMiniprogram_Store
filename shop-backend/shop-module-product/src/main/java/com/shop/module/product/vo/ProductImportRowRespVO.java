package com.shop.module.product.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductImportRowRespVO {

    private int rowNo;
    private boolean valid;
    private String productName;
    private String categoryName;
    private String skuCode;
    private String specName;
    private String specValue;
    private String price;
    private Integer stock;
    private List<String> errorColumns = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}
