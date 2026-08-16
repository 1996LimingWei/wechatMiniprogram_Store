package com.shop.module.member.vo;

import lombok.Data;

/** 用户资料写入白名单。手机号只能通过微信授权流程更新。 */
@Data
public class MemberProfileUpdateReqVO {
    private String nickname;
    private String avatar;
}
