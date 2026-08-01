package com.shop.module.trade.service;

import com.shop.module.product.service.ProductSkuProvider;
import com.shop.module.product.service.ProductSkuSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeProductService {

    private final ProductSkuProvider productSkuProvider;

    public TradeProductSnapshot getSnapshot(Long goodsId, Long productId) {
        ProductSkuSnapshot product = productSkuProvider.getSnapshot(goodsId, productId);
        TradeProductSnapshot snapshot = new TradeProductSnapshot();
        snapshot.setSpuId(product.getSpuId());
        snapshot.setSkuId(product.getSkuId());
        snapshot.setName(product.getName());
        snapshot.setPicUrl(product.getPicUrl());
        snapshot.setSpecName(product.getSpecName());
        snapshot.setPrice(product.getPrice());
        snapshot.setStock(product.getStock());
        return snapshot;
    }

    public void reduceStock(TradeProductSnapshot snapshot, int count) {
        productSkuProvider.reduceStock(snapshot.getSkuId(), count);
    }

    public void recoverStock(Long skuId, int count) {
        productSkuProvider.recoverStock(skuId, count);
    }
}
