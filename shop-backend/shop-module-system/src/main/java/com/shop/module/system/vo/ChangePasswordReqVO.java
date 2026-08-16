package com.shop.module.system.vo;

import lombok.Data;

@Data
public class ChangePasswordReqVO {
    private String oldPassword;
    private String newPassword;
}
