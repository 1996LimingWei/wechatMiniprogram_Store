package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.MarketingCouponDO;
import com.shop.module.trade.dal.dataobject.MarketingCouponTemplateDO;
import com.shop.module.trade.dal.dataobject.MarketingPromotionRuleDO;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeCheckoutServiceTest {

    @Mock
    private TradeCartService tradeCartService;
    @Mock
    private MemberAddressService memberAddressService;
    @Mock
    private TradeProductService tradeProductService;
    @Mock
    private MarketingCouponService marketingCouponService;
    @Mock
    private MarketingPromotionService marketingPromotionService;
    @Mock
    private MarketingShippingService marketingShippingService;
    @InjectMocks
    private TradeCheckoutService tradeCheckoutService;

    // ==================== calculateDiscount ====================

    @Test
    void shouldReturnZeroDiscountWhenNoCouponAndNoPromotion() {
        when(marketingPromotionService.findBestMatch(10000)).thenReturn(null);

        TradeCheckoutService.CheckoutDiscount result =
                tradeCheckoutService.calculateDiscount(1L, null, 10000);

        assertEquals(0, result.discountAmount());
        assertNull(result.couponId());
        assertNull(result.discountSource());
    }

    @Test
    void shouldUseCouponWhenBetterThanPromotion() {
        // 优惠券：满50减20
        MarketingCouponDO coupon = new MarketingCouponDO();
        coupon.setId(10L);
        coupon.setTemplateId(1L);
        coupon.setStatus(0);
        coupon.setExpireTime(LocalDateTime.now().plusDays(10));
        MarketingCouponTemplateDO template = new MarketingCouponTemplateDO();
        template.setId(1L);
        template.setDiscountAmount(2000);
        template.setThresholdAmount(5000);

        when(marketingCouponService.validateForCheckout(1L, 10L, 10000)).thenReturn(coupon);
        when(marketingCouponService.getTemplate(1L)).thenReturn(template);
        when(marketingPromotionService.findBestMatch(10000)).thenReturn(null);

        TradeCheckoutService.CheckoutDiscount result =
                tradeCheckoutService.calculateDiscount(1L, 10L, 10000);

        assertEquals(2000, result.discountAmount());
        assertEquals(10L, result.couponId());
        assertEquals("coupon", result.discountSource());
    }

    @Test
    void shouldUsePromotionWhenBetterThanCoupon() {
        // 优惠券：满50减5
        MarketingCouponDO coupon = new MarketingCouponDO();
        coupon.setId(10L);
        coupon.setTemplateId(1L);
        MarketingCouponTemplateDO template = new MarketingCouponTemplateDO();
        template.setId(1L);
        template.setDiscountAmount(500);
        template.setThresholdAmount(5000);

        // 满减：满100减30
        MarketingPromotionRuleDO rule = new MarketingPromotionRuleDO();
        rule.setId(1L);
        rule.setName("满100减30");
        rule.setThresholdAmount(10000);
        rule.setDiscountAmount(3000);

        when(marketingCouponService.validateForCheckout(1L, 10L, 15000)).thenReturn(coupon);
        when(marketingCouponService.getTemplate(1L)).thenReturn(template);
        when(marketingPromotionService.findBestMatch(15000)).thenReturn(rule);

        TradeCheckoutService.CheckoutDiscount result =
                tradeCheckoutService.calculateDiscount(1L, 10L, 15000);

        assertEquals(3000, result.discountAmount());
        assertNull(result.couponId()); // 使用满减时不锁定优惠券
        assertEquals("promotion", result.discountSource());
    }

    @Test
    void shouldPreferCouponWhenEqualDiscount() {
        // 优惠券和满减都是减1000
        MarketingCouponDO coupon = new MarketingCouponDO();
        coupon.setId(10L);
        coupon.setTemplateId(1L);
        MarketingCouponTemplateDO template = new MarketingCouponTemplateDO();
        template.setId(1L);
        template.setDiscountAmount(1000);

        MarketingPromotionRuleDO rule = new MarketingPromotionRuleDO();
        rule.setId(1L);
        rule.setDiscountAmount(1000);

        when(marketingCouponService.validateForCheckout(1L, 10L, 10000)).thenReturn(coupon);
        when(marketingCouponService.getTemplate(1L)).thenReturn(template);
        when(marketingPromotionService.findBestMatch(10000)).thenReturn(rule);

        TradeCheckoutService.CheckoutDiscount result =
                tradeCheckoutService.calculateDiscount(1L, 10L, 10000);

        // 相等时优先使用优惠券
        assertEquals(1000, result.discountAmount());
        assertEquals(10L, result.couponId());
        assertEquals("coupon", result.discountSource());
    }

    @Test
    void shouldUsePromotionWhenNoCouponSelected() {
        MarketingPromotionRuleDO rule = new MarketingPromotionRuleDO();
        rule.setId(1L);
        rule.setDiscountAmount(1000);
        when(marketingPromotionService.findBestMatch(10000)).thenReturn(rule);

        TradeCheckoutService.CheckoutDiscount result =
                tradeCheckoutService.calculateDiscount(1L, null, 10000);

        assertEquals(1000, result.discountAmount());
        assertNull(result.couponId());
        assertEquals("promotion", result.discountSource());
    }

    // ==================== checkout ====================

    @Test
    void shouldCalculateCheckoutWithCouponAndPromotion() {
        TradeCartDO cart = new TradeCartDO();
        cart.setId(1L);
        cart.setSpuId(100L);
        cart.setSkuId(200L);
        cart.setCount(2);
        cart.setChecked(1);

        TradeProductSnapshot snapshot = new TradeProductSnapshot();
        snapshot.setSpuId(100L);
        snapshot.setSkuId(200L);
        snapshot.setName("测试商品");
        snapshot.setPicUrl("https://example.com/img.png");
        snapshot.setSpecName("标准");
        snapshot.setPrice(5000);
        snapshot.setStock(10);

        MemberAddressDO address = new MemberAddressDO();
        address.setId(1L);
        address.setUserName("测试用户");
        address.setTelNumber("13800000000");
        address.setFullRegion("浙江省杭州市");
        address.setDetailInfo("测试地址");

        when(tradeCartService.getCheckedCartList(1L)).thenReturn(List.of(cart));
        when(tradeProductService.getSnapshot(100L, 200L)).thenReturn(snapshot);
        when(tradeCartService.toCartItem(any())).thenReturn(new HashMap<>(Map.of("id", 1L)));
        when(memberAddressService.getAddress(1L, 1L)).thenReturn(address);
        when(memberAddressService.toResp(any())).thenReturn(Map.of("id", 1L));
        when(marketingShippingService.calculateFreight(10000)).thenReturn(0); // 包邮

        // 优惠券：满50减20
        MarketingCouponDO coupon = new MarketingCouponDO();
        coupon.setId(10L);
        coupon.setTemplateId(1L);
        MarketingCouponTemplateDO template = new MarketingCouponTemplateDO();
        template.setId(1L);
        template.setName("满50减20");
        template.setType(1);
        template.setThresholdAmount(5000);
        template.setDiscountAmount(2000);

        when(marketingCouponService.validateForCheckout(1L, 10L, 10000)).thenReturn(coupon);
        when(marketingCouponService.getTemplate(1L)).thenReturn(template);
        when(marketingCouponService.getAvailableCoupons(1L, 10000)).thenReturn(List.of(coupon));
        when(marketingPromotionService.findBestMatch(10000)).thenReturn(null);

        Map<String, Object> result = tradeCheckoutService.checkout(1L, 1L, 10L);

        assertEquals("20.00", result.get("couponPrice")); // 优惠 20 元
        assertEquals("80.00", result.get("actualPrice"));   // 100 - 20 = 80
        assertEquals("coupon", result.get("discountSource"));
        assertEquals(10L, result.get("selectedCouponId"));
    }
}
