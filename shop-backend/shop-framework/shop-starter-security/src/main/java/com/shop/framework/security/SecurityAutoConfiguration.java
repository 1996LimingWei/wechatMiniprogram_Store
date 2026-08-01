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
                // 开发阶段：放行所有小程序接口
                .requestMatchers("/app-api/**").permitAll()
                .requestMatchers("/admin-api/auth/login").permitAll()
                .requestMatchers("/admin-api/**").hasRole("ADMIN")
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
