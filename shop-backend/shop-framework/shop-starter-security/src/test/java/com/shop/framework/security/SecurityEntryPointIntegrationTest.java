package com.shop.framework.security;

import com.shop.common.config.DeliveryMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 使用实际 Spring Security 过滤链，验证外部渠道通知无需会员 Token。 */
@SpringBootTest(classes = SecurityEntryPointIntegrationTest.Application.class,
        properties = "app.delivery-mode=commerce")
@AutoConfigureMockMvc
class SecurityEntryPointIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void refundNotificationReachesSignatureHandlerWithoutUserToken() throws Exception {
        mvc.perform(post("/app-api/pay/wechat/refund-notify")).andExpect(status().isOk());
    }

    @Test void phoneAuthorizationReachesLoginHandlerWithoutUserToken() throws Exception {
        mvc.perform(post("/app-api/auth/phone-login")).andExpect(status().isOk());
    }

    @Test void productWritesStillRequireAdminLogin() throws Exception {
        mvc.perform(post("/admin-api/product/spu/save")).andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, RedisAutoConfiguration.class})
    @Import({SecurityAutoConfiguration.class, DeliveryMode.class, Probe.class})
    static class Application {
        @Bean TokenService tokenService() { return mock(TokenService.class); }
        @Bean JdbcTemplate jdbcTemplate() { return mock(JdbcTemplate.class); }
    }

    @RestController
    static class Probe {
        @PostMapping({"/app-api/pay/wechat/refund-notify", "/app-api/auth/phone-login"})
        String handler() { return "reached"; }
    }
}
