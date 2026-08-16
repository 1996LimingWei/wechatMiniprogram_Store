package com.shop.module.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MarketingShippingAuditRespVO {

    private Long id;
    private Long adminUserId;
    private String username;
    private String nickname;
    private String method;
    private String requestUri;
    private String businessRef;
    private Integer success;
    private String ip;
    private Long durationMs;
    private String message;
    private LocalDateTime createTime;
}
