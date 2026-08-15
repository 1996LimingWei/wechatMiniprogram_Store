package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.MarketingPromotionRuleDO;
import com.shop.module.trade.dal.mysql.MarketingPromotionRuleMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingPromotionServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(MarketingPromotionRuleDO.class);
    }

    @Mock
    private MarketingPromotionRuleMapper promotionRuleMapper;
    @InjectMocks
    private MarketingPromotionService marketingPromotionService;

    // ==================== findBestMatch ====================

    @Test
    void shouldFindBestMatchRule() {
        // 返回按门槛降序排列的规则（模拟 SQL 排序）
        MarketingPromotionRuleDO rule500 = createRule(1L, "满500减100", 50000, 10000);
        MarketingPromotionRuleDO rule200 = createRule(2L, "满200减30", 20000, 3000);
        MarketingPromotionRuleDO rule100 = createRule(3L, "满100减10", 10000, 1000);
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of(rule500, rule200, rule100));

        // 商品金额 25000 分（250元），应匹配满200减30（500门槛不满足）
        MarketingPromotionRuleDO result = marketingPromotionService.findBestMatch(25000);

        assertNotNull(result);
        assertEquals("满200减30", result.getName());
        assertEquals(3000, result.getDiscountAmount());
    }

    @Test
    void shouldMatchHighestThresholdWhenMultipleMatch() {
        MarketingPromotionRuleDO rule500 = createRule(1L, "满500减100", 50000, 10000);
        MarketingPromotionRuleDO rule200 = createRule(2L, "满200减30", 20000, 3000);
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of(rule500, rule200));

        // 金额 60000 分（600元），两个都满足，应选门槛最高的（满500减100）
        MarketingPromotionRuleDO result = marketingPromotionService.findBestMatch(60000);

        assertNotNull(result);
        assertEquals("满500减100", result.getName());
        assertEquals(10000, result.getDiscountAmount());
    }

    @Test
    void shouldReturnNullWhenNoRuleMatches() {
        MarketingPromotionRuleDO rule100 = createRule(1L, "满100减10", 10000, 1000);
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of(rule100));

        // 金额 5000 分（50元），不满足任何规则
        MarketingPromotionRuleDO result = marketingPromotionService.findBestMatch(5000);

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenNoActiveRules() {
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of());

        MarketingPromotionRuleDO result = marketingPromotionService.findBestMatch(100000);

        assertNull(result);
    }

    @Test
    void shouldMatchExactThreshold() {
        MarketingPromotionRuleDO rule = createRule(1L, "满100减10", 10000, 1000);
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of(rule));

        // 金额刚好等于门槛
        MarketingPromotionRuleDO result = marketingPromotionService.findBestMatch(10000);

        assertNotNull(result);
        assertEquals(1000, result.getDiscountAmount());
    }

    // ==================== getActivePromotions ====================

    @Test
    void shouldReturnActivePromotions() {
        MarketingPromotionRuleDO rule1 = createRule(1L, "满100减10", 10000, 1000);
        MarketingPromotionRuleDO rule2 = createRule(2L, "满200减30", 20000, 3000);
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of(rule1, rule2));

        List<MarketingPromotionRuleDO> result = marketingPromotionService.getActivePromotions();

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnEmptyWhenNoActivePromotions() {
        when(promotionRuleMapper.selectList(any())).thenReturn(List.of());

        List<MarketingPromotionRuleDO> result = marketingPromotionService.getActivePromotions();

        assertTrue(result.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private MarketingPromotionRuleDO createRule(Long id, String name, int threshold, int discount) {
        MarketingPromotionRuleDO rule = new MarketingPromotionRuleDO();
        rule.setId(id);
        rule.setName(name);
        rule.setType(1);
        rule.setThresholdAmount(threshold);
        rule.setDiscountAmount(discount);
        rule.setStatus(1);
        rule.setPriority(0);
        return rule;
    }
}
