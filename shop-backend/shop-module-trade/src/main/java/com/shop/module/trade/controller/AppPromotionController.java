package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.dal.dataobject.MarketingPromotionRuleDO;
import com.shop.module.trade.service.MarketingPromotionService;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppPromotionController {

    private final MarketingPromotionService marketingPromotionService;

    @RequestMapping("/app-api/promotion/current")
    public CommonResult<Map<String, Object>> current() {
        List<MarketingPromotionRuleDO> rules = marketingPromotionService.getActivePromotions();
        List<Map<String, Object>> items = rules.stream().map(rule -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rule.getId());
            item.put("name", rule.getName());
            item.put("thresholdAmount", TradeMoneyUtils.formatYuan(rule.getThresholdAmount()));
            item.put("discountAmount", TradeMoneyUtils.formatYuan(rule.getDiscountAmount()));
            return item;
        }).toList();
        return CommonResult.success(Map.of("list", items));
    }
}
