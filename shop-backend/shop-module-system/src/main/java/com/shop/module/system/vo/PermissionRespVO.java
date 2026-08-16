package com.shop.module.system.vo;

import lombok.Data;

@Data
public class PermissionRespVO {
    private Long id;
    private String code;
    private String name;
    private String pathPattern;
    private String httpMethod;
    private Integer status;
}
