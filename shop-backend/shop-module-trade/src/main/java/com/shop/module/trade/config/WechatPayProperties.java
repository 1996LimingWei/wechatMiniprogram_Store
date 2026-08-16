package com.shop.module.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WechatPayProperties {

    private boolean enabled;
    private String appId;
    private String mchId;
    private String merchantSerialNo;
    private String privateKeyPath;
    private String apiV3Key;
    private String platformCertificatePath;
    private String notifyUrl;
    private String refundNotifyUrl;
}
