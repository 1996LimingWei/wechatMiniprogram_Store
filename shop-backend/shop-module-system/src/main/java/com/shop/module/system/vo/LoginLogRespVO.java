package com.shop.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogRespVO {
    private Long id;
    private Long adminUserId;
    private String username;
    private String nickname;
    private Integer success;
    private String ip;
    private String userAgent;
    private String message;
    private LocalDateTime createTime;
}
