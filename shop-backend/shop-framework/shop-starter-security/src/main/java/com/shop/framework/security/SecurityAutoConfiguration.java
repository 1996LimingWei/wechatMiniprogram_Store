package com.shop.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.pojo.CommonResult;
import com.shop.common.exception.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenService tokenService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 登录与 Token 刷新是匿名入口，其余认证接口由后续规则保护。
                .requestMatchers(
                        "/app-api/auth/LoginByMa",
                        "/app-api/auth/code",
                        "/app-api/auth/refresh-token"
                ).permitAll()
                // 商品、首页、品牌、专题、帮助与公开评价允许游客浏览。
                .requestMatchers(
                        "/app-api/product/**",
                        "/app-api/catalog/**",
                        "/app-api/goods/**",
                        "/app-api/index/**",
                        "/app-api/search/**",
                        "/app-api/brand/**",
                        "/app-api/topic/**",
                        "/app-api/helpissue/**",
                        "/app-api/comment/list",
                        "/app-api/comment/count",
                        "/app-api/region/list",
                        "/app-api/mock/**"
                ).permitAll()
                // 管理端认证与 RBAC 尚未完成前一律拒绝，避免匿名读取订单和操作资金。
                .requestMatchers("/admin-api/**").denyAll()
                .requestMatchers("/app-api/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        new ObjectMapper().writeValueAsString(CommonResult.error(ErrorCode.UNAUTHORIZED))
                    );
                })
            )
            .addFilterBefore(new TokenAuthenticationFilter(tokenService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
