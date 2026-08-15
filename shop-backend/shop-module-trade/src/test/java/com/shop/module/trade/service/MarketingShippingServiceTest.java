package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.MarketingShippingRuleDO;
import com.shop.module.trade.dal.mysql.MarketingShippingRuleMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingShippingServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(MarketingShippingRuleDO.class);
    }

    @Mock
    private MarketingShippingRuleMapper shippingRuleMapper;
    @InjectMocks
    private MarketingShippingService marketingShippingService;

    @Test
    void shouldChargeFreightWhenBelowThreshold() {
        MarketingShippingRuleDO rule = createRule(19900, 1000);
        when(shippingRuleMapper.selectOne(any())).thenReturn(rule);

        int freight = marketingShippingService.calculateFreight(10000);

        assertEquals(1000, freight);
    }

    @Test
    void shouldFreeShippingWhenAboveThreshold() {
        MarketingShippingRuleDO rule = createRule(19900, 1000);
        when(shippingRuleMapper.selectOne(any())).thenReturn(rule);

        int freight = marketingShippingService.calculateFreight(20000);

        assertEquals(0, freight);
    }

    @Test
    void shouldFreeShippingWhenExactThreshold() {
        MarketingShippingRuleDO rule = createRule(19900, 1000);
        when(shippingRuleMapper.selectOne(any())).thenReturn(rule);

        int freight = marketingShippingService.calculateFreight(19900);

        assertEquals(0, freight);
    }

    @Test
    void shouldUseFallbackWhenNoRuleInDatabase() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(null);
        ReflectionTestUtils.setField(marketingShippingService, "fallbackFreeThreshold", 19900);
        ReflectionTestUtils.setField(marketingShippingService, "fallbackBaseFee", 1000);

        int freight = marketingShippingService.calculateFreight(10000);

        assertEquals(1000, freight);
    }

    @Test
    void shouldFreeShippingByFallbackWhenAboveThreshold() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(null);
        ReflectionTestUtils.setField(marketingShippingService, "fallbackFreeThreshold", 19900);
        ReflectionTestUtils.setField(marketingShippingService, "fallbackBaseFee", 1000);

        int freight = marketingShippingService.calculateFreight(20000);

        assertEquals(0, freight);
    }

    @Test
    void shouldReturnZeroFreightWhenGoodsPriceIsZero() {
        MarketingShippingRuleDO rule = createRule(19900, 1000);
        when(shippingRuleMapper.selectOne(any())).thenReturn(rule);

        int freight = marketingShippingService.calculateFreight(0);

        assertEquals(0, freight);
    }

    @Test
    void shouldUseFallbackWhenRuleHasInvalidConfig() {
        MarketingShippingRuleDO rule = createRule(-1, -1);
        when(shippingRuleMapper.selectOne(any())).thenReturn(rule);
        ReflectionTestUtils.setField(marketingShippingService, "fallbackFreeThreshold", 19900);
        ReflectionTestUtils.setField(marketingShippingService, "fallbackBaseFee", 1000);

        int freight = marketingShippingService.calculateFreight(10000);

        assertEquals(1000, freight);
    }

    private MarketingShippingRuleDO createRule(int freeThreshold, int baseFee) {
        MarketingShippingRuleDO rule = new MarketingShippingRuleDO();
        rule.setId(1L);
        rule.setName("默认包邮规则");
        rule.setFreeThreshold(freeThreshold);
        rule.setBaseFee(baseFee);
        rule.setStatus(1);
        return rule;
    }
}
