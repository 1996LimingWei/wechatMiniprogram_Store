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
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenService tokenService,
                                           JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) throws Exception {
        TokenAuthenticationFilter tokenFilter = new TokenAuthenticationFilter(tokenService);
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 登录与 Token 刷新是匿名入口，其余认证接口由后续规则保护。
                .requestMatchers(
                        "/app-api/auth/LoginByMa",
                        "/app-api/auth/refresh-token",
                        "/app-api/pay/wechat/notify"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // 本地素材地址会直接写入商品与内容数据，必须允许小程序和后台图片标签匿名读取。
                .requestMatchers("/uploads/material/**").permitAll()
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
                        "/app-api/mock/**",
                        "/app-api/promotion/current"
                ).permitAll()
                .requestMatchers("/app-api/**").authenticated()
                .requestMatchers("/admin-api/auth/login").permitAll()
                .requestMatchers("/admin-api/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        new ObjectMapper().writeValueAsString(CommonResult.error(ErrorCode.UNAUTHORIZED))
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                            objectMapper.writeValueAsString(CommonResult.error(403, "没有访问权限")));
                })
            )
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new AdminSecurityFilter(jdbcTemplate, objectMapper), TokenAuthenticationFilter.class);
        return http.build();
    }
}
