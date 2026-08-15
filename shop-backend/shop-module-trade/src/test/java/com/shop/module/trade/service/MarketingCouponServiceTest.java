package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MarketingCouponDO;
import com.shop.module.trade.dal.dataobject.MarketingCouponTemplateDO;
import com.shop.module.trade.dal.mysql.MarketingCouponMapper;
import com.shop.module.trade.dal.mysql.MarketingCouponTemplateMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingCouponServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(MarketingCouponDO.class, MarketingCouponTemplateDO.class);
    }

    @Mock
    private MarketingCouponTemplateMapper templateMapper;
    @Mock
    private MarketingCouponMapper couponMapper;
    @InjectMocks
    private MarketingCouponService marketingCouponService;

    // ==================== claimCoupon ====================

    @Test
    void shouldClaimCouponSuccessfully() {
        MarketingCouponTemplateDO template = createTemplate(1L, 10000, 5000, 100, 0, 1);
        template.setValidityType(2);
        template.setValidDays(30);
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(couponMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.insert(any())).thenReturn(1);
        when(templateMapper.update(any(), any())).thenReturn(1);

        MarketingCouponDO coupon = marketingCouponService.claimCoupon(1L, 1L);

        assertNotNull(coupon);
        assertEquals(1L, coupon.getUserId());
        assertEquals(1L, coupon.getTemplateId());
        assertEquals(0, coupon.getStatus());
        verify(couponMapper).insert(any(MarketingCouponDO.class));
        verify(templateMapper).update(isNull(), any());
    }

    @Test
    void shouldRejectClaimWhenTemplateDisabled() {
        MarketingCouponTemplateDO template = createTemplate(1L, 10000, 5000, 100, 0, 0);
        when(templateMapper.selectById(1L)).thenReturn(template);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.claimCoupon(1L, 1L));
        assertEquals("优惠券不存在或已下架", ex.getMessage());
    }

    @Test
    void shouldRejectClaimWhenStockExhausted() {
        MarketingCouponTemplateDO template = createTemplate(1L, 10000, 5000, 10, 10, 1);
        when(templateMapper.selectById(1L)).thenReturn(template);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.claimCoupon(1L, 1L));
        assertEquals("优惠券已领完", ex.getMessage());
    }

    @Test
    void shouldRejectClaimWhenPerUserLimitReached() {
        MarketingCouponTemplateDO template = createTemplate(1L, 10000, 5000, 100, 10, 1);
        template.setPerUserLimit(1);
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(couponMapper.selectCount(any())).thenReturn(1L);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.claimCoupon(1L, 1L));
        assertEquals("已达到领取上限", ex.getMessage());
    }

    @Test
    void shouldRejectClaimWhenConcurrentStockDepleted() {
        MarketingCouponTemplateDO template = createTemplate(1L, 10000, 5000, 100, 50, 1);
        template.setValidityType(2);
        template.setValidDays(30);
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(couponMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.insert(any())).thenReturn(1);
        // 并发场景：另一个线程先领完了最后一张
        when(templateMapper.update(any(), any())).thenReturn(0);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.claimCoupon(1L, 1L));
        assertEquals("优惠券已领完", ex.getMessage());
    }

    @Test
    void shouldCalculateExpireTimeByFixedEndDate() {
        MarketingCouponTemplateDO template = createTemplate(1L, 0, 500, 0, 0, 1);
        template.setValidityType(1);
        LocalDateTime endTime = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
        template.setValidEndTime(endTime);
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(couponMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.insert(any())).thenReturn(1);
        when(templateMapper.update(any(), any())).thenReturn(1);

        marketingCouponService.claimCoupon(1L, 1L);

        ArgumentCaptor<MarketingCouponDO> captor = ArgumentCaptor.forClass(MarketingCouponDO.class);
        verify(couponMapper).insert(captor.capture());
        assertEquals(endTime, captor.getValue().getExpireTime());
    }

    @Test
    void shouldCalculateExpireTimeByValidDays() {
        MarketingCouponTemplateDO template = createTemplate(1L, 0, 500, 0, 0, 1);
        template.setValidityType(2);
        template.setValidDays(7);
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(couponMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.insert(any())).thenReturn(1);
        when(templateMapper.update(any(), any())).thenReturn(1);

        LocalDateTime before = LocalDateTime.now().plusDays(7).withNano(0);
        marketingCouponService.claimCoupon(1L, 1L);

        ArgumentCaptor<MarketingCouponDO> captor = ArgumentCaptor.forClass(MarketingCouponDO.class);
        verify(couponMapper).insert(captor.capture());
        LocalDateTime expireTime = captor.getValue().getExpireTime();
        assertNotNull(expireTime);
        // 允许1秒误差
        assertTrue(expireTime.isAfter(before.minusSeconds(2)));
        assertTrue(expireTime.isBefore(before.plusSeconds(2)));
    }

    // ==================== getAvailableCoupons ====================

    @Test
    void shouldReturnAvailableCouponsFilteredByThreshold() {
        MarketingCouponDO coupon1 = createCoupon(1L, 1L, 0, LocalDateTime.now().plusDays(10));
        MarketingCouponDO coupon2 = createCoupon(2L, 2L, 0, LocalDateTime.now().plusDays(10));
        when(couponMapper.selectList(any())).thenReturn(List.of(coupon1, coupon2));

        MarketingCouponTemplateDO tpl1 = createTemplate(1L, 5000, 1000, 0, 0, 1);
        MarketingCouponTemplateDO tpl2 = createTemplate(2L, 20000, 5000, 0, 0, 1);
        when(templateMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(tpl1, tpl2));

        // 商品总额 10000 分，满足 tpl1（5000门槛）不满足 tpl2（20000门槛）
        List<MarketingCouponDO> result = marketingCouponService.getAvailableCoupons(1L, 10000);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void shouldReturnEmptyWhenNoCoupons() {
        when(couponMapper.selectList(any())).thenReturn(List.of());

        List<MarketingCouponDO> result = marketingCouponService.getAvailableCoupons(1L, 10000);

        assertTrue(result.isEmpty());
    }

    // ==================== validateForCheckout ====================

    @Test
    void shouldValidateCouponForCheckout() {
        MarketingCouponDO coupon = createCoupon(1L, 1L, 0, LocalDateTime.now().plusDays(10));
        coupon.setUserId(1L);
        when(couponMapper.selectById(1L)).thenReturn(coupon);
        MarketingCouponTemplateDO template = createTemplate(1L, 5000, 1000, 0, 0, 1);
        when(templateMapper.selectById(1L)).thenReturn(template);

        MarketingCouponDO result = marketingCouponService.validateForCheckout(1L, 1L, 10000);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void shouldRejectExpiredCouponForCheckout() {
        MarketingCouponDO coupon = createCoupon(1L, 1L, 0, LocalDateTime.now().minusHours(1));
        coupon.setUserId(1L);
        when(couponMapper.selectById(1L)).thenReturn(coupon);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.validateForCheckout(1L, 1L, 10000));
        assertEquals("优惠券已过期", ex.getMessage());
    }

    @Test
    void shouldRejectCouponBelowThreshold() {
        MarketingCouponDO coupon = createCoupon(1L, 1L, 0, LocalDateTime.now().plusDays(10));
        coupon.setUserId(1L);
        when(couponMapper.selectById(1L)).thenReturn(coupon);
        MarketingCouponTemplateDO template = createTemplate(1L, 20000, 5000, 0, 0, 1);
        when(templateMapper.selectById(1L)).thenReturn(template);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.validateForCheckout(1L, 1L, 10000));
        assertEquals("未达到优惠券使用门槛", ex.getMessage());
    }

    // ==================== lockCoupon ====================

    @Test
    void shouldLockCouponSuccessfully() {
        when(couponMapper.update(isNull(), any())).thenReturn(1);

        assertDoesNotThrow(() -> marketingCouponService.lockCoupon(1L, 1L, 99L));
        verify(couponMapper).update(isNull(), any());
    }

    @Test
    void shouldThrowWhenLockFails() {
        when(couponMapper.update(isNull(), any())).thenReturn(0);

        ServerException ex = assertThrows(ServerException.class,
                () -> marketingCouponService.lockCoupon(1L, 1L, 99L));
        assertEquals("优惠券不可用或已过期", ex.getMessage());
    }

    // ==================== releaseCoupon ====================

    @Test
    void shouldReleaseCouponOnOrderClose() {
        when(couponMapper.update(isNull(), any())).thenReturn(1);

        marketingCouponService.releaseCoupon(99L);

        verify(couponMapper).update(isNull(), any());
    }

    // ==================== expireUnusedCoupons ====================

    @Test
    void shouldExpireUnusedCoupons() {
        MarketingCouponDO expired1 = createCoupon(1L, 1L, 0, LocalDateTime.now().minusHours(1));
        expired1.setId(10L);
        MarketingCouponDO expired2 = createCoupon(2L, 1L, 0, LocalDateTime.now().minusHours(2));
        expired2.setId(20L);
        when(couponMapper.selectList(any())).thenReturn(List.of(expired1, expired2));
        when(couponMapper.update(isNull(), any())).thenReturn(1);

        int count = marketingCouponService.expireUnusedCoupons(100);

        assertEquals(2, count);
        verify(couponMapper, times(2)).update(isNull(), any());
    }

    @Test
    void shouldReturnZeroWhenNoExpiredCoupons() {
        when(couponMapper.selectList(any())).thenReturn(List.of());

        int count = marketingCouponService.expireUnusedCoupons(100);

        assertEquals(0, count);
        verify(couponMapper, never()).update(isNull(), any());
    }

    // ==================== 辅助方法 ====================

    private MarketingCouponTemplateDO createTemplate(Long id, int threshold, int discount,
                                                      int totalCount, int claimedCount, int status) {
        MarketingCouponTemplateDO tpl = new MarketingCouponTemplateDO();
        tpl.setId(id);
        tpl.setName("测试优惠券");
        tpl.setType(1);
        tpl.setThresholdAmount(threshold);
        tpl.setDiscountAmount(discount);
        tpl.setTotalCount(totalCount);
        tpl.setClaimedCount(claimedCount);
        tpl.setPerUserLimit(1);
        tpl.setValidityType(1);
        tpl.setStatus(status);
        return tpl;
    }

    private MarketingCouponDO createCoupon(Long id, Long templateId, int status, LocalDateTime expireTime) {
        MarketingCouponDO coupon = new MarketingCouponDO();
        coupon.setId(id);
        coupon.setUserId(1L);
        coupon.setTemplateId(templateId);
        coupon.setStatus(status);
        coupon.setExpireTime(expireTime);
        return coupon;
    }
}
