package com.shop.module.product.service;

import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppProductQueryServiceTest {
    @Test
    void shouldBuildFullSkuReadModelAndIgnoreMalformedProperties() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(1L); spu.setCategoryId(1L); spu.setStatus(1); spu.setName("SKU product"); spu.setPicUrl("spu-pic"); spu.setPrice(9900); spu.setMarketPrice(10900); spu.setSalesCount(2);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(skuMapper.selectList(any())).thenReturn(List.of(
                sku(3L, "[{\"id\":20,\"name\":\"Color\",\"valueId\":202,\"valueName\":\"Blue\"},{\"id\":10,\"name\":\"Size\",\"valueId\":102,\"valueName\":\"Large\"}]", 1500, 1800, 5, "blue-large"),
                sku(1L, "[{\"id\":10,\"name\":\"Size\",\"valueId\":101,\"valueName\":\"Small\"},{\"id\":20,\"name\":\"Color\",\"valueId\":201,\"valueName\":\"Red\"}]", 1200, 1600, 3, "red-small"),
                sku(2L, "[{\"id\":10,\"name\":\"Size\",\"valueId\":101,\"valueName\":\"Small\"},{\"id\":20,\"name\":\"Color\",\"valueId\":202,\"valueName\":\"Blue\"}]", 1300, 1700, 0, "blue-small"),
                sku(4L, "{not-json", 999, 999, 1, "broken")
        ));
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(0);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        Map<String, Object> result = new AppProductQueryService(mock(CategoryMapper.class), spuMapper, skuMapper, jdbc).detail(1L);
        List<Map<String, Object>> specifications = (List<Map<String, Object>>) result.get("specificationList");
        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("productList");

        assertEquals(List.of(10L, 20L), specifications.stream().map(item -> item.get("specificationId")).toList());
        assertEquals(List.of(101L, 102L), ((List<Map<String, Object>>) specifications.get(0).get("valueList")).stream().map(item -> item.get("id")).toList());
        assertEquals(List.of(201L, 202L), ((List<Map<String, Object>>) specifications.get(1).get("valueList")).stream().map(item -> item.get("id")).toList());

        Map<String, Object> inStockSku = products.stream().filter(item -> item.get("id").equals(1L)).findFirst().orElseThrow();
        Map<String, Object> soldOutSku = products.stream().filter(item -> item.get("id").equals(2L)).findFirst().orElseThrow();
        assertEquals("101_201", inStockSku.get("goodsSpecificationIds"));
        assertEquals(List.of(101L, 201L), inStockSku.get("specificationValueIds"));
        assertEquals("12.00", inStockSku.get("retailPrice"));
        assertEquals("red-small", inStockSku.get("picUrl"));
        assertEquals(3, inStockSku.get("stock"));
        assertTrue((Boolean) inStockSku.get("available"));
        assertFalse((Boolean) soldOutSku.get("available"));
        assertEquals("", products.stream().filter(item -> item.get("id").equals(4L)).findFirst().orElseThrow().get("goodsSpecificationIds"));
    }

    @Test
    void shouldReturnDatabaseCommentSummaryForProductDetail() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(1L); spu.setCategoryId(1L); spu.setStatus(1); spu.setName("测试商品"); spu.setPicUrl("pic"); spu.setPrice(9900); spu.setMarketPrice(10900); spu.setSalesCount(2);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(skuMapper.selectList(any())).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(2);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("content", "很好", "addTime", "2026-07-30", "nickname", "用户", "avatar", "")));

        Map<String, Object> result = new AppProductQueryService(mock(CategoryMapper.class), spuMapper, skuMapper, jdbc).detail(1L);
        Map<String, Object> comment = (Map<String, Object>) result.get("comment");

        assertEquals(2, comment.get("count"));
        assertEquals("很好", ((Map<?, ?>) comment.get("data")).get("content"));
        assertEquals(0, result.get("userHasCollect"));
    }

    private ProductSkuDO sku(Long id, String properties, int price, int marketPrice, int stock, String picUrl) {
        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(id); sku.setSpuId(1L); sku.setProperties(properties); sku.setPrice(price); sku.setMarketPrice(marketPrice); sku.setStock(stock); sku.setPicUrl(picUrl);
        return sku;
    }
}
