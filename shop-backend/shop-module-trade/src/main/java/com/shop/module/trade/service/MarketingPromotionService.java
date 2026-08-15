package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.trade.dal.dataobject.MarketingPromotionRuleDO;
import com.shop.module.trade.dal.mysql.MarketingPromotionRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketingPromotionService {

    private final MarketingPromotionRuleMapper promotionRuleMapper;

    /**
     * 查找最优满减规则（门槛最高且满足条件的规则）
     */
    public MarketingPromotionRuleDO findBestMatch(int goodsTotalPrice) {
        LocalDateTime now = LocalDateTime.now();
        List<MarketingPromotionRuleDO> rules = promotionRuleMapper.selectList(
                new LambdaQueryWrapper<MarketingPromotionRuleDO>()
                        .eq(MarketingPromotionRuleDO::getStatus, 1)
                        .and(w -> w
                                .isNull(MarketingPromotionRuleDO::getStartTime)
                                .or().le(MarketingPromotionRuleDO::getStartTime, now))
                        .and(w -> w
                                .isNull(MarketingPromotionRuleDO::getEndTime)
                                .or().ge(MarketingPromotionRuleDO::getEndTime, now))
                        .orderByDesc(MarketingPromotionRuleDO::getThresholdAmount));
        return rules.stream()
                .filter(rule -> goodsTotalPrice >= rule.getThresholdAmount())
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询当前生效的满减活动列表
     */
    public List<MarketingPromotionRuleDO> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRuleMapper.selectList(
                new LambdaQueryWrapper<MarketingPromotionRuleDO>()
                        .eq(MarketingPromotionRuleDO::getStatus, 1)
                        .and(w -> w
                                .isNull(MarketingPromotionRuleDO::getStartTime)
                                .or().le(MarketingPromotionRuleDO::getStartTime, now))
                        .and(w -> w
                                .isNull(MarketingPromotionRuleDO::getEndTime)
                                .or().ge(MarketingPromotionRuleDO::getEndTime, now))
                        .orderByAsc(MarketingPromotionRuleDO::getThresholdAmount));
    }
}
