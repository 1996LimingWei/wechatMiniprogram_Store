package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.controller.MockData;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeProductService {

    private final ProductSpuMapper productSpuMapper;

    public TradeProductSnapshot getSnapshot(Long goodsId, Long productId) {
        Long cartSkuId = buildCartSkuId(goodsId, productId);
        ProductSpuDO spu = goodsId == null ? null : productSpuMapper.selectById(goodsId);
        if (spu != null) {
            if (spu.getStatus() != null && spu.getStatus() == 0) {
                throw new ServerException(1102, "商品已下架");
            }
            TradeProductSnapshot snapshot = new TradeProductSnapshot();
            snapshot.setSpuId(spu.getId());
            snapshot.setSkuId(cartSkuId);
            snapshot.setName(spu.getName());
            snapshot.setPicUrl(spu.getPicUrl());
            snapshot.setSpecName("默认规格");
            snapshot.setPrice(spu.getPrice());
            snapshot.setStock(spu.getStock() == null ? 0 : spu.getStock());
            snapshot.setDatabaseProduct(true);
            return snapshot;
        }

        Map<String, Object> goods = goodsId == null ? null : MockData.getGoodsById(goodsId);
        if (goods == null || goods.isEmpty()) {
            throw new ServerException(1101, "商品不存在");
        }
        TradeProductSnapshot snapshot = new TradeProductSnapshot();
        snapshot.setSpuId(goodsId);
        snapshot.setSkuId(cartSkuId);
        snapshot.setName(String.valueOf(goods.get("name")));
        snapshot.setPicUrl(String.valueOf(goods.get("listPicUrl")));
        snapshot.setSpecName("默认规格");
        snapshot.setPrice(TradeMoneyUtils.parseCent(goods.get("retailPrice")));
        snapshot.setStock(999);
        snapshot.setDatabaseProduct(false);
        return snapshot;
    }

    public void reduceStock(TradeProductSnapshot snapshot, int count) {
        if (!snapshot.isDatabaseProduct()) {
            return;
        }
        int updated = productSpuMapper.update(null, new LambdaUpdateWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getId, snapshot.getSpuId())
                .eq(ProductSpuDO::getStatus, 1)
                .ge(ProductSpuDO::getStock, count)
                .setSql("stock = stock - " + count));
        if (updated != 1) {
            throw new ServerException(1201, "商品库存不足");
        }
    }

    public void recoverStock(Long spuId, int count) {
        if (spuId == null || count <= 0) {
            return;
        }
        productSpuMapper.update(null, new LambdaUpdateWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getId, spuId)
                .setSql("stock = stock + " + count));
    }

    private Long buildCartSkuId(Long goodsId, Long productId) {
        long goodsPart = goodsId == null ? 0L : goodsId;
        long specPart = productId == null || productId <= 0 ? 0L : productId;
        return goodsPart * 1000000000L + specPart;
    }
}
