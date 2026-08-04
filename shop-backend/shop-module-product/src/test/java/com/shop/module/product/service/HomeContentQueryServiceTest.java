package com.shop.module.product.service;

import com.shop.module.product.dal.dataobject.*;
import com.shop.module.product.dal.mysql.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeContentQueryServiceTest {

    @Test
    void shouldFilterDisabledContentAndSortBySortThenId() {
        ContentBannerMapper bannerMapper = mock(ContentBannerMapper.class);
        when(bannerMapper.selectList(any())).thenReturn(List.of(banner(3L, 50, 1), banner(2L, 50, 1), banner(1L, 100, 0)));
        HomeContentQueryService service = service(bannerMapper, mock(ContentChannelMapper.class), mock(ContentBrandMapper.class), mock(ContentTopicMapper.class), mock(ContentTopicProductMapper.class), mock(CategoryMapper.class), mock(ProductSpuMapper.class));

        List<Map<String, Object>> banners = (List<Map<String, Object>>) service.banner().get("banner");

        assertEquals(List.of(2L, 3L), banners.stream().map(item -> item.get("id")).toList());
    }

    @Test
    void shouldExcludeDisabledGoodsAndEmptyFloors() {
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        ProductSpuMapper productSpuMapper = mock(ProductSpuMapper.class);
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(1L, 0L, 100, 1), category(2L, 1L, 100, 1), category(3L, 0L, 90, 1)));
        when(productSpuMapper.selectList(any())).thenReturn(List.of(goods(11L, 2L, 1), goods(12L, 2L, 0)));
        HomeContentQueryService service = service(mock(ContentBannerMapper.class), mock(ContentChannelMapper.class), mock(ContentBrandMapper.class), mock(ContentTopicMapper.class), mock(ContentTopicProductMapper.class), categoryMapper, productSpuMapper);

        List<Map<String, Object>> floors = (List<Map<String, Object>>) service.category().get("categoryList");

        assertEquals(1, floors.size());
        assertEquals(1L, floors.get(0).get("id"));
        assertEquals(List.of(11L), ((List<Map<String, Object>>) floors.get(0).get("goodsList")).stream().map(item -> item.get("id")).toList());
    }

    private HomeContentQueryService service(ContentBannerMapper bannerMapper, ContentChannelMapper channelMapper, ContentBrandMapper brandMapper, ContentTopicMapper topicMapper, ContentTopicProductMapper topicProductMapper, CategoryMapper categoryMapper, ProductSpuMapper productSpuMapper) {
        return new HomeContentQueryService(bannerMapper, channelMapper, brandMapper, topicMapper, topicProductMapper, categoryMapper, productSpuMapper);
    }

    private ContentBannerDO banner(Long id, int sort, int status) { ContentBannerDO value = new ContentBannerDO(); value.setId(id); value.setSort(sort); value.setStatus(status); value.setPicUrl("pic"); value.setUrl(""); return value; }
    private CategoryDO category(Long id, Long parentId, int sort, int status) { CategoryDO value = new CategoryDO(); value.setId(id); value.setParentId(parentId); value.setName("分类" + id); value.setSort(sort); value.setStatus(status); return value; }
    private ProductSpuDO goods(Long id, Long categoryId, int status) { ProductSpuDO value = new ProductSpuDO(); value.setId(id); value.setCategoryId(categoryId); value.setName("商品" + id); value.setPicUrl("pic"); value.setPrice(100); value.setSalesCount(1); value.setSort(1); value.setStatus(status); return value; }
}
