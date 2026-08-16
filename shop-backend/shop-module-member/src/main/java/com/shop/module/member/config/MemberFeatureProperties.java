package com.shop.module.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 尚未交付的会员能力统一由服务端开关控制，生产默认关闭。
 */
@Data
@Component
@ConfigurationProperties(prefix = "member.features")
public class MemberFeatureProperties {

    /** 会员等级、会员购买和权益展示。 */
    private boolean membershipEnabled = false;

    /** 头像、昵称等资料写入，需先完成对象存储和手机号流程后再开启。 */
    private boolean profileEditEnabled = false;
}
