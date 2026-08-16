package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.trade.dal.dataobject.MarketingShippingRuleDO;
import com.shop.module.trade.dal.mysql.MarketingShippingRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        MarketingShippingRuleDO rule = getCurrentRule();
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

    /**
     * 全局运费规则优先级：在有效期内且启用的数据库规则优先，按生效时间和 ID 最新规则胜出；没有规则时才使用配置兜底。
     * 规则内部优先执行免邮门槛，达到门槛时运费为 0，否则收取基础运费。
     */
    public MarketingShippingRuleDO getCurrentRule() {
        LocalDateTime now = LocalDateTime.now();
        return shippingRuleMapper.selectOne(
                new LambdaQueryWrapper<MarketingShippingRuleDO>()
                        .eq(MarketingShippingRuleDO::getStatus, 1)
                        .and(wrapper -> wrapper.isNull(MarketingShippingRuleDO::getStartTime)
                                .or()
                                .le(MarketingShippingRuleDO::getStartTime, now))
                        .and(wrapper -> wrapper.isNull(MarketingShippingRuleDO::getEndTime)
                                .or()
                                .gt(MarketingShippingRuleDO::getEndTime, now))
                        .orderByDesc(MarketingShippingRuleDO::getStartTime)
                        .orderByDesc(MarketingShippingRuleDO::getId)
                        .last("LIMIT 1"));
    }

    private int calculateFallback(int goodsTotalPrice) {
        if (fallbackFreeThreshold < 0 || fallbackBaseFee < 0) {
            throw new com.shop.common.exception.ServerException(500, "运费配置不正确");
        }
        return goodsTotalPrice > 0 && goodsTotalPrice < fallbackFreeThreshold
                ? fallbackBaseFee : 0;
    }
}
