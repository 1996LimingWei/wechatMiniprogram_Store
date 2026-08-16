package com.shop.module.member.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.common.exception.ServerException;
import com.shop.framework.security.LoginUser;
import com.shop.framework.security.TokenService;
import com.shop.module.member.config.MemberFeatureProperties;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import com.shop.module.member.vo.MemberProfileUpdateReqVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序端 — 会员中心
 */
@RestController
@RequestMapping("/app-api/member")
@RequiredArgsConstructor
public class AppMemberController {

    private final MemberUserMapper memberUserMapper;
    private final TokenService tokenService;
    private final MemberFeatureProperties memberFeatureProperties;

    @GetMapping("/center")
    public CommonResult<Void> center() {
        return membershipUnavailable();
    }

    @GetMapping("/gold-card")
    public CommonResult<Void> goldCard() {
        return membershipUnavailable();
    }

    @PostMapping("/gold-card/subscribe")
    public CommonResult<Void> subscribe() {
        return membershipUnavailable();
    }

    /**
     * 会员资料编辑尚未交付对象存储和手机号授权闭环，默认拒绝写入。
     */
    @PostMapping("/profile")
    public CommonResult<Map<String, Object>> updateProfile(
            @RequestBody MemberProfileUpdateReqVO body,
            HttpServletRequest request) {
        if (!memberFeatureProperties.isProfileEditEnabled()) {
            throw new ServerException(403, "个人资料编辑暂未开放");
        }
        LoginUser loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            return CommonResult.error(401, "请先登录");
        }
        MemberUserDO user = memberUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }

        String nickname = body == null ? "" : normalize(body.getNickname());
        if (nickname.length() < 1 || nickname.length() > 20) {
            throw new ServerException(400, "昵称应为 1 至 20 个字符");
        }
        user.setNickname(nickname);
        String avatar = body == null ? "" : normalize(body.getAvatar());
        if (!avatar.isEmpty()) {
            if (avatar.length() > 512 || avatar.startsWith("wxfile://") || avatar.startsWith("http://tmp")
                    || !(avatar.startsWith("https://") || avatar.startsWith("/static/"))) {
                throw new ServerException(400, "头像地址必须是已上传的 HTTPS 或站内静态资源");
            }
            user.setAvatar(avatar);
        }
        memberUserMapper.updateById(user);

        // 返回更新后的用户信息
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("mobile", user.getMobile() != null ? user.getMobile() : "");
        data.put("memberLevel", user.getMemberLevel() != null ? user.getMemberLevel() : 1);
        return CommonResult.success(data);
    }

    @GetMapping("/availability")
    public CommonResult<Map<String, Boolean>> availability() {
        return CommonResult.success(Map.of(
                "membershipEnabled", memberFeatureProperties.isMembershipEnabled(),
                "profileEditEnabled", memberFeatureProperties.isProfileEditEnabled()));
    }

    private CommonResult<Void> membershipUnavailable() {
        if (memberFeatureProperties.isMembershipEnabled()) {
            throw new ServerException(503, "会员权益模块尚未完成交付");
        }
        throw new ServerException(403, "会员权益暂未开放");
    }

    private LoginUser resolveLoginUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return tokenService.getLoginUser(token);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
