package com.shop.module.system.controller;

import com.shop.common.exception.ServerException;
import com.shop.common.pojo.CommonResult;
import com.shop.framework.security.LoginUser;
import com.shop.framework.security.TokenService;
import com.shop.module.system.config.AdminAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthProperties adminAuthProperties;
    private final TokenService tokenService;

    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@RequestBody(required = false) Map<String, String> request) {
        if (!StringUtils.hasText(adminAuthProperties.getUsername()) || !StringUtils.hasText(adminAuthProperties.getPassword())) {
            throw new ServerException(503, "管理员认证未配置");
        }
        String username = request == null ? null : request.get("username");
        String password = request == null ? null : request.get("password");
        if (!adminAuthProperties.getUsername().equals(username) || !adminAuthProperties.getPassword().equals(password)) {
            throw new ServerException(401, "管理员账号或密码错误");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(adminAuthProperties.getUserId());
        loginUser.setUserType(2);
        return CommonResult.success(Map.of("token", tokenService.createToken(loginUser), "userId", loginUser.getUserId()));
    }
}
