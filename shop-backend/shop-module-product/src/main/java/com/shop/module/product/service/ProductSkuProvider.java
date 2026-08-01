package com.shop.module.product.service;

/**
 * 商品与库存的可替换契约。Mock、数据库和第三方库存实现均须遵守此接口。
 */
public interface ProductSkuProvider {

    ProductSkuSnapshot getSnapshot(Long spuId, Long requestedSkuId);

    void reduceStock(Long skuId, int count);

    void recoverStock(Long skuId, int count);
}
