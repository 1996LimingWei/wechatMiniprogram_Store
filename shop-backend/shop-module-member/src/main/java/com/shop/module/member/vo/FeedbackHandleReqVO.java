package com.shop.module.member.vo;

import lombok.Data;

@Data
public class FeedbackHandleReqVO {
    private Long id;
    private Integer status;
    private String handleRemark;
}
