package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.trade.dal.dataobject.MarketingShippingRuleDO;
import com.shop.module.trade.dal.mysql.MarketingShippingRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketingShippingService {

    private final MarketingShippingRuleMapper shippingRuleMapper;

    @Value("${trade.freight.free-threshold:19900}")
    private int fallbackFreeThreshold = 19900;

    @Value("${trade.freight.base-fee:1000}")
    private int fallbackBaseFee = 1000;

    /**
     * 计算运费（从数据库包邮规则读取，无规则时兜底使用配置）
     */
    public int calculateFreight(int goodsTotalPrice) {
        MarketingShippingRuleDO rule = shippingRuleMapper.selectOne(
                new LambdaQueryWrapper<MarketingShippingRuleDO>()
                        .eq(MarketingShippingRuleDO::getStatus, 1)
                        .orderByDesc(MarketingShippingRuleDO::getId)
                        .last("LIMIT 1"));
        if (rule == null) {
            return calculateFallback(goodsTotalPrice);
        }
        if (rule.getFreeThreshold() == null || rule.getBaseFee() == null
                || rule.getFreeThreshold() < 0 || rule.getBaseFee() < 0) {
            return calculateFallback(goodsTotalPrice);
        }
        return goodsTotalPrice > 0 && goodsTotalPrice < rule.getFreeThreshold()
                ? rule.getBaseFee() : 0;
    }

    private int calculateFallback(int goodsTotalPrice) {
        if (fallbackFreeThreshold < 0 || fallbackBaseFee < 0) {
            throw new com.shop.common.exception.ServerException(500, "运费配置不正确");
        }
        return goodsTotalPrice > 0 && goodsTotalPrice < fallbackFreeThreshold
                ? fallbackBaseFee : 0;
    }
}
