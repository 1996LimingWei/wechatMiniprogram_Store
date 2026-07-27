package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.dal.mysql.TradeCartMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeCartService {

    private final TradeCartMapper tradeCartMapper;
    private final TradeProductService tradeProductService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addCart(Long userId, Long goodsId, Long productId, int number) {
        return addCart(userId, goodsId, productId, number, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addBuyNow(Long userId, Long goodsId, Long productId, int number) {
        return addCart(userId, goodsId, productId, number, true);
    }

    private Map<String, Object> addCart(Long userId, Long goodsId, Long productId, int number, boolean buyNow) {
        if (number <= 0) {
            throw new ServerException(400, "商品数量必须大于 0");
        }
        TradeProductSnapshot snapshot = tradeProductService.getSnapshot(goodsId, productId);
        if (snapshot.getStock() < number) {
            throw new ServerException(1201, "商品库存不足");
        }

        if (buyNow) {
            tradeCartMapper.update(null, new LambdaUpdateWrapper<TradeCartDO>()
                    .eq(TradeCartDO::getUserId, userId)
                    .eq(TradeCartDO::getChecked, 1)
                    .set(TradeCartDO::getChecked, 0));
        }

        TradeCartDO cart = tradeCartMapper.selectOne(new LambdaQueryWrapper<TradeCartDO>()
                .eq(TradeCartDO::getUserId, userId)
                .eq(TradeCartDO::getSkuId, snapshot.getSkuId()));
        if (cart == null) {
            cart = new TradeCartDO();
            cart.setUserId(userId);
            cart.setSpuId(snapshot.getSpuId());
            cart.setSkuId(snapshot.getSkuId());
            cart.setGoodsName(snapshot.getName());
            cart.setGoodsPicUrl(snapshot.getPicUrl());
            cart.setSpecName(snapshot.getSpecName());
            cart.setPrice(snapshot.getPrice());
            cart.setCount(number);
            cart.setChecked(1);
            tradeCartMapper.insert(cart);
        } else {
            cart.setCount(buyNow ? number : cart.getCount() + number);
            cart.setChecked(1);
            tradeCartMapper.updateById(cart);
        }
        return Map.of("cartTotal", getCartTotal(userId));
    }

    public Map<String, Object> getCartIndex(Long userId) {
        List<TradeCartDO> list = getCartList(userId);
        return Map.of(
                "cartList", list.stream().map(this::toCartItem).toList(),
                "cartTotal", buildCartTotal(list)
        );
    }

    public Map<String, Object> getCartTotal(Long userId) {
        return buildCartTotal(getCartList(userId));
    }

    public List<TradeCartDO> getCheckedCartList(Long userId) {
        return tradeCartMapper.selectList(new LambdaQueryWrapper<TradeCartDO>()
                .eq(TradeCartDO::getUserId, userId)
                .eq(TradeCartDO::getChecked, 1)
                .orderByDesc(TradeCartDO::getUpdateTime));
    }

    public List<TradeCartDO> getCartList(Long userId) {
        return tradeCartMapper.selectList(new LambdaQueryWrapper<TradeCartDO>()
                .eq(TradeCartDO::getUserId, userId)
                .orderByDesc(TradeCartDO::getUpdateTime));
    }

    public void updateCart(Long userId, Long id, Long goodsId, Long productId, int number) {
        if (number <= 0) {
            throw new ServerException(400, "商品数量必须大于 0");
        }
        TradeCartDO cart = getCart(userId, id, goodsId, productId);
        cart.setCount(number);
        tradeCartMapper.updateById(cart);
    }

    public Map<String, Object> deleteCart(Long userId, String productIds) {
        List<Long> skuIds = parseIds(productIds);
        if (!skuIds.isEmpty()) {
            tradeCartMapper.delete(new LambdaQueryWrapper<TradeCartDO>()
                    .eq(TradeCartDO::getUserId, userId)
                    .in(TradeCartDO::getSkuId, skuIds));
        }
        return getCartIndex(userId);
    }

    public Map<String, Object> checkedCart(Long userId, String productIds, int isChecked) {
        List<Long> skuIds = parseIds(productIds);
        if (!skuIds.isEmpty()) {
            tradeCartMapper.update(null, new LambdaUpdateWrapper<TradeCartDO>()
                    .eq(TradeCartDO::getUserId, userId)
                    .in(TradeCartDO::getSkuId, skuIds)
                    .set(TradeCartDO::getChecked, isChecked == 1 ? 1 : 0));
        }
        return getCartIndex(userId);
    }

    public void clearCheckedCart(Long userId) {
        tradeCartMapper.delete(new LambdaQueryWrapper<TradeCartDO>()
                .eq(TradeCartDO::getUserId, userId)
                .eq(TradeCartDO::getChecked, 1));
    }

    public Map<String, Object> toCartItem(TradeCartDO cart) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", cart.getId());
        item.put("goodsId", cart.getSpuId());
        item.put("productId", cart.getSkuId());
        item.put("goodsName", cart.getGoodsName());
        item.put("goodsSpecifitionNameValue", cart.getSpecName());
        item.put("listPicUrl", cart.getGoodsPicUrl());
        item.put("retailPrice", TradeMoneyUtils.formatYuan(cart.getPrice()));
        item.put("number", cart.getCount());
        item.put("checked", cart.getChecked() != null && cart.getChecked() == 1);
        return item;
    }

    private TradeCartDO getCart(Long userId, Long id, Long goodsId, Long productId) {
        LambdaQueryWrapper<TradeCartDO> wrapper = new LambdaQueryWrapper<TradeCartDO>()
                .eq(TradeCartDO::getUserId, userId);
        if (id != null && id > 0) {
            wrapper.eq(TradeCartDO::getId, id);
        } else if (productId != null && productId > 0) {
            wrapper.eq(TradeCartDO::getSkuId, productId);
        } else {
            wrapper.eq(TradeCartDO::getSpuId, goodsId);
        }
        TradeCartDO cart = tradeCartMapper.selectOne(wrapper);
        if (cart == null) {
            throw new ServerException(1404, "购物车商品不存在");
        }
        return cart;
    }

    private Map<String, Object> buildCartTotal(List<TradeCartDO> list) {
        int goodsCount = 0;
        int goodsAmount = 0;
        int checkedGoodsCount = 0;
        int checkedGoodsAmount = 0;
        for (TradeCartDO cart : list) {
            int count = cart.getCount() == null ? 0 : cart.getCount();
            int amount = (cart.getPrice() == null ? 0 : cart.getPrice()) * count;
            goodsCount += count;
            goodsAmount += amount;
            if (cart.getChecked() != null && cart.getChecked() == 1) {
                checkedGoodsCount += count;
                checkedGoodsAmount += amount;
            }
        }
        Map<String, Object> total = new LinkedHashMap<>();
        total.put("goodsCount", goodsCount);
        total.put("goodsAmount", TradeMoneyUtils.formatYuan(goodsAmount));
        total.put("checkedGoodsCount", checkedGoodsCount);
        total.put("checkedGoodsAmount", TradeMoneyUtils.formatYuan(checkedGoodsAmount));
        return total;
    }

    private List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
