package com.shop.module.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class RoleSaveReqVO {
    private Long id;
    private String code;
    private String name;
    private List<Long> permissionIds;
}
