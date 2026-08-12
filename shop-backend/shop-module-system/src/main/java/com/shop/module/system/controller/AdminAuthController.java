package com.shop.module.system.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.system.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;
import com.shop.framework.security.LoginUser;

@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@RequestBody(required = false) Map<String, String> body,
                                                    HttpServletRequest request) {
        String username = body == null ? null : body.get("username");
        String password = body == null ? null : body.get("password");
        return CommonResult.success(adminAuthService.login(username, password, request));
    }

    @PostMapping("/logout")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : "";
        adminAuthService.logout(token);
        return CommonResult.success(true);
    }

    @GetMapping("/profile")
    public CommonResult<Map<String, Object>> profile() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser loginUser) || !Integer.valueOf(2).equals(loginUser.getUserType())) {
            throw new com.shop.common.exception.ServerException(401, "管理员未登录");
        }
        return CommonResult.success(adminAuthService.profile(loginUser.getUserId()));
    }
}
