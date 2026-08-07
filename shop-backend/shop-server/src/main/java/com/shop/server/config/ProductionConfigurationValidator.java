package com.shop.server.config;

import com.shop.module.member.config.WxMaProperties;
import com.shop.module.system.config.AdminAuthProperties;
import com.shop.module.trade.config.TradeMockActionProperties;
import com.shop.module.trade.config.WechatPayProperties;
import com.shop.module.trade.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionConfigurationValidator implements ApplicationRunner {

    private final Environment environment;
    private final WxMaProperties wxMaProperties;
    private final AdminAuthProperties adminAuthProperties;
    private final TradeMockActionProperties tradeMockActionProperties;
    private final WechatPayProperties wechatPayProperties;
    private final WechatPayService wechatPayService;

    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = new ArrayList<>();
        requireText("spring.datasource.url", missing);
        requireText("spring.datasource.username", missing);
        requireText("spring.datasource.password", missing);
        requireText("spring.data.redis.host", missing);
        requireText("spring.data.redis.password", missing);

        if (isBlank(wxMaProperties.getAppid())) {
            missing.add("wx.ma.appid");
        }
        if (isBlank(wxMaProperties.getSecret())) {
            missing.add("wx.ma.secret");
        }
        if (wxMaProperties.isMockEnabled()) {
            missing.add("wx.ma.mock-enabled 必须为 false");
        }
        if (!isBlank(wxMaProperties.getAppid())
                && !wxMaProperties.getAppid().equals(wechatPayProperties.getAppId())) {
            missing.add("wx.ma.appid 与 wechat.pay.app-id 必须一致");
        }
        if (tradeMockActionProperties.isMockActionsEnabled()) {
            missing.add("trade.mock-actions-enabled 必须为 false");
        }
        if (environment.getProperty("product.mock-endpoints-enabled", Boolean.class, false)) {
            missing.add("product.mock-endpoints-enabled 必须为 false");
        }
        if (environment.getProperty("member.gold-card.mock-subscribe-enabled", Boolean.class, false)) {
            missing.add("member.gold-card.mock-subscribe-enabled 必须为 false");
        }
        if (!"wechat".equals(environment.getProperty("trade.refund.provider"))) {
            missing.add("trade.refund.provider 必须为 wechat");
        }
        String corsOrigins = environment.getProperty("web.cors.allowed-origin-patterns");
        if (isBlank(corsOrigins) || corsOrigins.contains("*")) {
            missing.add("web.cors.allowed-origin-patterns 必须配置明确来源且不能包含通配符");
        }
        if (isBlank(adminAuthProperties.getUsername())) {
            missing.add("admin.auth.username");
        }
        if (isBlank(adminAuthProperties.getPassword())) {
            missing.add("admin.auth.password");
        } else if ("admin123".equals(adminAuthProperties.getPassword())) {
            missing.add("admin.auth.password 不能使用开发默认值");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("生产配置不完整: " + String.join(", ", missing));
        }
        wechatPayService.validateCredentialFiles();
    }

    private void requireText(String key, List<String> missing) {
        if (isBlank(environment.getProperty(key))) {
            missing.add(key);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
