package com.shop.module.member.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shop.common.exception.ServerException;
import com.shop.module.member.config.WxMaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信小程序 code2session 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMaService {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";

    private static final String PHONE_NUMBER_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={access_token}";

    private final WxMaProperties wxMaProperties;

    /** 缓存 access_token（有效期 2 小时，这里缓存 1.5 小时） */
    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    /**
     * 通过微信 code 换取 openid 和 session_key
     *
     * @param code 小程序 wx.login 获取的 code
     * @return 包含 openid、session_key、unionid(可选) 的结果 Map
     */
    public Map<String, String> code2Session(String code) {
        // Mock 模式：直接返回开发用 openid，不依赖微信接口
        if (wxMaProperties.isMockEnabled()) {
            log.info("[WxMaService] Mock 模式，使用开发 openid, code={}", code);
            Map<String, String> mockResult = new HashMap<>();
            mockResult.put("openid", "dev_openid_" + code);
            mockResult.put("session_key", "dev_session_key");
            return mockResult;
        }

        // 真实模式：调用微信 jscode2session 接口
        String appid = wxMaProperties.getAppid();
        String secret = wxMaProperties.getSecret();
        if (appid == null || appid.isEmpty() || secret == null || secret.isEmpty()) {
            throw new ServerException(500, "微信小程序 appid/secret 未配置");
        }

        String url = CODE2SESSION_URL
                .replace("{appid}", appid)
                .replace("{secret}", secret)
                .replace("{code}", code);

        try {
            String responseBody = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(responseBody);

            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                log.error("[WxMaService] code2session 失败: {}", responseBody);
                throw new ServerException(500, "微信登录失败: " + json.getStr("errmsg"));
            }

            Map<String, String> result = new HashMap<>();
            result.put("openid", json.getStr("openid"));
            result.put("session_key", json.getStr("session_key"));
            if (json.containsKey("unionid")) {
                result.put("unionid", json.getStr("unionid"));
            }
            log.info("[WxMaService] code2session 成功, openid={}", result.get("openid"));
            return result;
        } catch (Exception e) {
            log.error("[WxMaService] code2session 网络异常", e);
            throw new ServerException(500, "微信登录服务暂时不可用");
        }
    }

    /**
     * 通过 phoneCode 获取用户手机号（微信 v2 新版接口）
     *
     * @param phoneCode getPhoneNumber 事件回调中的 code
     * @return 手机号
     */
    public String getPhoneNumber(String phoneCode) {
        if (wxMaProperties.isMockEnabled()) {
            log.info("[WxMaService] Mock 模式，返回模拟手机号, phoneCode={}", phoneCode);
            return "1380000" + String.format("%04d", Math.abs(phoneCode.hashCode() % 10000));
        }

        String accessToken = getAccessToken();
        String url = PHONE_NUMBER_URL.replace("{access_token}", accessToken);

        try {
            JSONObject body = new JSONObject();
            body.set("code", phoneCode);
            String responseBody = HttpUtil.post(url, body.toString(), 5000);
            JSONObject json = JSONUtil.parseObj(responseBody);

            int errcode = json.getInt("errcode", -1);
            if (errcode != 0) {
                log.error("[WxMaService] 获取手机号失败: {}", responseBody);
                throw new ServerException(500, "获取手机号失败: " + json.getStr("errmsg"));
            }

            JSONObject phoneInfo = json.getJSONObject("phone_info");
            if (phoneInfo == null) {
                throw new ServerException(500, "手机号信息为空");
            }

            String phoneNumber = phoneInfo.getStr("purePhoneNumber");
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                phoneNumber = phoneInfo.getStr("phoneNumber");
            }
            log.info("[WxMaService] 获取手机号成功: {}", phoneNumber);
            return phoneNumber;
        } catch (Exception e) {
            log.error("[WxMaService] 获取手机号网络异常", e);
            throw new ServerException(500, "获取手机号服务暂时不可用");
        }
    }

    /**
     * 获取小程序 access_token（带缓存）
     */
    private String getAccessToken() {
        String appid = wxMaProperties.getAppid();
        CachedToken cached = tokenCache.get(appid);
        if (cached != null && !cached.isExpired()) {
            return cached.token;
        }

        String secret = wxMaProperties.getSecret();
        String url = ACCESS_TOKEN_URL
                .replace("{appid}", appid)
                .replace("{secret}", secret);

        try {
            String responseBody = HttpUtil.get(url, 5000);
            JSONObject json = JSONUtil.parseObj(responseBody);

            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                log.error("[WxMaService] 获取 access_token 失败: {}", responseBody);
                throw new ServerException(500, "获取 access_token 失败");
            }

            String token = json.getStr("access_token");
            int expiresIn = json.getInt("expires_in", 7200);
            tokenCache.put(appid, new CachedToken(token, expiresIn));
            log.info("[WxMaService] access_token 刷新成功, expires_in={}", expiresIn);
            return token;
        } catch (Exception e) {
            log.error("[WxMaService] 获取 access_token 网络异常", e);
            throw new ServerException(500, "获取 access_token 失败");
        }
    }

    /** access_token 缓存条目 */
    private static class CachedToken {
        final String token;
        final long expireAt;

        CachedToken(String token, int expiresInSeconds) {
            this.token = token;
            // 提前 5 分钟过期，避免边界问题
            this.expireAt = System.currentTimeMillis() + (expiresInSeconds - 300) * 1000L;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expireAt;
        }
    }
}
