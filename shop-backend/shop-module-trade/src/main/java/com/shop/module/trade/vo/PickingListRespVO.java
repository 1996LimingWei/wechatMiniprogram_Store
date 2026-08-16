package com.shop.module.trade.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PickingListRespVO {

    private Integer orderCount;
    private Integer itemCount;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long spuId;
        private Long skuId;
        private String goodsName;
        private String specName;
        private Integer count;
        private List<String> orderSns = new ArrayList<>();
    }
}
