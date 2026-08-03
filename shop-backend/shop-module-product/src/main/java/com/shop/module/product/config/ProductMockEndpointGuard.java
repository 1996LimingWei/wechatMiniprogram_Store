package com.shop.module.product.config;

import com.shop.common.exception.ServerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 商品兼容 Mock 路径的统一环境守卫。 */
@Component
public class ProductMockEndpointGuard implements HandlerInterceptor {
    private final Environment environment;
    private final boolean enabled;

    public ProductMockEndpointGuard(Environment environment,
                                    @Value("${product.mock-endpoints-enabled:false}") boolean enabled) {
        this.environment = environment;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (environment.acceptsProfiles(Profiles.of("prod")) || !enabled) {
            throw new ServerException(403, "商品 Mock 兼容接口未启用");
        }
        return true;
    }
}
