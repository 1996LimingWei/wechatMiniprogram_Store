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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppProductQueryServiceTest {
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

        Map<String, Object> result = new AppProductQueryService(mock(CategoryMapper.class), spuMapper, skuMapper, jdbc,
                mock(ProductSearchService.class)).detail(1L);
        Map<String, Object> comment = (Map<String, Object>) result.get("comment");

        assertEquals(2, comment.get("count"));
        assertEquals("很好", ((Map<?, ?>) comment.get("data")).get("content"));
        assertEquals(0, result.get("userHasCollect"));
    }
}
