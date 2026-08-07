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

/**
 * 微信小程序 code2session 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxMaService {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final WxMaProperties wxMaProperties;

    /**
     * 通过微信 code 换取 openid 和 session_key
     *
     * @param code 小程序 wx.login 获取的 code
     * @return 包含 openid、session_key、unionid(可选) 的结果 Map
     */
    public Map<String, String> code2Session(String code) {
        // Mock 模式：直接返回开发用 openid，不依赖微信接口
        if (wxMaProperties.isMockEnabled()) {
            log.info("[WxMaService] Mock 模式登录");
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
            log.info("[WxMaService] code2session 成功");
            return result;
        } catch (Exception e) {
            log.error("[WxMaService] code2session 网络异常", e);
            throw new ServerException(500, "微信登录服务暂时不可用");
        }
    }
}
