package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockProductSkuProviderTest {

    @Test
    void shouldUseStableSkuAndRecoverStock() {
        MockProductSkuProvider provider = new MockProductSkuProvider();

        ProductSkuSnapshot snapshot = provider.getSnapshot(1L, 1L);
        assertEquals(1001L, snapshot.getSkuId());
        assertEquals(100, snapshot.getStock());

        provider.reduceStock(snapshot.getSkuId(), 2);
        assertEquals(98, provider.getSnapshot(1L, snapshot.getSkuId()).getStock());
        provider.recoverStock(snapshot.getSkuId(), 2);
        assertEquals(100, provider.getSnapshot(1L, snapshot.getSkuId()).getStock());
    }

    @Test
    void shouldRejectUnknownSkuAndInsufficientStock() {
        MockProductSkuProvider provider = new MockProductSkuProvider();

        assertThrows(ServerException.class, () -> provider.getSnapshot(1L, 999L));
        ServerException exception = assertThrows(ServerException.class,
                () -> provider.reduceStock(1001L, 101));
        assertEquals(1201, exception.getCode());
    }
}
