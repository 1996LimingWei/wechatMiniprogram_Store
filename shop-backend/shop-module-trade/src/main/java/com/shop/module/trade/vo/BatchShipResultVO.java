package com.shop.module.trade.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchShipResultVO {

    private Integer totalCount = 0;
    private Integer successCount = 0;
    private Integer failedCount = 0;
    private Boolean dryRun = false;
    private List<Row> rows = new ArrayList<>();

    @Data
    public static class Row {
        private Integer rowNo;
        private String orderSn;
        private Boolean success;
        private String message;
    }
}
