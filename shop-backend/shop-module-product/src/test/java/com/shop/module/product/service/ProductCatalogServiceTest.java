package com.shop.module.product.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCatalogServiceTest {
    @Test
    void selectsConfiguredProvider() {
        ProductCatalogProvider mockProvider = provider("mock");
        ProductCatalogProvider databaseProvider = provider("database");
        assertSame(databaseProvider, new ProductCatalogService(List.of(mockProvider, databaseProvider), "database").current());
        assertSame(mockProvider, new ProductCatalogService(List.of(mockProvider, databaseProvider), "mock").current());
    }

    @Test
    void rejectsUnknownProvider() {
        ProductCatalogService service = new ProductCatalogService(List.of(provider("mock")), "unknown");
        assertThrows(RuntimeException.class, service::current);
    }

    private ProductCatalogProvider provider(String type) {
        ProductCatalogProvider provider = mock(ProductCatalogProvider.class);
        when(provider.type()).thenReturn(type);
        return provider;
    }
}
