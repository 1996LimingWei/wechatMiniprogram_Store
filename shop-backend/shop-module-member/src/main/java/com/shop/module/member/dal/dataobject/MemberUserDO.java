package com.shop.module.member.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员用户 DO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_user")
public class MemberUserDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 微信openid */
    private String openid;

    /** 微信unionid */
    private String unionid;

    /** 微信session_key */
    private String sessionKey;

    /** 手机号 */
    private String mobile;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 状态 1=正常 0=禁用 */
    private Integer status;

    /** 会员等级 1=白银会员 2=黄金会员 */
    private Integer memberLevel;
}
