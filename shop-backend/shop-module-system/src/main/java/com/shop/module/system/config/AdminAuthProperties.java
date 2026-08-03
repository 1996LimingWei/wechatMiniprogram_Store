package com.shop.module.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 最小管理员认证配置；生产环境凭据必须通过环境变量注入。 */
@Data
@Component
@ConfigurationProperties(prefix = "admin.auth")
public class AdminAuthProperties {

    private Long userId = 1L;
    private String username;
    private String password;
}
