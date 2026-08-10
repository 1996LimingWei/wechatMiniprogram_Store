package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductInventoryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;

    public ProductSnapshot getAvailableSnapshot(Long spuId, Long skuId) {
        if (spuId == null || skuId == null) {
            throw new ServerException(1101, "商品或规格不存在");
        }
        ProductSpuDO spu = productSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new ServerException(1101, "商品不存在");
        }
        if (spu.getStatus() == null || spu.getStatus() != 1) {
            throw new ServerException(1102, "商品已下架");
        }
        ProductSkuDO sku = productSkuMapper.selectById(skuId);
        if (sku == null || !spuId.equals(sku.getSpuId())) {
            throw new ServerException(1101, "商品规格不存在");
        }
        if (sku.getPrice() == null || sku.getPrice() <= 0) {
            throw new ServerException(1103, "商品规格价格异常");
        }
        if (sku.getStock() == null || sku.getStock() < 0) {
            throw new ServerException(1201, "商品规格库存异常");
        }
        return new ProductSnapshot(spu.getId(), sku.getId(), spu.getName(),
                sku.getPicUrl() == null || sku.getPicUrl().isBlank() ? spu.getPicUrl() : sku.getPicUrl(),
                formatSpecName(sku.getProperties()), sku.getPrice(), sku.getStock() == null ? 0 : sku.getStock());
    }

    public void reduceSkuStock(Long skuId, int count) {
        if (skuId == null || count <= 0) {
            throw new ServerException(1201, "商品库存不足");
        }
        int updated = productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getId, skuId)
                .ge(ProductSkuDO::getStock, count)
                .setSql("stock = stock - " + count));
        if (updated != 1) {
            throw new ServerException(1201, "商品库存不足");
        }
        ProductSkuDO sku = productSkuMapper.selectById(skuId);
        if (sku != null) {
            syncSpuStock(sku.getSpuId());
        }
    }

    public void recoverSkuStock(Long skuId, int count) {
        if (skuId == null || count <= 0) {
            return;
        }
        int updated = productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getId, skuId)
                .setSql("stock = stock + " + count));
        if (updated == 1) {
            ProductSkuDO sku = productSkuMapper.selectById(skuId);
            if (sku != null) {
                syncSpuStock(sku.getSpuId());
            }
        }
    }

    public void adjustSpuSales(Long spuId, int delta) {
        if (spuId == null || delta == 0) {
            return;
        }
        int updated = productSpuMapper.update(null, new LambdaUpdateWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getId, spuId)
                .setSql("sales_count = GREATEST(sales_count + (" + delta + "), 0)"));
        if (updated != 1) {
            throw new ServerException(1101, "商品不存在，销量更新失败");
        }
    }

    public void syncSpuStock(Long spuId) {
        if (spuId == null) {
            return;
        }
        productSpuMapper.update(null, new LambdaUpdateWrapper<ProductSpuDO>()
                .eq(ProductSpuDO::getId, spuId)
                .setSql("stock = (SELECT COALESCE(SUM(s.stock), 0) FROM product_sku s "
                        + "WHERE s.spu_id = " + spuId + " AND s.deleted = 0)"));
    }

    private String formatSpecName(String properties) {
        if (properties == null || properties.isBlank() || "[]".equals(properties.trim())) {
            return "默认规格";
        }
        try {
            List<Map<String, Object>> values = OBJECT_MAPPER.readValue(properties, new TypeReference<>() { });
            String formatted = values.stream()
                    .map(value -> String.valueOf(value.getOrDefault("name", "")).trim()
                            + "：" + String.valueOf(value.getOrDefault("valueName", "")).trim())
                    .filter(value -> !value.startsWith("：") && !value.endsWith("："))
                    .reduce((left, right) -> left + "；" + right)
                    .orElse("");
            return formatted.isBlank() ? "默认规格" : formatted;
        } catch (Exception exception) {
            throw new ServerException(1103, "商品规格属性格式异常");
        }
    }

    public record ProductSnapshot(Long spuId, Long skuId, String name, String picUrl,
                                  String specName, Integer price, Integer stock) {
    }
}
