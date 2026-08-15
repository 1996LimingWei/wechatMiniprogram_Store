package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MarketingCouponDO;
import com.shop.module.trade.dal.dataobject.MarketingCouponTemplateDO;
import com.shop.module.trade.dal.mysql.MarketingCouponMapper;
import com.shop.module.trade.dal.mysql.MarketingCouponTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingCouponService {

    private static final int STATUS_UNUSED = 0;
    private static final int STATUS_USED = 1;
    private static final int STATUS_EXPIRED = 2;

    private final MarketingCouponTemplateMapper templateMapper;
    private final MarketingCouponMapper couponMapper;

    /**
     * 领取优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public MarketingCouponDO claimCoupon(Long userId, Long templateId) {
        MarketingCouponTemplateDO template = templateMapper.selectById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new ServerException(400, "优惠券不存在或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getValidityType() == 1) {
            if (template.getValidStartTime() != null && now.isBefore(template.getValidStartTime())) {
                throw new ServerException(400, "优惠券尚未开始领取");
            }
            if (template.getValidEndTime() != null && now.isAfter(template.getValidEndTime())) {
                throw new ServerException(400, "优惠券领取已结束");
            }
        }
        if (template.getTotalCount() != null && template.getTotalCount() > 0) {
            if (template.getClaimedCount() >= template.getTotalCount()) {
                throw new ServerException(400, "优惠券已领完");
            }
        }
        Long count = couponMapper.selectCount(new LambdaQueryWrapper<MarketingCouponDO>()
                .eq(MarketingCouponDO::getUserId, userId)
                .eq(MarketingCouponDO::getTemplateId, templateId));
        if (count >= (template.getPerUserLimit() == null ? 1 : template.getPerUserLimit())) {
            throw new ServerException(400, "已达到领取上限");
        }
        LocalDateTime expireTime = calculateExpireTime(template, now);

        MarketingCouponDO coupon = new MarketingCouponDO();
        coupon.setUserId(userId);
        coupon.setTemplateId(templateId);
        coupon.setStatus(STATUS_UNUSED);
        coupon.setExpireTime(expireTime);
        couponMapper.insert(coupon);

        int updated = templateMapper.update(null, new LambdaUpdateWrapper<MarketingCouponTemplateDO>()
                .eq(MarketingCouponTemplateDO::getId, templateId)
                .eq(MarketingCouponTemplateDO::getStatus, 1)
                .apply(template.getTotalCount() != null && template.getTotalCount() > 0,
                        "claimed_count < total_count")
                .setSql("claimed_count = claimed_count + 1"));
        if (updated != 1) {
            throw new ServerException(400, "优惠券已领完");
        }
        return coupon;
    }

    /**
     * 查询用户可用优惠券（满足金额门槛）
     */
    public List<MarketingCouponDO> getAvailableCoupons(Long userId, int goodsTotalPrice) {
        LocalDateTime now = LocalDateTime.now();
        List<MarketingCouponDO> coupons = couponMapper.selectList(new LambdaQueryWrapper<MarketingCouponDO>()
                .eq(MarketingCouponDO::getUserId, userId)
                .eq(MarketingCouponDO::getStatus, STATUS_UNUSED)
                .gt(MarketingCouponDO::getExpireTime, now)
                .orderByDesc(MarketingCouponDO::getExpireTime));

        List<Long> templateIds = coupons.stream().map(MarketingCouponDO::getTemplateId).distinct().toList();
        if (templateIds.isEmpty()) {
            return List.of();
        }
        List<MarketingCouponTemplateDO> templates = templateMapper.selectBatchIds(templateIds);
        return coupons.stream()
                .filter(coupon -> {
                    MarketingCouponTemplateDO tpl = templates.stream()
                            .filter(t -> t.getId().equals(coupon.getTemplateId())).findFirst().orElse(null);
                    if (tpl == null || tpl.getStatus() != 1) return false;
                    int threshold = tpl.getThresholdAmount() == null ? 0 : tpl.getThresholdAmount();
                    return goodsTotalPrice >= threshold;
                })
                .toList();
    }

    /**
     * 验证优惠券可用于结算
     */
    public MarketingCouponDO validateForCheckout(Long userId, Long couponId, int goodsTotalPrice) {
        MarketingCouponDO coupon = couponMapper.selectById(couponId);
        if (coupon == null || !coupon.getUserId().equals(userId)) {
            throw new ServerException(400, "优惠券不存在");
        }
        if (coupon.getStatus() != STATUS_UNUSED) {
            throw new ServerException(400, "优惠券不可用");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServerException(400, "优惠券已过期");
        }
        MarketingCouponTemplateDO template = templateMapper.selectById(coupon.getTemplateId());
        if (template == null || template.getStatus() != 1) {
            throw new ServerException(400, "优惠券已失效");
        }
        int threshold = template.getThresholdAmount() == null ? 0 : template.getThresholdAmount();
        if (goodsTotalPrice < threshold) {
            throw new ServerException(400, "未达到优惠券使用门槛");
        }
        return coupon;
    }

    /**
     * 核销优惠券（下单时调用，必须在事务内）
     */
    public void lockCoupon(Long userId, Long couponId, Long orderId) {
        int updated = couponMapper.update(null, new LambdaUpdateWrapper<MarketingCouponDO>()
                .eq(MarketingCouponDO::getId, couponId)
                .eq(MarketingCouponDO::getUserId, userId)
                .eq(MarketingCouponDO::getStatus, STATUS_UNUSED)
                .gt(MarketingCouponDO::getExpireTime, LocalDateTime.now())
                .set(MarketingCouponDO::getStatus, STATUS_USED)
                .set(MarketingCouponDO::getOrderId, orderId)
                .set(MarketingCouponDO::getUsedTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new ServerException(400, "优惠券不可用或已过期");
        }
    }

    /**
     * 释放优惠券（订单关闭时调用）
     */
    public void releaseCoupon(Long orderId) {
        couponMapper.update(null, new LambdaUpdateWrapper<MarketingCouponDO>()
                .eq(MarketingCouponDO::getOrderId, orderId)
                .eq(MarketingCouponDO::getStatus, STATUS_USED)
                .set(MarketingCouponDO::getStatus, STATUS_UNUSED)
                .set(MarketingCouponDO::getOrderId, null)
                .set(MarketingCouponDO::getUsedTime, null));
    }

    /**
     * 查询用户优惠券列表
     */
    public List<MarketingCouponDO> getUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<MarketingCouponDO> wrapper = new LambdaQueryWrapper<MarketingCouponDO>()
                .eq(MarketingCouponDO::getUserId, userId)
                .orderByDesc(MarketingCouponDO::getCreateTime);
        if (status != null) {
            wrapper.eq(MarketingCouponDO::getStatus, status);
        }
        return couponMapper.selectList(wrapper);
    }

    /**
     * 查询可领取的优惠券模板
     */
    public List<MarketingCouponTemplateDO> getClaimableTemplates(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<MarketingCouponTemplateDO> templates = templateMapper.selectList(
                new LambdaQueryWrapper<MarketingCouponTemplateDO>()
                        .eq(MarketingCouponTemplateDO::getStatus, 1)
                        .orderByDesc(MarketingCouponTemplateDO::getId));
        return templates.stream().filter(tpl -> {
            if (tpl.getValidityType() != null && tpl.getValidityType() == 1) {
                if (tpl.getValidEndTime() != null && now.isAfter(tpl.getValidEndTime())) return false;
                if (tpl.getValidStartTime() != null && now.isBefore(tpl.getValidStartTime())) return false;
            }
            if (tpl.getTotalCount() != null && tpl.getTotalCount() > 0
                    && tpl.getClaimedCount() >= tpl.getTotalCount()) {
                return false;
            }
            Long claimedByUser = couponMapper.selectCount(new LambdaQueryWrapper<MarketingCouponDO>()
                    .eq(MarketingCouponDO::getUserId, userId)
                    .eq(MarketingCouponDO::getTemplateId, tpl.getId()));
            int limit = tpl.getPerUserLimit() == null ? 1 : tpl.getPerUserLimit();
            return claimedByUser < limit;
        }).toList();
    }

    /**
     * 获取优惠券关联的模板
     */
    public MarketingCouponTemplateDO getTemplate(Long templateId) {
        return templateMapper.selectById(templateId);
    }

    /**
     * 过期优惠券清理
     */
    public int expireUnusedCoupons(int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        List<MarketingCouponDO> expired = couponMapper.selectList(new LambdaQueryWrapper<MarketingCouponDO>()
                .eq(MarketingCouponDO::getStatus, STATUS_UNUSED)
                .le(MarketingCouponDO::getExpireTime, now)
                .last("LIMIT " + Math.max(1, batchSize)));
        for (MarketingCouponDO coupon : expired) {
            couponMapper.update(null, new LambdaUpdateWrapper<MarketingCouponDO>()
                    .eq(MarketingCouponDO::getId, coupon.getId())
                    .eq(MarketingCouponDO::getStatus, STATUS_UNUSED)
                    .set(MarketingCouponDO::getStatus, STATUS_EXPIRED));
        }
        return expired.size();
    }

    private LocalDateTime calculateExpireTime(MarketingCouponTemplateDO template, LocalDateTime claimTime) {
        if (template.getValidityType() != null && template.getValidityType() == 2
                && template.getValidDays() != null) {
            return claimTime.plusDays(template.getValidDays());
        }
        if (template.getValidEndTime() != null) {
            return template.getValidEndTime();
        }
        return claimTime.plusDays(30);
    }
}
