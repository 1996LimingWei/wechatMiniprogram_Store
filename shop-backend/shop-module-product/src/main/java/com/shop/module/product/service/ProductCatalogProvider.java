package com.shop.module.product.service;

import java.util.Map;

/** 商品目录只读契约，数据来源切换不改变正式 API。 */
public interface ProductCatalogProvider {
    String type();
    Map<String, Object> catalogIndex();
    Map<String, Object> catalog(Long id);
    Map<String, Object> goodsCategory(Long id);
    Map<String, Object> count();
    Map<String, Object> list(Long categoryId, String keyword, int isHot, int isNew, int page, int size);
    Map<String, Object> detail(Long id);
    Map<String, Object> related(Long id);
}
