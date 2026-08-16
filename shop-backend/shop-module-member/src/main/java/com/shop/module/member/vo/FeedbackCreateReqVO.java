package com.shop.module.member.vo;

import lombok.Data;

@Data
public class FeedbackCreateReqVO {
    private Integer type;
    private String content;
    private String mobile;
}
