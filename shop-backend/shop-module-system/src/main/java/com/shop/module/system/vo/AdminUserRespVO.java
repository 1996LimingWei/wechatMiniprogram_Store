package com.shop.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserRespVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private Integer failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private List<String> roleNames;
    private LocalDateTime createTime;
}
