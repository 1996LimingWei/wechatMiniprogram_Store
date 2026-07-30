package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shop.module.product.dal.dataobject.*;
import com.shop.module.product.dal.mysql.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeContentQueryService {
    private static final int HOME_GOODS_LIMIT = 4;

    private final ContentBannerMapper bannerMapper;
    private final ContentChannelMapper channelMapper;
    private final ContentBrandMapper brandMapper;
    private final ContentTopicMapper topicMapper;
    private final CategoryMapper categoryMapper;
    private final ProductSpuMapper productSpuMapper;

    public Map<String, Object> banner() {
        return Map.of("banner", banners().stream().map(item -> Map.of(
                "id", item.getId(), "imageUrl", item.getPicUrl(), "link", safe(item.getUrl()))).toList());
    }

    public Map<String, Object> channel() {
        return Map.of("channel", channels().stream().map(item -> Map.of(
                "id", item.getId(), "name", item.getName(), "iconUrl", safe(item.getIconUrl()), "url", safe(item.getUrl()))).toList());
    }

    public Map<String, Object> brand() {
        return Map.of("brandList", brands().stream().map(item -> Map.of(
                "id", item.getId(), "name", item.getName(), "newPicUrl", safe(item.getPicUrl()),
                "floorPrice", AppProductResponseAssembler.formatPrice(item.getFloorPrice()))).toList());
    }

    public Map<String, Object> topic() {
        return Map.of("topicList", topics().stream().map(item -> Map.of(
                "id", item.getId(), "title", item.getTitle(), "subtitle", safe(item.getSubtitle()),
                "scenePicUrl", safe(item.getPicUrl()), "priceInfo", safe(item.getPriceInfo()))).toList());
    }

    public Map<String, Object> newGoods() {
        return Map.of("newGoodsList", availableGoods().stream()
                .sorted(Comparator.comparing(ProductSpuDO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(HOME_GOODS_LIMIT).map(this::goods).toList());
    }

    public Map<String, Object> hotGoods() {
        return Map.of("hotGoodsList", availableGoods().stream()
                .sorted(Comparator.comparing(ProductSpuDO::getSalesCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProductSpuDO::getId))
                .limit(HOME_GOODS_LIMIT).map(this::goods).toList());
    }

    public Map<String, Object> category() {
        List<CategoryDO> categories = enabledCategories();
        Map<Long, List<Long>> children = categories.stream()
                .collect(Collectors.groupingBy(CategoryDO::getParentId,
                        Collectors.mapping(CategoryDO::getId, Collectors.toList())));
        List<ProductSpuDO> goods = availableGoods();
        List<Map<String, Object>> floors = categories.stream()
                .filter(item -> item.getParentId() == 0)
                .map(item -> categoryFloor(item, descendants(item.getId(), children), goods))
                .filter(item -> !((List<?>) item.get("goodsList")).isEmpty())
                .toList();
        return Map.of("categoryList", floors);
    }

    private Map<String, Object> categoryFloor(CategoryDO category, Set<Long> categoryIds, List<ProductSpuDO> goods) {
        List<Map<String, Object>> floorGoods = goods.stream().filter(item -> categoryIds.contains(item.getCategoryId()))
                .sorted(Comparator.comparing(ProductSpuDO::getSort, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ProductSpuDO::getId))
                .limit(HOME_GOODS_LIMIT).map(this::goods).toList();
        return Map.of("id", category.getId(), "name", category.getName(), "goodsList", floorGoods);
    }

    private Set<Long> descendants(Long id, Map<Long, List<Long>> children) {
        Set<Long> result = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(id);
        while (!pending.isEmpty()) {
            Long current = pending.removeFirst();
            if (result.add(current)) pending.addAll(children.getOrDefault(current, List.of()));
        }
        return result;
    }

    private List<ContentBannerDO> banners() { return enabledAndSorted(bannerMapper.selectList(contentQuery()), ContentBannerDO::getStatus, ContentBannerDO::getSort, ContentBannerDO::getId); }
    private List<ContentChannelDO> channels() { return enabledAndSorted(channelMapper.selectList(contentQuery()), ContentChannelDO::getStatus, ContentChannelDO::getSort, ContentChannelDO::getId); }
    private List<ContentBrandDO> brands() { return enabledAndSorted(brandMapper.selectList(contentQuery()), ContentBrandDO::getStatus, ContentBrandDO::getSort, ContentBrandDO::getId); }
    private List<ContentTopicDO> topics() { return enabledAndSorted(topicMapper.selectList(contentQuery()), ContentTopicDO::getStatus, ContentTopicDO::getSort, ContentTopicDO::getId); }
    private <T> QueryWrapper<T> contentQuery() {
        return new QueryWrapper<T>().eq("status", 1).orderByDesc("sort").orderByAsc("id");
    }
    private List<CategoryDO> enabledCategories() { return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>().eq(CategoryDO::getStatus, 1).orderByDesc(CategoryDO::getSort).orderByAsc(CategoryDO::getId)).stream().filter(item -> item.getStatus() == 1).toList(); }
    private List<ProductSpuDO> availableGoods() { return productSpuMapper.selectList(new LambdaQueryWrapper<ProductSpuDO>().eq(ProductSpuDO::getStatus, 1)).stream().filter(item -> item.getStatus() == 1).toList(); }
    private <T> List<T> enabledAndSorted(List<T> source, java.util.function.Function<T, Integer> status, java.util.function.Function<T, Integer> sort, java.util.function.Function<T, Long> id) {
        return source.stream().filter(item -> status.apply(item) == 1).sorted(Comparator.comparing(sort, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(id)).toList();
    }
    private Map<String, Object> goods(ProductSpuDO item) { return Map.of("id", item.getId(), "name", item.getName(), "listPicUrl", safe(item.getPicUrl()), "retailPrice", AppProductResponseAssembler.formatPrice(item.getPrice()), "goodsBrief", safe(item.getIntroduction())); }
    private String safe(String value) { return value == null ? "" : value; }
}
