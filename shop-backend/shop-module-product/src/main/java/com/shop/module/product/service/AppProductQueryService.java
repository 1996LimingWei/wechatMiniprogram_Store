package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ErrorCode;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.shop.framework.security.LoginUser;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppProductQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final CategoryMapper categoryMapper;
    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> catalogIndex() {
        List<CategoryDO> roots = categories().stream().filter(c -> c.getParentId() == 0).toList();
        Long id = roots.isEmpty() ? 0L : roots.get(0).getId();
        return Map.of("categoryList", roots.stream().map(this::categoryBrief).toList(), "currentCategory", catalog(id));
    }
    public Map<String, Object> catalog(Long id) {
        CategoryDO current = category(id);
        return Map.of("id", current.getId(), "name", current.getName(), "frontName", current.getName() + "精选好物", "wapBannerUrl", current.getIcon() == null ? "" : current.getIcon(), "subCategoryList", categories().stream().filter(c -> Objects.equals(c.getParentId(), current.getId())).map(this::categoryBrief).toList());
    }
    public Map<String, Object> goodsCategory(Long id) { return Map.of("brotherCategory", categories().stream().filter(c -> c.getParentId() == 0).map(this::categoryBrief).toList(), "currentCategory", categoryBrief(category(id))); }
    public Map<String, Object> count() { return Map.of("goodsCount", productSpuMapper.selectCount(available())); }
    public Map<String, Object> list(Long categoryId, String keyword, int isHot, int isNew, int page, int size) {
        List<ProductSpuDO> all = productSpuMapper.selectList(available());
        Set<Long> ids = categoryIds(categoryId);
        all = all.stream().filter(s -> ids.isEmpty() || ids.contains(s.getCategoryId())).filter(s -> keyword == null || keyword.isBlank() || (s.getName()+s.getKeyword()+s.getIntroduction()).contains(keyword)).sorted(isNew == 1 ? Comparator.comparing(ProductSpuDO::getCreateTime).reversed() : Comparator.comparing(ProductSpuDO::getSalesCount, Comparator.nullsLast(Integer::compareTo)).reversed()).toList();
        int from = Math.min(Math.max(page - 1, 0) * size, all.size()), to = Math.min(from + size, all.size());
        return Map.of("goodsList", Map.of("records", all.subList(from, to).stream().map(this::goods).toList(), "current", page, "size", size, "total", all.size(), "pages", (all.size()+size-1)/size), "filterCategory", categories().stream().filter(c -> c.getParentId()==0).map(c -> Map.of("id",c.getId(),"name",c.getName(),"checked",Objects.equals(c.getId(),categoryId))).toList());
    }
    public Map<String,Object> detail(Long id) {
        ProductSpuDO s = productSpuMapper.selectById(id); if (s == null || s.getStatus() != 1) throw new ServerException(ErrorCode.PRODUCT_NOT_EXISTS);
        List<ProductSkuDO> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>().eq(ProductSkuDO::getSpuId,id));
        List<SkuReadModel> skuModels = skus.stream().map(this::skuReadModel).toList();
        List<Map<String, Object>> specifications = specificationList(skuModels);
        Long userId = currentUserId();
        if (userId != null) recordFootprint(userId, id);
        Integer commentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_comment WHERE spu_id=? AND status=1 AND deleted=0", Integer.class, id);
        List<Map<String, Object>> comments = jdbcTemplate.queryForList("SELECT c.content, DATE_FORMAT(c.create_time,'%Y-%m-%d') addTime, u.nickname, u.avatar FROM product_comment c JOIN member_user u ON u.id=c.user_id WHERE c.spu_id=? AND c.status=1 AND c.deleted=0 ORDER BY c.create_time DESC LIMIT 1", id);
        Map<String, Object> comment = new LinkedHashMap<>();
        comment.put("count", commentCount == null ? 0 : commentCount);
        if (!comments.isEmpty()) {
            Map<String, Object> latest = new LinkedHashMap<>(comments.get(0));
            latest.put("picList", List.of());
            comment.put("data", latest);
        }
        int userHasCollect = userId == null ? 0 : count("SELECT COUNT(*) FROM member_collect WHERE user_id=? AND spu_id=? AND deleted=0", userId, id) > 0 ? 1 : 0;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", goods(s)); result.put("gallery", List.of(Map.of("id",1,"imgUrl",s.getPicUrl()))); result.put("specificationList", specifications); result.put("productList",skuModels.stream().map(this::product).toList()); result.put("attribute",List.of()); result.put("issue",List.of()); result.put("comment",comment); result.put("brand",Map.of()); result.put("userHasCollect",userHasCollect);
        return result;
    }
    public List<Map<String,Object>> related(Long id) { ProductSpuDO s=productSpuMapper.selectById(id); return s==null?List.of():productSpuMapper.selectList(available()).stream().filter(x->!Objects.equals(x.getId(),id)&&Objects.equals(x.getCategoryId(),s.getCategoryId())).limit(4).map(this::goods).toList(); }
    private LambdaQueryWrapper<ProductSpuDO> available(){return new LambdaQueryWrapper<ProductSpuDO>().eq(ProductSpuDO::getStatus,1).orderByDesc(ProductSpuDO::getSort);}
    private List<CategoryDO> categories(){return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>().eq(CategoryDO::getStatus,1).orderByDesc(CategoryDO::getSort));}
    private CategoryDO category(Long id){return categories().stream().filter(c->Objects.equals(c.getId(),id)).findFirst().orElseThrow(()->new ServerException(ErrorCode.PRODUCT_NOT_EXISTS));}
    private Set<Long> categoryIds(Long id){if(id==null||id==0)return Set.of(); return categories().stream().filter(c->Objects.equals(c.getId(),id)||Objects.equals(c.getParentId(),id)).map(CategoryDO::getId).collect(Collectors.toSet());}
    private Map<String,Object> categoryBrief(CategoryDO c){return Map.of("id",c.getId(),"name",c.getName(),"wapBannerUrl",c.getIcon()==null?"":c.getIcon());}
    private Map<String,Object> goods(ProductSpuDO s){return Map.of("id",s.getId(),"name",s.getName(),"goodsBrief",s.getIntroduction()==null?"":s.getIntroduction(),"goodsDesc",s.getDescription()==null?"":s.getDescription(),"listPicUrl",s.getPicUrl(),"retailPrice",AppProductResponseAssembler.formatPrice(s.getPrice()),"counterPrice",AppProductResponseAssembler.formatPrice(s.getMarketPrice()),"sellVolume",s.getSalesCount()==null?0:s.getSalesCount(),"categoryId",s.getCategoryId());}
    private List<Map<String, Object>> specificationList(List<SkuReadModel> skus) {
        Map<Long, Map<Long, SkuProperty>> dimensions = new TreeMap<>();
        for (SkuReadModel sku : skus) for (SkuProperty property : sku.properties()) dimensions.computeIfAbsent(property.specificationId(), ignored -> new TreeMap<>()).putIfAbsent(property.valueId(), property);
        return dimensions.entrySet().stream().map(entry -> {
            List<Map<String, Object>> values = entry.getValue().values().stream().map(property -> new LinkedHashMap<>(Map.<String, Object>of("id", property.valueId(), "specificationId", property.specificationId(), "value", property.valueName(), "checked", false))).map(value -> (Map<String, Object>) value).toList();
            return Map.<String, Object>of("specificationId", entry.getKey(), "name", entry.getValue().values().iterator().next().name(), "valueList", values);
        }).toList();
    }
    private SkuReadModel skuReadModel(ProductSkuDO sku) {
        List<SkuProperty> properties = new ArrayList<>();
        try { for (Map<String, Object> item : OBJECT_MAPPER.readValue(sku.getProperties(), new TypeReference<List<Map<String, Object>>>() {})) { Long specificationId = longValue(item.get("id")); Long valueId = longValue(item.get("valueId")); String name = stringValue(item.get("name")); String valueName = stringValue(item.get("valueName")); if (specificationId != null && valueId != null && !name.isBlank() && !valueName.isBlank()) properties.add(new SkuProperty(specificationId, valueId, name, valueName)); } } catch (Exception ignored) { }
        properties.sort(Comparator.comparing(SkuProperty::specificationId).thenComparing(SkuProperty::valueId)); return new SkuReadModel(sku, properties);
    }
    private Map<String, Object> product(SkuReadModel model) {
        ProductSkuDO sku = model.sku(); List<Long> valueIds = model.properties().stream().map(SkuProperty::valueId).toList(); int stock = sku.getStock() == null ? 0 : sku.getStock(); Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", sku.getId()); result.put("goodsSpecificationIds", valueIds.stream().map(String::valueOf).collect(Collectors.joining("_"))); result.put("specificationValueIds", valueIds); result.put("properties", model.properties().stream().map(p -> Map.of("specificationId",p.specificationId(),"valueId",p.valueId(),"name",p.name(),"value",p.valueName())).toList()); result.put("goodsNumber", stock); result.put("stock", stock); result.put("available", stock > 0); result.put("retailPrice", AppProductResponseAssembler.formatPrice(sku.getPrice())); result.put("counterPrice", AppProductResponseAssembler.formatPrice(sku.getMarketPrice())); result.put("picUrl", sku.getPicUrl() == null ? "" : sku.getPicUrl()); return result;
    }
    private Long longValue(Object value) { if (value instanceof Number number) return number.longValue(); try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; } }
    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private record SkuProperty(Long specificationId, Long valueId, String name, String valueName) { }
    private record SkuReadModel(ProductSkuDO sku, List<SkuProperty> properties) { }
    private Long currentUserId() { Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); return authentication != null && authentication.getPrincipal() instanceof LoginUser user ? user.getUserId() : null; }
    private int count(String sql, Object... args) { Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args); return value == null ? 0 : value; }
    private void recordFootprint(Long userId, Long spuId) { int updated = jdbcTemplate.update("UPDATE member_footprint SET update_time=CURRENT_TIMESTAMP WHERE user_id=? AND spu_id=? AND browse_date=CURRENT_DATE AND deleted=0", userId, spuId); if (updated == 0) jdbcTemplate.update("INSERT INTO member_footprint(user_id,spu_id,browse_date) VALUES (?,?,CURRENT_DATE)", userId, spuId); }
}
