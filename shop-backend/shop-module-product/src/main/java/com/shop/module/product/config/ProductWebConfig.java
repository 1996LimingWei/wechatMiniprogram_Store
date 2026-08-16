package com.shop.module.product.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 商品模块 Web 边界配置。 */
@Configuration
@RequiredArgsConstructor
public class ProductWebConfig implements WebMvcConfigurer {
    private final ProductMockEndpointGuard mockEndpointGuard;
    private final MaterialStorageProperties materialStorageProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mockEndpointGuard).addPathPatterns("/app-api/mock/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(materialStorageProperties.getRoot()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        registry.addResourceHandler("/uploads/material/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
