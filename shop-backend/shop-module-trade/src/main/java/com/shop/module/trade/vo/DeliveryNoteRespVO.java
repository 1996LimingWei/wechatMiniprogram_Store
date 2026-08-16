package com.shop.module.trade.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeliveryNoteRespVO {

    private Long orderId;
    private String orderSn;
    private String consignee;
    private String mobile;
    private String fullAddress;
    private String logisticsCompany;
    private String logisticsNo;
    private String deliveryTime;
    private String goodsPrice;
    private String freightPrice;
    private String couponPrice;
    private String actualPrice;
    private String adminRemark;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long skuId;
        private String goodsName;
        private String specName;
        private String retailPrice;
        private Integer count;
        private String totalPrice;
    }
}
