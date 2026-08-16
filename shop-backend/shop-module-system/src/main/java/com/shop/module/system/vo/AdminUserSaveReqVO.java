package com.shop.module.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserSaveReqVO {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private List<Long> roleIds;
}
