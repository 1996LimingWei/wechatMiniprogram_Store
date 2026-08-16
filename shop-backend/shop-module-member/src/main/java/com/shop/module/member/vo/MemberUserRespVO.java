package com.shop.module.member.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理后台可见的会员基础信息，禁止包含微信身份凭据。 */
@Data
public class MemberUserRespVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String mobile;
    private Integer status;
    private LocalDateTime createTime;
}
