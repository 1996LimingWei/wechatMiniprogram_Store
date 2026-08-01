package com.shop.module.product.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 商品模块 Web 边界配置。 */
@Configuration
@RequiredArgsConstructor
public class ProductWebConfig implements WebMvcConfigurer {
    private final ProductMockEndpointGuard mockEndpointGuard;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mockEndpointGuard).addPathPatterns("/app-api/mock/**");
    }
}
