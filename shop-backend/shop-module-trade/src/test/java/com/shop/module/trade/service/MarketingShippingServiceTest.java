package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MarketingShippingRuleDO;
import com.shop.module.trade.dal.mysql.MarketingShippingRuleMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketingShippingServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(MarketingShippingRuleDO.class);
    }

    @Mock
    private MarketingShippingRuleMapper shippingRuleMapper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private MarketingShippingService marketingShippingService;

    @Test
    void shouldChargeFreightWhenBelowThreshold() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));

        assertEquals(1000, marketingShippingService.calculateFreight(10000));
    }

    @Test
    void shouldFreeShippingWhenAboveThreshold() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));

        assertEquals(0, marketingShippingService.calculateFreight(20000));
    }

    @Test
    void shouldFreeShippingWhenExactThreshold() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));

        assertEquals(0, marketingShippingService.calculateFreight(19900));
    }

    @Test
    void shouldReturnZeroFreightWhenGoodsPriceIsZero() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));

        assertEquals(0, marketingShippingService.calculateFreight(0));
    }

    @Test
    void shouldFailFastWhenNoRuleConfigured() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(null);

        ServerException error = assertThrows(ServerException.class,
                () -> marketingShippingService.calculateFreight(10000));
        assertEquals("运费规则未配置，请先在后台「营销 → 包邮规则」设置", error.getMessage());
    }

    @Test
    void shouldFailFastWhenRuleIsInvalid() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(-1, -1));

        assertThrows(ServerException.class, () -> marketingShippingService.calculateFreight(10000));
    }

    /** 一单多商品时运费取各商品候选值的最大者，不叠加。 */
    @Test
    void multiGoodsOrderChargesFreightOnlyOnce() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        List<MarketingShippingService.FreightLine> lines = List.of(
                new MarketingShippingService.FreightLine(101L, 5000),
                new MarketingShippingService.FreightLine(102L, 6000));

        assertEquals(1000, marketingShippingService.calculateFreight(lines));
    }

    /** 其中一个商品达到包邮门槛时，整单仍按另一个商品的运费收取。 */
    @Test
    void oneGoodsReachingThresholdDoesNotWaiveOthers() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        List<MarketingShippingService.FreightLine> lines = List.of(
                new MarketingShippingService.FreightLine(101L, 25000),
                new MarketingShippingService.FreightLine(102L, 6000));

        assertEquals(1000, marketingShippingService.calculateFreight(lines));
    }

    @Test
    void allGoodsReachingThresholdMeansFreeShipping() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 1000));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        List<MarketingShippingService.FreightLine> lines = List.of(
                new MarketingShippingService.FreightLine(101L, 25000),
                new MarketingShippingService.FreightLine(102L, 21000));

        assertEquals(0, marketingShippingService.calculateFreight(lines));
    }

    @Test
    void cartThresholdMatchesActiveRule() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(9900, 1000));

        assertEquals(9900, marketingShippingService.getFreeThreshold());
        assertEquals(0, marketingShippingService.calculateFreight(9900));
    }

    @Test
    void zeroBaseFeeDoesNotRequireMorePurchases() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(createRule(19900, 0));

        assertEquals(0, marketingShippingService.getFreeThreshold());
        assertEquals(0, marketingShippingService.calculateFreight(100));
    }

    @Test
    void cartThresholdFallsBackToZeroWhenNoRule() {
        when(shippingRuleMapper.selectOne(any())).thenReturn(null);

        assertEquals(0, marketingShippingService.getFreeThreshold());
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
