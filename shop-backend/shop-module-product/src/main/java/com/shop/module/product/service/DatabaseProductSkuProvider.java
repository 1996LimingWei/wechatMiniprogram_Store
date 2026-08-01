package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 数据库 SKU 实现，切换 `product.provider=database` 后生效。 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "product", name = "provider", havingValue = "database")
public class DatabaseProductSkuProvider implements ProductSkuProvider {

    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public ProductSkuSnapshot getSnapshot(Long spuId, Long requestedSkuId) {
        ProductSpuDO spu = spuId == null ? null : productSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new ServerException(1101, "商品不存在");
        }
        if (!Integer.valueOf(1).equals(spu.getStatus())) {
            throw new ServerException(1102, "商品已下架");
        }
        ProductSkuDO sku = findSku(spuId, requestedSkuId);
        if (sku == null) {
            throw new ServerException(1101, "商品规格不存在");
        }
        ProductSkuSnapshot snapshot = new ProductSkuSnapshot();
        snapshot.setSpuId(spu.getId());
        snapshot.setSkuId(sku.getId());
        snapshot.setName(spu.getName());
        snapshot.setPicUrl(sku.getPicUrl() == null || sku.getPicUrl().isBlank() ? spu.getPicUrl() : sku.getPicUrl());
        snapshot.setSpecName(sku.getProperties() == null || sku.getProperties().isBlank() ? "默认规格" : sku.getProperties());
        snapshot.setPrice(sku.getPrice() == null ? spu.getPrice() : sku.getPrice());
        snapshot.setStock(sku.getStock() == null ? 0 : sku.getStock());
        return snapshot;
    }

    @Override
    public void reduceStock(Long skuId, int count) {
        int updated = productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getId, skuId)
                .ge(ProductSkuDO::getStock, count)
                .setSql("stock = stock - " + count));
        if (updated != 1) {
            throw new ServerException(1201, "商品库存不足");
        }
    }

    @Override
    public void recoverStock(Long skuId, int count) {
        if (skuId != null && count > 0) {
            productSkuMapper.update(null, new LambdaUpdateWrapper<ProductSkuDO>()
                    .eq(ProductSkuDO::getId, skuId)
                    .setSql("stock = stock + " + count));
        }
    }

    private ProductSkuDO findSku(Long spuId, Long requestedSkuId) {
        LambdaQueryWrapper<ProductSkuDO> wrapper = new LambdaQueryWrapper<ProductSkuDO>().eq(ProductSkuDO::getSpuId, spuId);
        if (requestedSkuId != null && requestedSkuId > 0 && !requestedSkuId.equals(spuId)) {
            wrapper.eq(ProductSkuDO::getId, requestedSkuId);
        } else {
            wrapper.orderByAsc(ProductSkuDO::getId).last("LIMIT 1");
        }
        return productSkuMapper.selectOne(wrapper);
    }
}
