package com.shop.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleRespVO {
    private Long id;
    private String code;
    private String name;
    private Integer status;
    private List<Long> permissionIds;
    private List<String> permissionCodes;
    private LocalDateTime createTime;
}
