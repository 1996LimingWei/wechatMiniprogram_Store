package com.shop.module.member.controller;

import com.shop.framework.security.LoginUser;
import com.shop.framework.security.TokenService;
import com.shop.module.member.service.MemberAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序登录认证接口
 */
@Slf4j
@RestController
@RequestMapping("/app-api/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final MemberAuthService memberAuthService;
    private final TokenService tokenService;

    /**
     * 微信小程序静默登录
     * 前端 wx.login 获取 code 后，POST 到此接口
     */
    @RequestMapping("/LoginByMa")
    public Map<String, Object> loginByMa(@RequestBody(required = false) Map<String, Object> body) {
        String code = "";
        if (body != null && body.get("code") != null) {
            code = body.get("code").toString();
        }
        if (code.isEmpty()) {
            return fail("缺少微信登录 code");
        }
        try {
            Map<String, Object> data = memberAuthService.loginByWeixin(code);
            return success(data);
        } catch (Exception e) {
            log.error("[Auth] 微信登录异常", e);
            return fail(e.getMessage());
        }
    }

    /**
     * 兼容旧接口：通过 URL 路径传递 code 登录
     */
    @RequestMapping("/code")
    public Map<String, Object> loginByCode(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isEmpty()) {
            return fail("缺少微信登录 code");
        }
        try {
            Map<String, Object> data = memberAuthService.loginByWeixin(code);
            return success(data);
        } catch (Exception e) {
            log.error("[Auth] 微信登录异常", e);
            return fail(e.getMessage());
        }
    }

    /**
     * 手机号快速登录（微信 v2 新版接口）
     * 前端通过 getPhoneNumber 按钮获取 phoneCode，连同 wx.login 的 code 一起发送
     */
    @RequestMapping("/phone-login")
    public Map<String, Object> loginByPhone(@RequestBody(required = false) Map<String, Object> body) {
        String code = "";
        String phoneCode = "";
        if (body != null) {
            if (body.get("code") != null) code = body.get("code").toString();
            if (body.get("phoneCode") != null) phoneCode = body.get("phoneCode").toString();
        }
        if (code.isEmpty()) {
            return fail("缺少微信登录 code");
        }
        if (phoneCode.isEmpty()) {
            return fail("缺少手机号授权 code");
        }
        try {
            Map<String, Object> data = memberAuthService.loginByPhone(code, phoneCode);
            return success(data);
        } catch (Exception e) {
            log.error("[Auth] 手机号登录异常", e);
            return fail(e.getMessage());
        }
    }

    /**
     * 刷新 Token
     * 前端收到 401 时，携带旧 token 调用此接口
     */
    @RequestMapping("/refresh-token")
    public Map<String, Object> refreshToken(@RequestBody(required = false) Map<String, Object> body) {
        String oldToken = null;
        if (body != null && body.get("token") != null) {
            oldToken = body.get("token").toString();
        }
        if (oldToken == null || oldToken.isEmpty()) {
            return fail("缺少 token");
        }
        String newToken = memberAuthService.refreshToken(oldToken);
        if (newToken == null) {
            return fail(401, "Token 已失效，请重新登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", newToken);
        return success(data);
    }

    /**
     * 退出登录
     */
    @RequestMapping("/logout")
    public Map<String, Object> logout() {
        String token = resolveRawToken();
        memberAuthService.logout(token);
        return success(null);
    }

    /**
     * 获取当前登录用户信息（需认证）
     */
    @RequestMapping("/user-info")
    public Map<String, Object> getUserInfo() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return fail(401, "未登录");
        }
        Map<String, Object> data = memberAuthService.getUserInfo(loginUser.getUserId());
        if (data == null) {
            return fail("用户不存在");
        }
        return success(data);
    }

    // ===== helpers =====

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        return null;
    }

    /**
     * 从 SecurityContext 中取出原始 token 字符串（Filter 存入 credentials）
     */
    private String resolveRawToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String) {
            return (String) auth.getCredentials();
        }
        return null;
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }

    private Map<String, Object> fail(String msg) {
        return fail(500, msg);
    }

    private Map<String, Object> fail(int code, String msg) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("msg", msg);
        result.put("data", null);
        return result;
    }
}
