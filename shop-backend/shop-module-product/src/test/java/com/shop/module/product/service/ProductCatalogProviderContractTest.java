package com.shop.module.product.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCatalogProviderContractTest {
    @Test
    void mockAndDatabaseProvidersFollowSameCoreContract() {
        MockProductCatalogProvider mockProvider = new MockProductCatalogProvider();
        AppProductQueryService queryService = mock(AppProductQueryService.class);
        when(queryService.list(0L, "", 0, 0, 1, 10)).thenReturn(mockProvider.list(0L, "", 0, 0, 1, 10));
        when(queryService.detail(1L)).thenReturn(mockProvider.detail(1L));
        DatabaseProductCatalogProvider databaseProvider = new DatabaseProductCatalogProvider(queryService);

        assertListContract(mockProvider.list(0L, "", 0, 0, 1, 10));
        assertListContract(databaseProvider.list(0L, "", 0, 0, 1, 10));
        assertDetailContract(mockProvider.detail(1L));
        assertDetailContract(databaseProvider.detail(1L));
    }

    @Test
    void mockProviderFiltersAndRejectsUnknownGoods() {
        MockProductCatalogProvider provider = new MockProductCatalogProvider();
        Map<String, Object> result = provider.list(1L, "枸杞", 1, 0, 1, 10);
        Map<String, Object> page = castMap(result.get("goodsList"));
        assertEquals(1, ((List<?>) page.get("records")).size());
        assertThrows(RuntimeException.class, () -> provider.detail(999L));
    }

    private void assertListContract(Map<String, Object> result) {
        assertTrue(result.containsKey("goodsList"));
        assertTrue(result.containsKey("filterCategory"));
        Map<String, Object> page = castMap(result.get("goodsList"));
        for (String field : List.of("records", "current", "size", "total", "pages")) assertTrue(page.containsKey(field));
    }

    private void assertDetailContract(Map<String, Object> result) {
        for (String field : List.of("info", "gallery", "specificationList", "productList", "comment", "userHasCollect")) assertTrue(result.containsKey(field));
        Map<String, Object> product = castMap(((List<?>) result.get("productList")).get(0));
        for (String field : List.of("id", "specificationValueIds", "stock", "available", "retailPrice", "picUrl")) assertTrue(product.containsKey(field));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) { return (Map<String, Object>) value; }
}
