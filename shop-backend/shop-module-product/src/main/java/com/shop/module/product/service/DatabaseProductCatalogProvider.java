package com.shop.module.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 数据库商品目录实现。 */
@Service
@RequiredArgsConstructor
public class DatabaseProductCatalogProvider implements ProductCatalogProvider {
    private final AppProductQueryService queryService;

    @Override public String type() { return "database"; }
    @Override public Map<String, Object> catalogIndex() { return queryService.catalogIndex(); }
    @Override public Map<String, Object> catalog(Long id) { return queryService.catalog(id); }
    @Override public Map<String, Object> goodsCategory(Long id) { return queryService.goodsCategory(id); }
    @Override public Map<String, Object> count() { return queryService.count(); }
    @Override public Map<String, Object> list(Long categoryId, String keyword, int isHot, int isNew, int page, int size) { return queryService.list(categoryId, keyword, isHot, isNew, page, size); }
    @Override public Map<String, Object> detail(Long id) { return queryService.detail(id); }
    @Override public Map<String, Object> related(Long id) { return Map.of("goodsList", queryService.related(id)); }
}
