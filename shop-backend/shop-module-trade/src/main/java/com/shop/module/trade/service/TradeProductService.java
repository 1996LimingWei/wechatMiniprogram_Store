package com.shop.module.trade.service;

import com.shop.module.product.service.ProductSkuProvider;
import com.shop.module.product.service.ProductSkuSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeProductService {

    private final ProductSkuProvider productSkuProvider;
    private final JdbcTemplate jdbcTemplate;

    public TradeProductSnapshot getSnapshot(Long goodsId, Long productId) {
        ProductSkuSnapshot product = productSkuProvider.getSnapshot(goodsId, productId);
        TradeProductSnapshot snapshot = new TradeProductSnapshot();
        snapshot.setSpuId(product.getSpuId());
        snapshot.setSkuId(product.getSkuId());
        snapshot.setName(product.getName());
        snapshot.setPicUrl(product.getPicUrl());
        snapshot.setSpecName(product.getSpecName());
        snapshot.setPrice(product.getPrice());
        snapshot.setStock(product.getStock());
        return snapshot;
    }

    public void reduceStock(TradeProductSnapshot snapshot, int count) {
        productSkuProvider.reduceStock(snapshot.getSkuId(), count);
    }

    public void reduceStock(TradeProductSnapshot snapshot, int count, String bizType, String bizNo,
                            String operatorType, Long operatorId) {
        ensureStockLogAbsent(bizType, bizNo, snapshot.getSkuId());
        productSkuProvider.reduceStock(snapshot.getSkuId(), count);
        recordStockChange(snapshot.getSkuId(), -count, bizType, bizNo, operatorType, operatorId,
                "交易扣减库存");
    }

    public void recoverStock(Long skuId, int count) {
        productSkuProvider.recoverStock(skuId, count);
    }

    public void recoverStock(Long skuId, int count, String bizType, String bizNo,
                             String operatorType, Long operatorId) {
        ensureStockLogAbsent(bizType, bizNo, skuId);
        productSkuProvider.recoverStock(skuId, count);
        recordStockChange(skuId, count, bizType, bizNo, operatorType, operatorId, "交易回补库存");
    }

    public void adjustSales(Long spuId, int delta) {
        productSkuProvider.adjustSales(spuId, delta);
    }

    private void ensureStockLogAbsent(String bizType, String bizNo, Long skuId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM product_stock_log
                 WHERE biz_type = ? AND biz_no = ? AND sku_id = ?
                """, Integer.class, bizType, bizNo, skuId);
        if (count != null && count > 0) {
            throw new com.shop.common.exception.ServerException(409, "库存业务已处理，请勿重复提交");
        }
    }

    private void recordStockChange(Long skuId, int change, String bizType, String bizNo,
                                   String operatorType, Long operatorId, String remark) {
        MapRow row = jdbcTemplate.queryForObject(
                "SELECT spu_id, stock FROM product_sku WHERE id = ? AND deleted = b'0'",
                (rs, index) -> new MapRow(rs.getLong("spu_id"), rs.getInt("stock")), skuId);
        if (row == null) {
            throw new com.shop.common.exception.ServerException(1101, "商品规格不存在");
        }
        int beforeStock = Math.subtractExact(row.stock(), change);
        jdbcTemplate.update("""
                INSERT INTO product_stock_log
                    (sku_id, spu_id, biz_type, biz_no, change_quantity, before_stock,
                     after_stock, operator_type, operator_id, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, skuId, row.spuId(), bizType, bizNo, change, beforeStock, row.stock(),
                operatorType, operatorId == null ? 0L : operatorId, remark);
    }

    private record MapRow(Long spuId, Integer stock) {
    }
}
