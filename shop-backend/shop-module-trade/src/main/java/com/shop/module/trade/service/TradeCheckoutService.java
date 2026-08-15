package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.MarketingCouponDO;
import com.shop.module.trade.dal.dataobject.MarketingCouponTemplateDO;
import com.shop.module.trade.dal.dataobject.MarketingPromotionRuleDO;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeCheckoutService {

    private final TradeCartService tradeCartService;
    private final MemberAddressService memberAddressService;
    private final TradeProductService tradeProductService;
    private final MarketingCouponService marketingCouponService;
    private final MarketingPromotionService marketingPromotionService;
    private final MarketingShippingService marketingShippingService;

    public Map<String, Object> checkout(Long userId, Long addressId, Long couponId) {
        List<TradeCartDO> checkedList = tradeCartService.getCheckedCartList(userId);
        if (checkedList.isEmpty()) {
            throw new com.shop.common.exception.ServerException(400, "请选择要结算的商品");
        }
        List<TradeProductSnapshot> snapshots = checkedList.stream()
                .map(item -> tradeProductService.getSnapshot(item.getSpuId(), item.getSkuId()))
                .toList();
        int goodsTotalPrice = 0;
        for (int index = 0; index < checkedList.size(); index++) {
            TradeCartDO item = checkedList.get(index);
            TradeProductSnapshot snapshot = snapshots.get(index);
            if (item.getCount() == null || item.getCount() < 1 || item.getCount() > 99) {
                throw new com.shop.common.exception.ServerException(400, "商品数量必须在 1 到 99 之间");
            }
            if (snapshot.getStock() == null || snapshot.getStock() < item.getCount()) {
                throw new com.shop.common.exception.ServerException(1201, "商品库存不足");
            }
            item.setGoodsName(snapshot.getName());
            item.setGoodsPicUrl(snapshot.getPicUrl());
            item.setSpecName(snapshot.getSpecName());
            item.setPrice(snapshot.getPrice());
            goodsTotalPrice = Math.addExact(goodsTotalPrice, Math.multiplyExact(snapshot.getPrice(), item.getCount()));
        }

        int freightPrice = marketingShippingService.calculateFreight(goodsTotalPrice);

        int couponDiscount = 0;
        int promotionDiscount = 0;
        Long selectedCouponId = null;
        String discountSource = null;
        MarketingPromotionRuleDO matchedRule = null;

        if (couponId != null && couponId > 0) {
            MarketingCouponDO coupon = marketingCouponService.validateForCheckout(userId, couponId, goodsTotalPrice);
            MarketingCouponTemplateDO template = marketingCouponService.getTemplate(coupon.getTemplateId());
            if (template != null) {
                couponDiscount = template.getDiscountAmount();
            }
            selectedCouponId = couponId;
        }

        matchedRule = marketingPromotionService.findBestMatch(goodsTotalPrice);
        if (matchedRule != null) {
            promotionDiscount = matchedRule.getDiscountAmount();
        }

        int bestDiscount;
        if (couponDiscount >= promotionDiscount && couponDiscount > 0) {
            bestDiscount = couponDiscount;
            discountSource = "coupon";
        } else if (promotionDiscount > 0) {
            bestDiscount = promotionDiscount;
            discountSource = "promotion";
            selectedCouponId = null;
        } else {
            bestDiscount = 0;
            discountSource = null;
        }

        int couponPrice = bestDiscount;
        int orderTotalPrice = Math.addExact(goodsTotalPrice, freightPrice);
        int actualPrice = Math.max(0, Math.subtractExact(orderTotalPrice, couponPrice));
        MemberAddressDO address = memberAddressService.getAddress(userId, addressId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedGoodsList", java.util.stream.IntStream.range(0, checkedList.size()).mapToObj(index -> {
            Map<String, Object> item = tradeCartService.toCartItem(checkedList.get(index));
            item.put("productId", snapshots.get(index).getSkuId());
            item.put("retailPrice", TradeMoneyUtils.formatYuan(snapshots.get(index).getPrice()));
            return item;
        }).toList());
        data.put("checkedAddress", memberAddressService.toResp(address));
        data.put("actualPrice", TradeMoneyUtils.formatYuan(actualPrice));
        data.put("couponPrice", TradeMoneyUtils.formatYuan(couponPrice));
        data.put("freightPrice", TradeMoneyUtils.formatYuan(freightPrice));
        data.put("goodsTotalPrice", TradeMoneyUtils.formatYuan(goodsTotalPrice));
        data.put("orderTotalPrice", TradeMoneyUtils.formatYuan(orderTotalPrice));
        data.put("actualPriceCent", actualPrice);
        data.put("goodsTotalPriceCent", goodsTotalPrice);
        data.put("freightPriceCent", freightPrice);
        data.put("couponPriceCent", couponPrice);
        data.put("discountSource", discountSource);
        data.put("selectedCouponId", selectedCouponId);

        List<MarketingCouponDO> availableCoupons = marketingCouponService.getAvailableCoupons(userId, goodsTotalPrice);
        final Long finalSelectedCouponId = selectedCouponId;
        data.put("couponList", availableCoupons.stream().map(coupon -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", coupon.getId());
            item.put("templateId", coupon.getTemplateId());
            MarketingCouponTemplateDO tpl = marketingCouponService.getTemplate(coupon.getTemplateId());
            if (tpl != null) {
                item.put("name", tpl.getName());
                item.put("type", tpl.getType());
                item.put("thresholdAmount", TradeMoneyUtils.formatYuan(tpl.getThresholdAmount()));
                item.put("discountAmount", TradeMoneyUtils.formatYuan(tpl.getDiscountAmount()));
            }
            item.put("expireTime", coupon.getExpireTime());
            item.put("selected", coupon.getId().equals(finalSelectedCouponId));
            return item;
        }).toList());

        if (matchedRule != null) {
            Map<String, Object> promo = new LinkedHashMap<>();
            promo.put("name", matchedRule.getName());
            promo.put("thresholdAmount", TradeMoneyUtils.formatYuan(matchedRule.getThresholdAmount()));
            promo.put("discountAmount", TradeMoneyUtils.formatYuan(matchedRule.getDiscountAmount()));
            data.put("promotion", promo);
            int gap = matchedRule.getThresholdAmount() - goodsTotalPrice;
            if (gap > 0) {
                data.put("promotionGap", TradeMoneyUtils.formatYuan(gap));
            }
        } else {
            data.put("promotion", null);
        }

        return data;
    }

    public int calculateFreight(int goodsTotalPrice) {
        return marketingShippingService.calculateFreight(goodsTotalPrice);
    }

    /**
     * 计算结算优惠（供 submitOrder 复用）
     */
    public CheckoutDiscount calculateDiscount(Long userId, Long couponId, int goodsTotalPrice) {
        int couponDiscount = 0;
        Long selectedCouponId = null;
        String discountSource = null;

        if (couponId != null && couponId > 0) {
            MarketingCouponDO coupon = marketingCouponService.validateForCheckout(userId, couponId, goodsTotalPrice);
            MarketingCouponTemplateDO template = marketingCouponService.getTemplate(coupon.getTemplateId());
            if (template != null) {
                couponDiscount = template.getDiscountAmount();
            }
            selectedCouponId = couponId;
        }

        MarketingPromotionRuleDO matchedRule = marketingPromotionService.findBestMatch(goodsTotalPrice);
        int promotionDiscount = matchedRule != null ? matchedRule.getDiscountAmount() : 0;

        int bestDiscount;
        if (couponDiscount >= promotionDiscount && couponDiscount > 0) {
            bestDiscount = couponDiscount;
            discountSource = "coupon";
        } else if (promotionDiscount > 0) {
            bestDiscount = promotionDiscount;
            discountSource = "promotion";
            selectedCouponId = null;
        } else {
            bestDiscount = 0;
            discountSource = null;
        }

        return new CheckoutDiscount(bestDiscount, selectedCouponId, discountSource);
    }

    public record CheckoutDiscount(int discountAmount, Long couponId, String discountSource) {
    }
}
