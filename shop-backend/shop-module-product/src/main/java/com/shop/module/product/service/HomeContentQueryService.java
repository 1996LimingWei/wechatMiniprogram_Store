package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shop.common.exception.ServerException;
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
    private final ContentTopicProductMapper topicProductMapper;
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

    public Map<String, Object> goodsHot() {
        return Map.of("bannerInfo", Map.of("imgUrl", "https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=750&auto=format&fit=crop", "name", "热销爆款"));
    }

    public Map<String, Object> goodsNew() {
        return Map.of("bannerInfo", Map.of("imgUrl", "https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=750&auto=format&fit=crop", "name", "新品推荐"));
    }

    public Map<String, Object> brandList() {
        List<Map<String, Object>> records = brands().stream().map(item -> Map.<String, Object>of(
                "id", item.getId(), "name", item.getName(), "picUrl", safe(item.getPicUrl()),
                "floorPrice", AppProductResponseAssembler.formatPrice(item.getFloorPrice()))).toList();
        return Map.of("brandList", records, "totalPages", records.isEmpty() ? 0 : 1);
    }

    public Map<String, Object> brandDetail(Long id) {
        ContentBrandDO item = brands().stream().filter(brand -> Objects.equals(brand.getId(), id)).findFirst()
                .orElseThrow(() -> new ServerException(404, "品牌不存在"));
        return Map.of("brand", Map.of("id", item.getId(), "name", item.getName(), "picUrl", safe(item.getPicUrl()),
                "simpleDesc", item.getName() + "，品质保证，值得信赖"));
    }

    public Map<String, Object> topicList(int page, int size) {
        List<Map<String, Object>> source = topics().stream().map(this::topicItem).toList();
        int safePage = Math.max(page, 1), safeSize = Math.max(size, 1);
        int from = Math.min((safePage - 1) * safeSize, source.size()), to = Math.min(from + safeSize, source.size());
        return Map.of("records", source.subList(from, to), "total", source.size(), "pages", (source.size() + safeSize - 1) / safeSize);
    }

    public Map<String, Object> topicDetail(Long id) {
        ContentTopicDO item = topics().stream().filter(topic -> Objects.equals(topic.getId(), id)).findFirst()
                .orElseThrow(() -> new ServerException(404, "专题不存在"));
        Map<String, Object> result = new LinkedHashMap<>(topicItem(item));
        result.put("content", "<p>" + safe(item.getSubtitle()) + "</p>");
        // 查询关联商品
        List<ContentTopicProductDO> associations = topicProductMapper.selectList(
                new LambdaQueryWrapper<ContentTopicProductDO>()
                        .eq(ContentTopicProductDO::getTopicId, id)
                        .orderByDesc(ContentTopicProductDO::getSort));
        if (!associations.isEmpty()) {
            Set<Long> spuIds = associations.stream().map(ContentTopicProductDO::getSpuId).collect(Collectors.toSet());
            List<ProductSpuDO> products = productSpuMapper.selectList(
                    new LambdaQueryWrapper<ProductSpuDO>().in(ProductSpuDO::getId, spuIds).eq(ProductSpuDO::getStatus, 1));
            // 保持关联表排序
            Map<Long, ProductSpuDO> productMap = products.stream().collect(Collectors.toMap(ProductSpuDO::getId, p -> p));
            List<Map<String, Object>> goodsList = associations.stream()
                    .map(ContentTopicProductDO::getSpuId)
                    .filter(productMap::containsKey)
                    .map(spuId -> goods(productMap.get(spuId)))
                    .toList();
            result.put("goodsList", goodsList);
        } else {
            result.put("goodsList", List.of());
        }
        return result;
    }

    public List<Map<String, Object>> topicRelated(Long id) {
        return topics().stream().filter(topic -> !Objects.equals(topic.getId(), id)).limit(2).map(this::topicItem).toList();
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
    private Map<String, Object> topicItem(ContentTopicDO item) { return Map.of("id", item.getId(), "title", item.getTitle(), "subtitle", safe(item.getSubtitle()), "scenePicUrl", safe(item.getPicUrl()), "priceInfo", safe(item.getPriceInfo())); }
    private String safe(String value) { return value == null ? "" : value; }
}
