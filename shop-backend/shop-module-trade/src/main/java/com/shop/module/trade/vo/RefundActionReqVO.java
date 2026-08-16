package com.shop.module.trade.vo;

import lombok.Data;

@Data
public class RefundActionReqVO {
    private Long afterSaleId;
    private String remark;
}
