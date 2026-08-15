package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.dal.dataobject.MarketingCouponDO;
import com.shop.module.trade.dal.dataobject.MarketingCouponTemplateDO;
import com.shop.module.trade.service.MarketingCouponService;
import com.shop.module.trade.util.TradeMoneyUtils;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppCouponController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MarketingCouponService marketingCouponService;

    @RequestMapping("/app-api/coupon/list")
    public CommonResult<Map<String, Object>> list(@RequestBody(required = false) String rawBody,
                                                   @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Integer status = TradeRequestUtils.getInt(request, "status", null);
        List<MarketingCouponDO> coupons = marketingCouponService.getUserCoupons(userId, status);
        List<Map<String, Object>> items = coupons.stream().map(this::toCouponItem).toList();
        return CommonResult.success(Map.of("list", items));
    }

    @RequestMapping("/app-api/coupon/available")
    public CommonResult<Map<String, Object>> available(@RequestBody(required = false) String rawBody,
                                                        @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        int goodsTotalPrice = TradeRequestUtils.getInt(request, "goodsTotalPrice", 0);
        List<MarketingCouponDO> coupons = marketingCouponService.getAvailableCoupons(userId, goodsTotalPrice);
        List<Map<String, Object>> items = coupons.stream().map(this::toCouponItem).toList();
        return CommonResult.success(Map.of("list", items));
    }

    @RequestMapping("/app-api/coupon/claim")
    public CommonResult<Map<String, Object>> claim(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long templateId = TradeRequestUtils.getLong(request, "templateId", 0L);
        MarketingCouponDO coupon = marketingCouponService.claimCoupon(userId, templateId);
        return CommonResult.success(toCouponItem(coupon));
    }

    @RequestMapping("/app-api/coupon/claimable")
    public CommonResult<Map<String, Object>> claimable() {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        List<MarketingCouponTemplateDO> templates = marketingCouponService.getClaimableTemplates(userId);
        List<Map<String, Object>> items = templates.stream().map(tpl -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tpl.getId());
            item.put("name", tpl.getName());
            item.put("type", tpl.getType());
            item.put("thresholdAmount", TradeMoneyUtils.formatYuan(tpl.getThresholdAmount()));
            item.put("discountAmount", TradeMoneyUtils.formatYuan(tpl.getDiscountAmount()));
            item.put("totalCount", tpl.getTotalCount());
            item.put("claimedCount", tpl.getClaimedCount());
            item.put("validEndTime", tpl.getValidEndTime() == null ? "" : tpl.getValidEndTime().format(TIME_FORMATTER));
            return item;
        }).toList();
        return CommonResult.success(Map.of("list", items));
    }

    private Map<String, Object> toCouponItem(MarketingCouponDO coupon) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", coupon.getId());
        item.put("templateId", coupon.getTemplateId());
        item.put("status", coupon.getStatus());
        item.put("orderId", coupon.getOrderId());
        item.put("expireTime", coupon.getExpireTime() == null ? "" : coupon.getExpireTime().format(TIME_FORMATTER));
        item.put("usedTime", coupon.getUsedTime() == null ? "" : coupon.getUsedTime().format(TIME_FORMATTER));
        MarketingCouponTemplateDO tpl = marketingCouponService.getTemplate(coupon.getTemplateId());
        if (tpl != null) {
            item.put("name", tpl.getName());
            item.put("type", tpl.getType());
            item.put("thresholdAmount", TradeMoneyUtils.formatYuan(tpl.getThresholdAmount()));
            item.put("discountAmount", TradeMoneyUtils.formatYuan(tpl.getDiscountAmount()));
        }
        return item;
    }
}
