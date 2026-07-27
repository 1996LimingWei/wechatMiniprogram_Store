package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductInventoryService {

    private static final Pattern SPEC_NAME_PATTERN = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern SPEC_VALUE_PATTERN = Pattern.compile("\\\"valueName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

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
    }

    public void recoverSkuStock(Long skuId, int count) {
        if (skuId == null || count <= 0) {
            return;
        }
        productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getId, skuId)
                .setSql("stock = stock + " + count));
    }

    private String formatSpecName(String properties) {
        String name = findValue(SPEC_NAME_PATTERN, properties);
        String value = findValue(SPEC_VALUE_PATTERN, properties);
        if (name.isBlank() || value.isBlank()) {
            return "默认规格";
        }
        return name + "：" + value;
    }

    private String findValue(Pattern pattern, String source) {
        if (source == null) {
            return "";
        }
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    public record ProductSnapshot(Long spuId, Long skuId, String name, String picUrl,
                                  String specName, Integer price, Integer stock) {
    }
}
