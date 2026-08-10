package com.shop.module.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 数据库 SKU 实现，切换 `product.provider=database` 后生效。 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "product", name = "provider", havingValue = "database")
public class DatabaseProductSkuProvider implements ProductSkuProvider {

    private final ProductInventoryService productInventoryService;

    @Override
    public ProductSkuSnapshot getSnapshot(Long spuId, Long requestedSkuId) {
        ProductInventoryService.ProductSnapshot sku =
                productInventoryService.getAvailableSnapshot(spuId, requestedSkuId);
        ProductSkuSnapshot snapshot = new ProductSkuSnapshot();
        snapshot.setSpuId(sku.spuId());
        snapshot.setSkuId(sku.skuId());
        snapshot.setName(sku.name());
        snapshot.setPicUrl(sku.picUrl());
        snapshot.setSpecName(sku.specName());
        snapshot.setPrice(sku.price());
        snapshot.setStock(sku.stock());
        return snapshot;
    }

    @Override
    public void reduceStock(Long skuId, int count) {
        productInventoryService.reduceSkuStock(skuId, count);
    }

    @Override
    public void recoverStock(Long skuId, int count) {
        productInventoryService.recoverSkuStock(skuId, count);
    }

    @Override
    public void adjustSales(Long spuId, int delta) {
        productInventoryService.adjustSpuSales(spuId, delta);
    }
}
