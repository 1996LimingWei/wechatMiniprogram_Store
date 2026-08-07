package com.shop.module.member.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.framework.security.LoginUser;
import com.shop.framework.security.TokenService;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${member.gold-card.mock-subscribe-enabled:false}")
    private boolean mockSubscribeEnabled;

    /**
     * 会员中心首页 — 返回当前会员等级、权益概览
     */
    @RequestMapping("/center")
    public CommonResult<Map<String, Object>> center(HttpServletRequest request) {
        LoginUser loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            return CommonResult.error(401, "请先登录");
        }
        MemberUserDO user = memberUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }

        int level = user.getMemberLevel() != null ? user.getMemberLevel() : 1;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberLevel", level);
        data.put("memberLevelName", level == 2 ? "黄金会员" : "白银会员");
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("mobile", user.getMobile() != null ? user.getMobile() : "");
        data.put("purchaseEnabled", mockSubscribeEnabled);
        data.put("purchaseMessage", mockSubscribeEnabled ? "开发环境体验开通" : "黄金会员服务暂未开放");

        // 当前等级权益
        Map<String, Object> currentBenefits = new LinkedHashMap<>();
        currentBenefits.put("discount", mockSubscribeEnabled && level == 2 ? "全场9折" : "无专属折扣");
        currentBenefits.put("shipping", mockSubscribeEnabled && level == 2 ? "优先发货" : "标准发货");
        currentBenefits.put("coupon", mockSubscribeEnabled && level == 2 ? "每月赠送优惠券" : "无专属优惠券");
        data.put("benefits", currentBenefits);

        return CommonResult.success(data);
    }

    /**
     * 黄金卡详情 — 展示黄金会员权益和价格
     */
    @RequestMapping("/gold-card")
    public CommonResult<Map<String, Object>> goldCard(HttpServletRequest request) {
        LoginUser loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            return CommonResult.error(401, "请先登录");
        }
        MemberUserDO user = memberUserMapper.selectById(loginUser.getUserId());
        int currentLevel = (user != null && user.getMemberLevel() != null) ? user.getMemberLevel() : 1;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentLevel", currentLevel);
        data.put("isGold", currentLevel == 2);
        data.put("purchaseEnabled", mockSubscribeEnabled);
        data.put("purchaseMessage", mockSubscribeEnabled ? "开发环境体验开通" : "黄金会员服务暂未开放");

        if (mockSubscribeEnabled) {
            data.put("price", 9900);
            data.put("originalPrice", 19900);
            data.put("duration", "365天");
            data.put("dailyPrice", "0.27元/天");
            data.put("benefits", new Object[]{
                    Map.of("icon", "discount", "title", "专享折扣", "desc", "全场商品享受9折优惠"),
                    Map.of("icon", "shipping", "title", "优先发货", "desc", "订单优先处理，更快送达"),
                    Map.of("icon", "coupon", "title", "每月优惠券", "desc", "每月赠送满100减20优惠券"),
                    Map.of("icon", "service", "title", "专属客服", "desc", "VIP专属客服通道，优先响应"),
                    Map.of("icon", "birthday", "title", "生日礼遇", "desc", "生日当月双倍积分+专属礼品")
            });
        } else {
            data.put("benefits", new Object[0]);
        }

        return CommonResult.success(data);
    }

    /**
     * 开通黄金会员（Mock，无真实支付）
     */
    @RequestMapping("/gold-card/subscribe")
    public CommonResult<Map<String, Object>> subscribe(HttpServletRequest request) {
        if (!mockSubscribeEnabled) {
            return CommonResult.error(403, "黄金会员购买暂未开放");
        }
        LoginUser loginUser = resolveLoginUser(request);
        if (loginUser == null) {
            return CommonResult.error(401, "请先登录");
        }
        MemberUserDO user = memberUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            return CommonResult.error(404, "用户不存在");
        }
        if (user.getMemberLevel() != null && user.getMemberLevel() == 2) {
            return CommonResult.error(400, "您已经是黄金会员");
        }

        // Mock：直接升级为黄金会员
        user.setMemberLevel(2);
        memberUserMapper.updateById(user);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberLevel", 2);
        data.put("memberLevelName", "黄金会员");
        data.put("message", "恭喜！您已成功开通黄金会员（体验模式）");
        return CommonResult.success(data);
    }

    private LoginUser resolveLoginUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return tokenService.getLoginUser(token);
    }
}
