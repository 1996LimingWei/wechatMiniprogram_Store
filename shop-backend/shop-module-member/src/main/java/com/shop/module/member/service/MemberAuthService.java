package com.shop.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.framework.security.LoginUser;
import com.shop.framework.security.TokenService;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会员认证服务：登录、注册、Token 管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAuthService {

    private final MemberUserMapper memberUserMapper;
    private final TokenService tokenService;
    private final WxMaService wxMaService;

    /**
     * 微信小程序静默登录（code2session）
     *
     * @param code 微信 wx.login 获取的 code
     * @return 登录结果（token、userInfo、userId）
     */
    public Map<String, Object> loginByWeixin(String code) {
        // 1. 通过 code 换取 openid 和 session_key
        Map<String, String> wxResult = wxMaService.code2Session(code);
        String openid = wxResult.get("openid");
        String sessionKey = wxResult.get("session_key");
        String unionid = wxResult.get("unionid");

        // 2. 查找或创建用户
        MemberUserDO user = memberUserMapper.selectOne(
                new LambdaQueryWrapper<MemberUserDO>().eq(MemberUserDO::getOpenid, openid)
        );

        if (user == null) {
            // 新用户：自动注册
            user = new MemberUserDO();
            user.setOpenid(openid);
            user.setSessionKey(sessionKey);
            user.setUnionid(unionid);
            user.setNickname("微信用户");
            user.setAvatar("");
            user.setStatus(1);
            user.setMemberLevel(1); // 新用户自动绑定白银会员
            memberUserMapper.insert(user);
            log.info("[MemberAuth] 新用户注册, userId={}, openid={}", user.getId(), openid);
        } else {
            // 老用户：更新 session_key
            user.setSessionKey(sessionKey);
            if (unionid != null) {
                user.setUnionid(unionid);
            }
            memberUserMapper.updateById(user);
            log.info("[MemberAuth] 用户登录, userId={}, openid={}", user.getId(), openid);
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new ServerException(403, "账号已被禁用");
        }

        // 4. 生成 Token
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUserType(1); // 1=会员
        String token = tokenService.createToken(loginUser);

        // 5. 构造响应（字段名与前端 ucenter/index.vue 的 isLogin 判断对齐）
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("mobile", user.getMobile() != null ? user.getMobile() : "");
        userInfo.put("memberLevel", user.getMemberLevel() != null ? user.getMemberLevel() : 1);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("userInfo", userInfo);
        data.put("userId", user.getId());
        return data;
    }

    /**
     * 刷新 Token：用旧 token 换新 token
     *
     * @param oldToken 旧 token
     * @return 新 token
     */
    public String refreshToken(String oldToken) {
        LoginUser loginUser = tokenService.getLoginUser(oldToken);
        if (loginUser == null) {
            return null; // token 已失效，需重新登录
        }
        // 删除旧 token，创建新 token
        tokenService.deleteToken(oldToken);
        return tokenService.createToken(loginUser);
    }

    /**
     * 退出登录
     */
    public void logout(String token) {
        if (token != null) {
            tokenService.deleteToken(token);
        }
    }

    /**
     * 获取当前登录用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        MemberUserDO user = memberUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("nickName", user.getNickname());
        userInfo.put("avatarUrl", user.getAvatar());
        userInfo.put("mobile", user.getMobile() != null
                ? user.getMobile().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")
                : "");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userInfo", userInfo);
        return data;
    }
}
