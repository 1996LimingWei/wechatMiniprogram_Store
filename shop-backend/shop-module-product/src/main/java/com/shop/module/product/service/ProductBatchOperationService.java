package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import com.shop.module.product.vo.ProductBatchItemResultRespVO;
import com.shop.module.product.vo.ProductBatchOperationReqVO;
import com.shop.module.product.vo.ProductBatchOperationRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductBatchOperationService {

    private static final int MAX_BATCH_SIZE = 100;
    private static final int MAX_PRICE_CENTS = 100_000_000;
    private static final int MAX_SKU_STOCK = 1_000_000;

    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final ProductAdminService productAdminService;

    public ProductBatchOperationRespVO updateStatus(ProductBatchOperationReqVO request) {
        List<Long> ids = validateIdsAndConfirm(request);
        Integer status = request.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new ServerException(400, "批量上下架状态不正确");
        }
        return executeByProduct(ids, false, spu -> {
            ProductSpuDO update = new ProductSpuDO();
            update.setId(spu.getId());
            update.setStatus(status);
            productAdminService.updateSpu(update);
            return success(spu, status == 1 ? "上架成功" : "下架成功");
        });
    }

    public ProductBatchOperationRespVO updateCategory(ProductBatchOperationReqVO request) {
        List<Long> ids = validateIdsAndConfirm(request);
        CategoryDO category = request.getCategoryId() == null ? null : categoryMapper.selectById(request.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new ServerException(400, "请选择有效分类");
        }
        return executeByProduct(ids, false, spu -> {
            ProductSpuDO update = new ProductSpuDO();
            update.setId(spu.getId());
            update.setCategoryId(category.getId());
            productAdminService.updateSpu(update);
            return success(spu, "分类调整成功");
        });
    }

    public ProductBatchOperationRespVO updateSort(ProductBatchOperationReqVO request) {
        List<Long> ids = validateIdsAndConfirm(request);
        Integer sort = request.getSort();
        if (sort == null || sort < 0 || sort > 9999) {
            throw new ServerException(400, "排序值应为 0 至 9999");
        }
        return executeByProduct(ids, false, spu -> {
            int updated = productSpuMapper.update(null, new LambdaUpdateWrapper<ProductSpuDO>()
                    .eq(ProductSpuDO::getId, spu.getId())
                    .set(ProductSpuDO::getSort, sort));
            if (updated != 1) {
                throw new ServerException(409, "商品信息已变化，请刷新后重试");
            }
            return success(spu, "排序调整成功");
        });
    }

    public ProductBatchOperationRespVO previewPrice(ProductBatchOperationReqVO request) {
        List<Long> ids = validateIds(request);
        validatePriceRule(request);
        return executeByProduct(ids, true, spu -> priceResult(spu, request, true, 0L));
    }

    public ProductBatchOperationRespVO updatePrice(ProductBatchOperationReqVO request, Long adminId) {
        List<Long> ids = validateIdsAndConfirm(request);
        validatePriceRule(request);
        return executeByProduct(ids, false, spu -> priceResult(spu, request, false, adminId));
    }

    public ProductBatchOperationRespVO updateStock(ProductBatchOperationReqVO request, Long adminId) {
        List<Long> ids = validateIdsAndConfirm(request);
        Integer delta = request.getStockDelta();
        if (delta == null || delta == 0 || delta < -MAX_SKU_STOCK || delta > MAX_SKU_STOCK) {
            throw new ServerException(400, "库存调整数量应为 -1000000 至 1000000 且不能为 0");
        }
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.length() < 4 || reason.length() > 200) {
            throw new ServerException(400, "批量调库存原因长度应为 4 至 200 个字符");
        }
        return executeByProduct(ids, false, spu -> {
            List<ProductSkuDO> skus = listSkus(spu.getId());
            int beforeStock = skus.stream().mapToInt(sku -> sku.getStock() == null ? 0 : sku.getStock()).sum();
            List<ProductSkuDO> adjusted = new ArrayList<>();
            for (ProductSkuDO sku : skus) {
                int before = sku.getStock() == null ? 0 : sku.getStock();
                int after = before + delta;
                if (after < 0 || after > MAX_SKU_STOCK) {
                    throw new ServerException(400, "SKU " + displaySku(sku) + " 调整后库存超出范围");
                }
                ProductSkuDO copy = copySku(sku);
                copy.setStock(after);
                adjusted.add(copy);
            }
            productAdminService.saveSkus(spu.getId(), adjusted, adminId, reason);
            ProductBatchItemResultRespVO row = success(spu, "库存调整成功");
            row.setBeforeStock(beforeStock);
            row.setAfterStock(beforeStock + delta * skus.size());
            return row;
        });
    }

    private ProductBatchItemResultRespVO priceResult(ProductSpuDO spu, ProductBatchOperationReqVO request,
                                                     boolean dryRun, Long adminId) {
        List<ProductSkuDO> skus = listSkus(spu.getId());
        List<ProductSkuDO> adjusted = new ArrayList<>();
        int beforePrice = skus.stream().map(ProductSkuDO::getPrice).min(Integer::compareTo).orElse(spu.getPrice());
        int afterPrice = Integer.MAX_VALUE;
        for (ProductSkuDO sku : skus) {
            int price = sku.getPrice() == null ? 0 : sku.getPrice();
            int newPrice = calculatePrice(price, request);
            if (newPrice <= 0 || newPrice > MAX_PRICE_CENTS) {
                throw new ServerException(400, "SKU " + displaySku(sku) + " 调整后价格超出范围");
            }
            ProductSkuDO copy = copySku(sku);
            copy.setPrice(newPrice);
            if (copy.getMarketPrice() != null && copy.getMarketPrice() > 0 && copy.getMarketPrice() < newPrice) {
                copy.setMarketPrice(newPrice);
            }
            adjusted.add(copy);
            afterPrice = Math.min(afterPrice, newPrice);
        }
        if (!dryRun) {
            productAdminService.saveSkus(spu.getId(), adjusted, adminId, "批量调价");
        }
        ProductBatchItemResultRespVO row = success(spu, dryRun ? "预览成功" : "调价成功");
        row.setBeforePrice(beforePrice);
        row.setAfterPrice(afterPrice == Integer.MAX_VALUE ? beforePrice : afterPrice);
        return row;
    }

    private int calculatePrice(int currentPrice, ProductBatchOperationReqVO request) {
        BigDecimal value = request.getPriceAdjustValue();
        if ("FIXED_AMOUNT".equals(request.getPriceAdjustType())) {
            return BigDecimal.valueOf(currentPrice)
                    .add(value.movePointRight(2))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();
        }
        BigDecimal ratio = BigDecimal.ONE.add(value.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        return BigDecimal.valueOf(currentPrice).multiply(ratio).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private void validatePriceRule(ProductBatchOperationReqVO request) {
        String type = request.getPriceAdjustType();
        BigDecimal value = request.getPriceAdjustValue();
        if (!"FIXED_AMOUNT".equals(type) && !"PERCENT".equals(type)) {
            throw new ServerException(400, "调价方式不正确");
        }
        if (value == null || BigDecimal.ZERO.compareTo(value) == 0) {
            throw new ServerException(400, "调价值不能为空且不能为 0");
        }
        if ("FIXED_AMOUNT".equals(type) && value.abs().compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new ServerException(400, "固定调价值不能超过 1000000 元");
        }
        if ("PERCENT".equals(type)
                && (value.compareTo(BigDecimal.valueOf(-90)) < 0
                || value.compareTo(BigDecimal.valueOf(1000)) > 0)) {
            throw new ServerException(400, "百分比调价值应为 -90 至 1000");
        }
    }

    private ProductBatchOperationRespVO executeByProduct(List<Long> ids, boolean dryRun, ProductAction action) {
        ProductBatchOperationRespVO response = new ProductBatchOperationRespVO();
        response.setTotalCount(ids.size());
        response.setDryRun(dryRun);
        for (Long id : ids) {
            ProductSpuDO spu = productSpuMapper.selectById(id);
            if (spu == null) {
                response.getRows().add(failure(id, "", "商品不存在"));
                response.setFailureCount(response.getFailureCount() + 1);
                continue;
            }
            try {
                ProductBatchItemResultRespVO row = action.apply(spu);
                response.getRows().add(row);
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (ServerException exception) {
                response.getRows().add(failure(spu.getId(), spu.getName(), exception.getMessage()));
                response.setFailureCount(response.getFailureCount() + 1);
            } catch (Exception exception) {
                response.getRows().add(failure(spu.getId(), spu.getName(), "操作失败"));
                response.setFailureCount(response.getFailureCount() + 1);
            }
        }
        return response;
    }

    private List<Long> validateIdsAndConfirm(ProductBatchOperationReqVO request) {
        List<Long> ids = validateIds(request);
        if (request.getConfirmCount() == null || request.getConfirmCount() != ids.size()) {
            throw new ServerException(400, "确认数量与本次选择商品数不一致");
        }
        return ids;
    }

    private List<Long> validateIds(ProductBatchOperationReqVO request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new ServerException(400, "请选择要批量操作的商品");
        }
        if (request.getIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ServerException(400, "商品 ID 不正确");
        }
        List<Long> ids = request.getIds().stream().distinct().toList();
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new ServerException(400, "单次最多批量操作 100 个商品");
        }
        return ids;
    }

    private List<ProductSkuDO> listSkus(Long spuId) {
        List<ProductSkuDO> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getSpuId, spuId)
                .orderByAsc(ProductSkuDO::getId));
        if (skus.isEmpty()) {
            throw new ServerException(400, "商品没有可调整 SKU");
        }
        return skus;
    }

    private ProductSkuDO copySku(ProductSkuDO source) {
        ProductSkuDO copy = new ProductSkuDO();
        copy.setId(source.getId());
        copy.setSpuId(source.getSpuId());
        copy.setSkuCode(source.getSkuCode());
        copy.setProperties(source.getProperties());
        copy.setPrice(source.getPrice());
        copy.setMarketPrice(source.getMarketPrice());
        copy.setStock(source.getStock());
        copy.setPicUrl(source.getPicUrl());
        copy.setWeight(source.getWeight());
        copy.setVolume(source.getVolume());
        return copy;
    }

    private ProductBatchItemResultRespVO success(ProductSpuDO spu, String message) {
        ProductBatchItemResultRespVO row = new ProductBatchItemResultRespVO();
        row.setId(spu.getId());
        row.setName(spu.getName());
        row.setSuccess(true);
        row.setMessage(message);
        return row;
    }

    private ProductBatchItemResultRespVO failure(Long id, String name, String message) {
        ProductBatchItemResultRespVO row = new ProductBatchItemResultRespVO();
        row.setId(id);
        row.setName(name);
        row.setSuccess(false);
        row.setMessage(message);
        return row;
    }

    private String displaySku(ProductSkuDO sku) {
        return sku.getSkuCode() == null || sku.getSkuCode().isBlank()
                ? "#" + sku.getId() : sku.getSkuCode();
    }

    private interface ProductAction {
        ProductBatchItemResultRespVO apply(ProductSpuDO spu);
    }
}
