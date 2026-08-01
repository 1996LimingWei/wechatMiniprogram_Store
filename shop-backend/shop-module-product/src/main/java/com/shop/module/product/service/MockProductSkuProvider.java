package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import com.shop.module.product.controller.MockData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 开发环境的稳定 Mock SKU 与库存实现。 */
@Service
@ConditionalOnProperty(prefix = "product", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockProductSkuProvider implements ProductSkuProvider {

    private static final int INITIAL_STOCK = 100;
    private final Map<Long, MockSku> skuCatalog = new ConcurrentHashMap<>();

    public MockProductSkuProvider() {
        for (Map<String, Object> goods : MockData.GOODS_LIST) {
            Long spuId = Long.valueOf(String.valueOf(goods.get("id")));
            Long skuId = defaultSkuId(spuId);
            skuCatalog.put(skuId, new MockSku(spuId, skuId, String.valueOf(goods.get("name")),
                    String.valueOf(goods.get("listPicUrl")), parseCent(goods.get("retailPrice")),
                    new AtomicInteger(INITIAL_STOCK)));
        }
    }

    @Override
    public ProductSkuSnapshot getSnapshot(Long spuId, Long requestedSkuId) {
        if (spuId == null || spuId <= 0) {
            throw new ServerException(1101, "商品不存在");
        }
        Long skuId = normalizeSkuId(spuId, requestedSkuId);
        MockSku sku = skuCatalog.get(skuId);
        if (sku == null || !sku.spuId.equals(spuId)) {
            throw new ServerException(1101, "商品规格不存在");
        }
        ProductSkuSnapshot snapshot = new ProductSkuSnapshot();
        snapshot.setSpuId(sku.spuId);
        snapshot.setSkuId(sku.skuId);
        snapshot.setName(sku.name);
        snapshot.setPicUrl(sku.picUrl);
        snapshot.setSpecName("默认规格");
        snapshot.setPrice(sku.price);
        snapshot.setStock(sku.stock.get());
        return snapshot;
    }

    @Override
    public void reduceStock(Long skuId, int count) {
        if (count <= 0) {
            throw new ServerException(400, "商品数量必须大于 0");
        }
        MockSku sku = requireSku(skuId);
        while (true) {
            int current = sku.stock.get();
            if (current < count) {
                throw new ServerException(1201, "商品库存不足");
            }
            if (sku.stock.compareAndSet(current, current - count)) {
                return;
            }
        }
    }

    @Override
    public void recoverStock(Long skuId, int count) {
        if (count > 0) {
            requireSku(skuId).stock.addAndGet(count);
        }
    }

    private MockSku requireSku(Long skuId) {
        MockSku sku = skuCatalog.get(skuId);
        if (sku == null) {
            throw new ServerException(1101, "商品规格不存在");
        }
        return sku;
    }

    private Long normalizeSkuId(Long spuId, Long requestedSkuId) {
        if (requestedSkuId == null || requestedSkuId <= 0 || requestedSkuId.equals(spuId)) {
            return defaultSkuId(spuId);
        }
        return requestedSkuId;
    }

    private static Long defaultSkuId(Long spuId) {
        return spuId * 1000 + 1;
    }

    private static int parseCent(Object amount) {
        return new BigDecimal(String.valueOf(amount)).movePointRight(2).intValueExact();
    }

    private record MockSku(Long spuId, Long skuId, String name, String picUrl, Integer price, AtomicInteger stock) {
    }
}
