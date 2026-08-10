package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductInventoryServiceTest {

    @Test
    void shouldBuildSnapshotFromAvailableSku() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(10L);
        spu.setStatus(1);
        spu.setName("测试商品");
        spu.setPicUrl("https://example.com/spu.png");
        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(20L);
        sku.setSpuId(10L);
        sku.setPrice(1990);
        sku.setStock(3);
        sku.setPicUrl("https://example.com/sku.png");
        sku.setProperties("[{\"name\":\"规格\",\"valueName\":\"250g\"}]");
        when(spuMapper.selectById(10L)).thenReturn(spu);
        when(skuMapper.selectById(20L)).thenReturn(sku);

        ProductInventoryService.ProductSnapshot snapshot = new ProductInventoryService(spuMapper, skuMapper)
                .getAvailableSnapshot(10L, 20L);

        assertEquals(20L, snapshot.skuId());
        assertEquals(1990, snapshot.price());
        assertEquals("规格：250g", snapshot.specName());
        assertEquals("https://example.com/sku.png", snapshot.picUrl());
    }

    @Test
    void shouldRejectInsufficientSkuStock() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(skuMapper.update(isNull(), any())).thenReturn(0);

        ServerException exception = assertThrows(ServerException.class,
                () -> new ProductInventoryService(spuMapper, skuMapper).reduceSkuStock(20L, 2));

        assertEquals(1201, exception.getCode());
    }

    @Test
    void shouldSynchronizeSpuStockAfterSkuDeduction() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(20L);
        sku.setSpuId(10L);
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        when(skuMapper.selectById(20L)).thenReturn(sku);

        new ProductInventoryService(spuMapper, skuMapper).reduceSkuStock(20L, 2);

        verify(spuMapper).update(isNull(), any());
    }

    @Test
    void shouldRejectSkuThatBelongsToAnotherSpu() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(10L);
        spu.setStatus(1);
        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(20L);
        sku.setSpuId(11L);
        when(spuMapper.selectById(10L)).thenReturn(spu);
        when(skuMapper.selectById(20L)).thenReturn(sku);

        ServerException exception = assertThrows(ServerException.class,
                () -> new ProductInventoryService(spuMapper, skuMapper).getAvailableSnapshot(10L, 20L));

        assertEquals(1101, exception.getCode());
    }
}
