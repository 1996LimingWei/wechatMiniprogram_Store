package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeCheckoutService {

    @Value("${trade.freight.free-threshold:19900}")
    private int freeFreightAmount = 19900;

    @Value("${trade.freight.base-fee:1000}")
    private int defaultFreight = 1000;

    private final TradeCartService tradeCartService;
    private final MemberAddressService memberAddressService;
    private final TradeProductService tradeProductService;

    public Map<String, Object> checkout(Long userId, Long addressId) {
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
        int freightPrice = calculateFreight(goodsTotalPrice);
        int couponPrice = 0;
        int orderTotalPrice = Math.addExact(goodsTotalPrice, freightPrice);
        int actualPrice = Math.subtractExact(orderTotalPrice, couponPrice);
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
        data.put("checkedCoupon", null);
        data.put("couponList", List.of());
        data.put("couponPrice", TradeMoneyUtils.formatYuan(couponPrice));
        data.put("freightPrice", TradeMoneyUtils.formatYuan(freightPrice));
        data.put("goodsTotalPrice", TradeMoneyUtils.formatYuan(goodsTotalPrice));
        data.put("orderTotalPrice", TradeMoneyUtils.formatYuan(orderTotalPrice));
        data.put("actualPriceCent", actualPrice);
        data.put("goodsTotalPriceCent", goodsTotalPrice);
        data.put("freightPriceCent", freightPrice);
        return data;
    }

    public int calculateFreight(int goodsTotalPrice) {
        if (freeFreightAmount < 0 || defaultFreight < 0) {
            throw new com.shop.common.exception.ServerException(500, "运费配置不正确");
        }
        return goodsTotalPrice > 0 && goodsTotalPrice < freeFreightAmount ? defaultFreight : 0;
    }
}
