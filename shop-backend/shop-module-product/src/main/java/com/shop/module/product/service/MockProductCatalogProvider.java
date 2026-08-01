package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import com.shop.module.product.fixture.ProductMockFixture;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mock 商品目录实现。 */
@Service
public class MockProductCatalogProvider implements ProductCatalogProvider {
    @Override public String type() { return "mock"; }

    public Map<String, Object> goods(Long id) {
        try {
            return ProductMockFixture.requireGoods(id == null ? 0 : id);
        } catch (IllegalArgumentException exception) {
            throw new ServerException(1101, "商品不存在");
        }
    }

    @Override
    public Map<String, Object> catalogIndex() {
        return Map.of("categoryList", categories(0L), "currentCategory", catalog(1L));
    }

    @Override
    public Map<String, Object> catalog(Long id) {
        String name = categoryName(id);
        String banner = ProductMockFixture.CATEGORY_BANNERS.getOrDefault(id, "");
        return Map.of("id", id, "name", name, "frontName", name + "精选好物", "wapBannerUrl", banner,
                "subCategoryList", List.of());
    }

    @Override
    public Map<String, Object> goodsCategory(Long id) {
        return Map.of("brotherCategory", categories(id), "currentCategory", catalog(id));
    }

    @Override public Map<String, Object> count() { return Map.of("goodsCount", ProductMockFixture.GOODS.size()); }

    @Override
    public Map<String, Object> list(Long categoryId, String keyword, int isHot, int isNew, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Map<String, Object>> filtered = ProductMockFixture.GOODS.stream()
                .filter(item -> categoryId == null || categoryId == 0 || ((Number) item.get("categoryId")).longValue() == categoryId)
                .filter(item -> isHot != 1 || ((Number) item.get("isHot")).intValue() == 1)
                .filter(item -> isNew != 1 || ((Number) item.get("isNew")).intValue() == 1)
                .filter(item -> normalizedKeyword.isEmpty() || (String.valueOf(item.get("name")) + item.get("goodsBrief")).contains(normalizedKeyword))
                .toList();
        int safeSize = Math.max(size, 1);
        int safePage = Math.max(page, 1);
        int from = Math.min((safePage - 1) * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        Map<String, Object> goodsList = Map.of("records", filtered.subList(from, to), "current", safePage,
                "size", safeSize, "total", filtered.size(), "pages", (filtered.size() + safeSize - 1) / safeSize);
        return Map.of("goodsList", goodsList, "filterCategory", categories(categoryId));
    }

    @Override
    public Map<String, Object> detail(Long id) {
        Map<String, Object> goods = goods(id);
        long skuId = id * 1000 + 1;
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", skuId);
        product.put("goodsSpecificationIds", "1");
        product.put("specificationValueIds", List.of(1L));
        product.put("properties", List.of(Map.of("specificationId", 1L, "valueId", 1L, "name", "规格", "value", "默认规格")));
        product.put("goodsNumber", 100);
        product.put("stock", 100);
        product.put("available", true);
        product.put("retailPrice", goods.get("retailPrice"));
        product.put("counterPrice", goods.get("counterPrice"));
        product.put("picUrl", goods.get("listPicUrl"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", goods);
        result.put("gallery", List.of(Map.of("id", 1, "imgUrl", goods.get("listPicUrl"))));
        result.put("specificationList", List.of(Map.of("specificationId", 1L, "name", "规格", "valueList",
                List.of(Map.of("id", 1L, "specificationId", 1L, "value", "默认规格", "checked", false)))));
        result.put("productList", List.of(product));
        result.put("attribute", List.of());
        result.put("issue", List.of());
        result.put("comment", Map.of("count", 0));
        result.put("brand", Map.of());
        result.put("userHasCollect", 0);
        return result;
    }

    @Override
    public Map<String, Object> related(Long id) {
        List<Map<String, Object>> related = ProductMockFixture.GOODS.stream()
                .filter(item -> !item.get("id").equals(id)).limit(4).toList();
        return Map.of("goodsList", related);
    }

    private List<Map<String, Object>> categories(Long selectedId) {
        List<Map<String, Object>> result = new ArrayList<>();
        ProductMockFixture.CATEGORY_NAMES.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Map<String, Object> category = new LinkedHashMap<>();
            category.put("id", entry.getKey());
            category.put("name", entry.getValue());
            category.put("wapBannerUrl", ProductMockFixture.CATEGORY_BANNERS.get(entry.getKey()));
            category.put("checked", entry.getKey().equals(selectedId));
            result.add(category);
        });
        return result;
    }

    private String categoryName(Long id) {
        String name = ProductMockFixture.CATEGORY_NAMES.get(id);
        if (name == null) throw new ServerException(1101, "商品分类不存在");
        return name;
    }
}
