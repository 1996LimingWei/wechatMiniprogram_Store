package com.shop.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogRespVO {
    private Long id;
    private Long adminUserId;
    private String username;
    private String nickname;
    private String adminRoleCodes;
    private String method;
    private String requestUri;
    private String operationType;
    private Integer highRisk;
    private String businessRef;
    private Integer success;
    private String ip;
    private String userAgent;
    private Long durationMs;
    private String message;
    private String beforeSnapshot;
    private String afterSnapshot;
    private LocalDateTime createTime;
}
