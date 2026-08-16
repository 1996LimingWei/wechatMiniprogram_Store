package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductAdminService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_SKU_STOCK = 1_000_000;
    private static final int MAX_PRICE_CENTS = 100_000_000;
    private static final Pattern DANGEROUS_HTML = Pattern.compile(
            "(?i)<\\s*(script|iframe|object|embed|form|link|meta)|javascript\\s*:|on[a-z]+\\s*=");
    private static final Pattern IMG_SRC = Pattern.compile(
            "(?i)<img\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1");

    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final ProductInventoryService productInventoryService;
    private final JdbcTemplate jdbcTemplate;
    private final MaterialAssetService materialAssetService;

    @Transactional(rollbackFor = Exception.class)
    public Long saveProduct(ProductSpuDO spu, List<ProductSkuDO> requestedSkus) {
        return saveProduct(spu, requestedSkus, 0L, "系统初始化库存");
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveProduct(ProductSpuDO spu, List<ProductSkuDO> requestedSkus,
                            Long adminId, String stockAdjustReason) {
        if (spu == null) {
            throw new ServerException(400, "商品信息不能为空");
        }
        List<ProductSkuDO> skus = normalizeRequestedSkus(spu, requestedSkus);
        applySkuSummary(spu, skus);
        validateSpu(spu);

        boolean creating = spu.getId() == null;
        if (creating) {
            spu.setSalesCount(0);
            productSpuMapper.insert(spu);
        } else if (productSpuMapper.selectById(spu.getId()) == null) {
            throw new ServerException(1101, "商品不存在");
        } else {
            spu.setSalesCount(null);
            if (productSpuMapper.updateById(spu) != 1) {
                throw new ServerException(409, "商品信息已变化，请刷新后重试");
            }
        }
        String reason = creating && (stockAdjustReason == null || stockAdjustReason.isBlank())
                ? "商品创建初始化库存" : stockAdjustReason;
        saveSkusInternal(spu.getId(), skus, adminStockBizNo(), adminId, reason);
        syncSpuSummary(spu.getId());
        materialAssetService.refreshAllReferenceCounts();
        return spu.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSkus(Long spuId, List<ProductSkuDO> requestedSkus) {
        saveSkus(spuId, requestedSkus, 0L, "系统调整库存");
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSkus(Long spuId, List<ProductSkuDO> requestedSkus,
                         Long adminId, String stockAdjustReason) {
        ProductSpuDO spu = requireSpu(spuId);
        List<ProductSkuDO> skus = normalizeRequestedSkus(spu, requestedSkus);
        saveSkusInternal(spuId, skus, adminStockBizNo(), adminId, stockAdjustReason);
        syncSpuSummary(spuId);
        materialAssetService.refreshAllReferenceCounts();
    }

    public List<ProductSkuDO> listSkus(Long spuId) {
        requireSpu(spuId);
        return productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getSpuId, spuId)
                .orderByAsc(ProductSkuDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSpu(ProductSpuDO request) {
        if (request == null || request.getId() == null) {
            throw new ServerException(400, "商品 ID 不能为空");
        }
        ProductSpuDO current = requireSpu(request.getId());
        if (requiresMergedValidation(request)) {
            ProductSpuDO merged = mergeForValidation(current, request);
            validateSpu(merged);
            if (Integer.valueOf(1).equals(merged.getStatus())) {
                List<ProductSkuDO> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                        .eq(ProductSkuDO::getSpuId, current.getId()));
                if (skus.isEmpty()) {
                    throw new ServerException(400, "商品至少需要一个有效规格才能上架");
                }
                for (ProductSkuDO sku : skus) {
                    validateSku(sku);
                }
                syncSpuSummary(current.getId());
            }
        }
        request.setPrice(null);
        request.setMarketPrice(null);
        request.setStock(null);
        request.setSalesCount(null);
        if (productSpuMapper.updateById(request) != 1) {
            throw new ServerException(409, "商品信息已变化，请刷新后重试");
        }
        materialAssetService.refreshAllReferenceCounts();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long spuId) {
        ProductSpuDO spu = requireSpu(spuId);
        if (Integer.valueOf(1).equals(spu.getStatus())) {
            throw new ServerException(400, "请先下架商品，再执行删除");
        }
        if (hasTradeReferenceBySpu(spuId)) {
            throw new ServerException(400, "商品仍被购物车或未结束订单引用，不能删除");
        }
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getSpuId, spuId));
        productSpuMapper.deleteById(spuId);
        materialAssetService.refreshAllReferenceCounts();
    }

    private void saveSkusInternal(Long spuId, List<ProductSkuDO> requestedSkus, String stockBizNo,
                                  Long adminId, String stockAdjustReason) {
        List<ProductSkuDO> existingSkus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSkuDO>()
                        .eq(ProductSkuDO::getSpuId, spuId)
                        .orderByAsc(ProductSkuDO::getId));
        Map<Long, ProductSkuDO> existingById = new HashMap<>();
        Map<String, ProductSkuDO> existingByProperties = new HashMap<>();
        for (ProductSkuDO existing : existingSkus) {
            existingById.put(existing.getId(), existing);
            existingByProperties.put(normalizeProperties(existing.getProperties()), existing);
        }

        Set<Long> retainedIds = new HashSet<>();
        for (ProductSkuDO requested : requestedSkus) {
            requested.setSpuId(spuId);
            String properties = normalizeProperties(requested.getProperties());
            requested.setProperties(properties);
            ProductSkuDO existing = requested.getId() == null
                    ? existingByProperties.get(properties)
                    : existingById.get(requested.getId());
            if (requested.getId() != null && existing == null) {
                throw new ServerException(400, "商品规格不属于当前商品");
            }
            if (existing == null) {
                requested.setId(null);
                productSkuMapper.insert(requested);
                recordAdminStockChange(requested.getId(), spuId, 0, requested.getStock(), stockBizNo,
                        adminId, stockAdjustReason);
                retainedIds.add(requested.getId());
                continue;
            }
            int updated = productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSkuDO>()
                    .eq(ProductSkuDO::getId, existing.getId())
                    .eq(ProductSkuDO::getSpuId, spuId)
                    .eq(ProductSkuDO::getStock, existing.getStock())
                    .set(ProductSkuDO::getProperties, requested.getProperties())
                    .set(ProductSkuDO::getPrice, requested.getPrice())
                    .set(ProductSkuDO::getMarketPrice, requested.getMarketPrice())
                    .set(ProductSkuDO::getStock, requested.getStock())
                    .set(ProductSkuDO::getPicUrl, requested.getPicUrl())
                    .set(ProductSkuDO::getWeight, requested.getWeight())
                    .set(ProductSkuDO::getVolume, requested.getVolume()));
            if (updated != 1) {
                throw new ServerException(409, "规格库存已被交易修改，请刷新商品后重新保存");
            }
            recordAdminStockChange(existing.getId(), spuId, existing.getStock(), requested.getStock(), stockBizNo,
                    adminId, stockAdjustReason);
            retainedIds.add(existing.getId());
        }

        for (ProductSkuDO existing : existingSkus) {
            if (!retainedIds.contains(existing.getId())) {
                if (hasTradeReferenceBySku(existing.getId())) {
                    throw new ServerException(400, "规格已被购物车或未结束订单引用，不能删除");
                }
                recordAdminStockChange(existing.getId(), spuId, existing.getStock(), 0, stockBizNo,
                        adminId, stockAdjustReason);
                productSkuMapper.deleteById(existing.getId());
            }
        }
    }

    private List<ProductSkuDO> normalizeRequestedSkus(ProductSpuDO spu, List<ProductSkuDO> requestedSkus) {
        List<ProductSkuDO> skus = requestedSkus == null ? new ArrayList<>() : new ArrayList<>(requestedSkus);
        if (skus.isEmpty() && spu.getId() == null) {
            ProductSkuDO defaultSku = new ProductSkuDO();
            defaultSku.setProperties("[]");
            defaultSku.setPrice(spu.getPrice());
            defaultSku.setMarketPrice(spu.getMarketPrice());
            defaultSku.setStock(spu.getStock());
            defaultSku.setPicUrl(spu.getPicUrl());
            skus.add(defaultSku);
        }
        if (skus.isEmpty()) {
            throw new ServerException(400, "商品至少需要一个有效规格");
        }
        Set<String> propertyKeys = new HashSet<>();
        for (ProductSkuDO sku : skus) {
            validateSku(sku);
            String key = normalizeProperties(sku.getProperties());
            if (!propertyKeys.add(key)) {
                throw new ServerException(400, "商品规格不能重复");
            }
            sku.setProperties(key);
        }
        return skus;
    }

    private void validateSpu(ProductSpuDO spu) {
        String name = spu.getName() == null ? "" : spu.getName().trim();
        if (name.isEmpty() || name.length() > 128) {
            throw new ServerException(400, "商品名称长度应为 1 至 128 个字符");
        }
        spu.setName(name);
        CategoryDO category = spu.getCategoryId() == null ? null : categoryMapper.selectById(spu.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new ServerException(400, "请选择有效的商品分类");
        }
        if (spu.getStatus() == null || (spu.getStatus() != 0 && spu.getStatus() != 1)) {
            throw new ServerException(400, "商品状态不正确");
        }
        if (spu.getStatus() == 1 && (spu.getPicUrl() == null || spu.getPicUrl().isBlank())) {
            throw new ServerException(400, "上架商品必须设置主图");
        }
        spu.setPicUrl(normalizeResourceUrl(spu.getPicUrl(), "商品主图", spu.getStatus() == 1));
        materialAssetService.validateBusinessImageUrl(spu.getPicUrl(), "商品主图", spu.getStatus() == 1);
        spu.setSliderPicUrls(normalizeSliderPicUrls(spu.getSliderPicUrls(), spu.getPicUrl(), spu.getStatus() == 1));
        String introduction = spu.getIntroduction() == null ? "" : spu.getIntroduction().trim();
        if (introduction.length() > 255) {
            throw new ServerException(400, "商品简介不能超过 255 个字符");
        }
        spu.setIntroduction(introduction);
        String keyword = spu.getKeyword() == null ? "" : spu.getKeyword().trim();
        if (keyword.length() > 255) {
            throw new ServerException(400, "商品关键词不能超过 255 个字符");
        }
        spu.setKeyword(keyword);
        String description = spu.getDescription() == null ? "" : spu.getDescription().trim();
        if (description.length() > 100_000) {
            throw new ServerException(400, "商品详情内容过长");
        }
        if (DANGEROUS_HTML.matcher(description).find()) {
            throw new ServerException(400, "商品详情包含不安全的脚本或标签");
        }
        validateDescriptionImages(description);
        spu.setDescription(description);
    }

    private void validateSku(ProductSkuDO sku) {
        if (sku == null || sku.getPrice() == null || sku.getPrice() <= 0
                || sku.getPrice() > MAX_PRICE_CENTS) {
            throw new ServerException(400, "规格售价必须大于 0");
        }
        if (sku.getMarketPrice() != null && sku.getMarketPrice() > 0 && sku.getMarketPrice() < sku.getPrice()) {
            throw new ServerException(400, "规格市场价不能低于售价");
        }
        if (sku.getStock() == null || sku.getStock() < 0 || sku.getStock() > MAX_SKU_STOCK) {
            throw new ServerException(400, "规格库存应为 0 至 1000000");
        }
        if (sku.getWeight() != null && sku.getWeight() < 0) {
            throw new ServerException(400, "规格重量不能为负数");
        }
        if (sku.getVolume() != null && sku.getVolume() < 0) {
            throw new ServerException(400, "规格体积不能为负数");
        }
        sku.setPicUrl(normalizeResourceUrl(sku.getPicUrl(), "规格图片", false));
        materialAssetService.validateBusinessImageUrl(sku.getPicUrl(), "规格图片", false);
    }

    private String normalizeSliderPicUrls(String rawValue, String mainPicUrl, boolean required) {
        String source = rawValue == null || rawValue.isBlank()
                ? (mainPicUrl == null || mainPicUrl.isBlank() ? "[]" : toJson(List.of(mainPicUrl)))
                : rawValue.trim();
        try {
            JsonNode value = OBJECT_MAPPER.readTree(source);
            if (!value.isArray() || value.size() > 10 || (required && value.isEmpty())) {
                throw new ServerException(400, "商品轮播图应为 1 至 10 张");
            }
            Set<String> uniqueUrls = new HashSet<>();
            List<String> urls = new ArrayList<>();
            for (JsonNode node : value) {
                if (!node.isTextual()) {
                    throw new ServerException(400, "商品轮播图格式不正确");
                }
                String url = normalizeResourceUrl(node.asText(), "商品轮播图", true);
                materialAssetService.validateBusinessImageUrl(url, "商品轮播图", true);
                if (!uniqueUrls.add(url)) {
                    throw new ServerException(400, "商品轮播图不能重复");
                }
                urls.add(url);
            }
            return OBJECT_MAPPER.writeValueAsString(urls);
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(400, "商品轮播图格式不正确");
        }
    }

    private String normalizeResourceUrl(String rawValue, String fieldName, boolean required) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            if (required) throw new ServerException(400, fieldName + "不能为空");
            return "";
        }
        if (value.length() > 1024
                || (!value.startsWith("https://") && !value.startsWith("http://") && !value.startsWith("/"))) {
            throw new ServerException(400, fieldName + "必须使用 HTTP(S) 或站内路径");
        }
        return value;
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ServerException(500, "商品图片数据序列化失败");
        }
    }

    private void validateDescriptionImages(String description) {
        var matcher = IMG_SRC.matcher(description);
        while (matcher.find()) {
            materialAssetService.validateBusinessImageUrl(matcher.group(2), "商品详情图", true);
        }
    }

    private boolean requiresMergedValidation(ProductSpuDO request) {
        return request.getStatus() != null
                || request.getCategoryId() != null
                || request.getName() != null
                || request.getPicUrl() != null
                || request.getSliderPicUrls() != null
                || request.getDescription() != null;
    }

    private ProductSpuDO mergeForValidation(ProductSpuDO current, ProductSpuDO request) {
        ProductSpuDO merged = new ProductSpuDO();
        merged.setId(current.getId());
        merged.setName(request.getName() != null ? request.getName() : current.getName());
        merged.setCategoryId(request.getCategoryId() != null ? request.getCategoryId() : current.getCategoryId());
        merged.setKeyword(request.getKeyword() != null ? request.getKeyword() : current.getKeyword());
        merged.setIntroduction(request.getIntroduction() != null ? request.getIntroduction() : current.getIntroduction());
        merged.setDescription(request.getDescription() != null ? request.getDescription() : current.getDescription());
        merged.setPicUrl(request.getPicUrl() != null ? request.getPicUrl() : current.getPicUrl());
        merged.setSliderPicUrls(request.getSliderPicUrls() != null ? request.getSliderPicUrls() : current.getSliderPicUrls());
        merged.setStatus(request.getStatus() != null ? request.getStatus() : current.getStatus());
        return merged;
    }

    private void applySkuSummary(ProductSpuDO spu, List<ProductSkuDO> skus) {
        try {
            int stock = 0;
            int minPrice = Integer.MAX_VALUE;
            Integer minMarketPrice = null;
            for (ProductSkuDO sku : skus) {
                stock = Math.addExact(stock, sku.getStock());
                minPrice = Math.min(minPrice, sku.getPrice());
                if (sku.getMarketPrice() != null && sku.getMarketPrice() > 0) {
                    minMarketPrice = minMarketPrice == null
                            ? sku.getMarketPrice() : Math.min(minMarketPrice, sku.getMarketPrice());
                }
            }
            spu.setStock(stock);
            spu.setPrice(minPrice);
            spu.setMarketPrice(minMarketPrice);
        } catch (ArithmeticException exception) {
            throw new ServerException(400, "商品库存汇总超出系统上限");
        }
    }

    private void syncSpuSummary(Long spuId) {
        productInventoryService.syncSpuStock(spuId);
        ProductSkuDO cheapest = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getSpuId, spuId)
                .orderByAsc(ProductSkuDO::getPrice)
                .orderByAsc(ProductSkuDO::getId)
                .last("LIMIT 1"));
        if (cheapest == null) {
            throw new ServerException(400, "商品至少需要一个有效规格");
        }
        productSpuMapper.update(null, new LambdaUpdateWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getId, spuId)
                .set(ProductSpuDO::getPrice, cheapest.getPrice())
                .set(ProductSpuDO::getMarketPrice, cheapest.getMarketPrice()));
    }

    private void validateProductCanBeOnSale(Long spuId) {
        ProductSpuDO spu = requireSpu(spuId);
        spu.setStatus(1);
        validateSpu(spu);
        List<ProductSkuDO> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getSpuId, spuId));
        if (skus.isEmpty()) {
            throw new ServerException(400, "商品至少需要一个有效规格才能上架");
        }
        for (ProductSkuDO sku : skus) {
            validateSku(sku);
        }
        syncSpuSummary(spuId);
    }

    private ProductSpuDO requireSpu(Long spuId) {
        ProductSpuDO spu = spuId == null ? null : productSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new ServerException(1101, "商品不存在");
        }
        return spu;
    }

    private String normalizeProperties(String properties) {
        String source = properties == null || properties.isBlank() ? "[]" : properties.trim();
        try {
            JsonNode value = OBJECT_MAPPER.readTree(source);
            if (!value.isArray()) {
                throw new ServerException(400, "规格属性必须为 JSON 数组");
            }
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (ServerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ServerException(400, "规格属性格式不正确");
        }
    }

    private boolean hasTradeReferenceBySku(Long skuId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT (
                    (SELECT COUNT(*) FROM trade_cart c WHERE c.sku_id = ? AND c.deleted = 0) +
                    (SELECT COUNT(*) FROM trade_order_item oi
                     JOIN trade_order o ON o.id = oi.order_id AND o.deleted = 0
                     WHERE oi.sku_id = ? AND oi.deleted = 0
                       AND ((o.status = 0 AND o.pay_status = 0)
                            OR (o.status IN (1, 2, 3, 5) AND o.pay_status = 1)))
                )
                """, Integer.class, skuId, skuId);
        return count != null && count > 0;
    }

    private boolean hasTradeReferenceBySpu(Long spuId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT (
                    (SELECT COUNT(*) FROM trade_cart c WHERE c.spu_id = ? AND c.deleted = 0) +
                    (SELECT COUNT(*) FROM trade_order_item oi
                     JOIN trade_order o ON o.id = oi.order_id AND o.deleted = 0
                     WHERE oi.spu_id = ? AND oi.deleted = 0
                       AND ((o.status = 0 AND o.pay_status = 0)
                            OR (o.status IN (1, 2, 3, 5) AND o.pay_status = 1)))
                )
                """, Integer.class, spuId, spuId);
        return count != null && count > 0;
    }

    private String adminStockBizNo() {
        return "ADMIN-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void recordAdminStockChange(Long skuId, Long spuId, int beforeStock, int afterStock, String bizNo,
                                        Long adminId, String stockAdjustReason) {
        int change = Math.subtractExact(afterStock, beforeStock);
        if (change == 0) return;
        String reason = stockAdjustReason == null ? "" : stockAdjustReason.trim();
        if (reason.length() < 4 || reason.length() > 200) {
            throw new ServerException(400, "库存发生变化时，调整原因长度应为 4 至 200 个字符");
        }
        if (adminId == null || adminId < 0) {
            throw new ServerException(401, "库存调整缺少管理员身份");
        }
        jdbcTemplate.update("""
                INSERT INTO product_stock_log
                    (sku_id, spu_id, biz_type, biz_no, change_quantity, before_stock,
                     after_stock, operator_type, operator_id, remark)
                VALUES (?, ?, 'ADMIN_ADJUST', ?, ?, ?, ?, 'admin', ?, ?)
                """, skuId, spuId, bizNo, change, beforeStock, afterStock, adminId, reason);
    }
}
