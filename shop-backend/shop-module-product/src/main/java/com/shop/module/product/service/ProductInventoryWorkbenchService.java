package com.shop.module.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.vo.InventoryReconcileRespVO;
import com.shop.module.product.vo.InventorySkuRespVO;
import com.shop.module.product.vo.InventoryStockAdjustReqVO;
import com.shop.module.product.vo.InventoryStockLogRespVO;
import com.shop.module.product.vo.InventoryWarningStockReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductInventoryWorkbenchService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_SKU_STOCK = 1_000_000;

    private final JdbcTemplate jdbcTemplate;
    private final ProductInventoryService productInventoryService;

    public PageResult<InventorySkuRespVO> page(PageParam pageParam, String productName, String skuCode,
                                               String stockStatus, Boolean lowStockOnly) {
        QueryParts query = inventoryWhere(productName, skuCode, stockStatus, lowStockOnly);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + query.fromAndWhere(), Long.class,
                query.args().toArray());
        List<Object> args = new ArrayList<>(query.args());
        args.add((pageParam.getPageNo() - 1) * pageParam.getPageSize());
        args.add(pageParam.getPageSize());
        List<InventorySkuRespVO> rows = jdbcTemplate.query("""
                SELECT s.id sku_id, s.spu_id, s.sku_code, s.properties, s.price, s.stock,
                       s.warning_stock, s.pic_url sku_pic_url, s.create_time,
                       p.name product_name, p.pic_url spu_pic_url, p.category_id,
                       c.name category_name
                """ + query.fromAndWhere() + """
                ORDER BY
                  CASE
                    WHEN s.stock = 0 THEN 0
                    WHEN s.warning_stock > 0 AND s.stock <= s.warning_stock THEN 1
                    ELSE 2
                  END,
                  s.stock ASC, s.id DESC
                LIMIT ?, ?
                """, (rs, index) -> mapInventorySku(rs), args.toArray());
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateWarningStock(InventoryWarningStockReqVO request) {
        if (request == null || request.getSkuId() == null) {
            throw new ServerException(400, "SKU ID 不能为空");
        }
        Integer warningStock = request.getWarningStock();
        if (warningStock == null || warningStock < 0 || warningStock > MAX_SKU_STOCK) {
            throw new ServerException(400, "预警库存应为 0 至 1000000");
        }
        int updated = jdbcTemplate.update("""
                UPDATE product_sku
                   SET warning_stock = ?, update_time = NOW()
                 WHERE id = ? AND deleted = b'0'
                """, warningStock, request.getSkuId());
        if (updated != 1) {
            throw new ServerException(1101, "商品规格不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public InventorySkuRespVO adjustStock(InventoryStockAdjustReqVO request, Long adminId) {
        if (request == null || request.getSkuId() == null) {
            throw new ServerException(400, "SKU ID 不能为空");
        }
        Integer change = request.getChangeQuantity();
        if (change == null || change == 0 || change < -MAX_SKU_STOCK || change > MAX_SKU_STOCK) {
            throw new ServerException(400, "库存调整数量应为 -1000000 至 1000000 且不能为 0");
        }
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.length() < 4 || reason.length() > 200) {
            throw new ServerException(400, "人工调整库存原因长度应为 4 至 200 个字符");
        }
        StockRow row = jdbcTemplate.queryForObject("""
                SELECT s.id, s.spu_id, s.stock
                  FROM product_sku s
                 WHERE s.id = ? AND s.deleted = b'0'
                """, (rs, index) -> new StockRow(rs.getLong("id"), rs.getLong("spu_id"), rs.getInt("stock")),
                request.getSkuId());
        if (row == null) {
            throw new ServerException(1101, "商品规格不存在");
        }
        int afterStock = row.stock() + change;
        if (afterStock < 0 || afterStock > MAX_SKU_STOCK) {
            throw new ServerException(400, "调整后库存应为 0 至 1000000");
        }
        int updated = jdbcTemplate.update("""
                UPDATE product_sku
                   SET stock = ?, update_time = NOW()
                 WHERE id = ? AND stock = ? AND deleted = b'0'
                """, afterStock, row.skuId(), row.stock());
        if (updated != 1) {
            throw new ServerException(409, "库存已变化，请刷新后重试");
        }
        jdbcTemplate.update("""
                INSERT INTO product_stock_log
                    (sku_id, spu_id, biz_type, biz_no, change_quantity, before_stock,
                     after_stock, operator_type, operator_id, remark)
                VALUES (?, ?, 'ADMIN_ADJUST', ?, ?, ?, ?, 'admin', ?, ?)
                """, row.skuId(), row.spuId(), "MANUAL-" + UUID.randomUUID().toString().replace("-", ""),
                change, row.stock(), afterStock, adminId == null ? 0L : adminId, reason);
        productInventoryService.syncSpuStock(row.spuId());
        return getSku(row.skuId());
    }

    public PageResult<InventoryStockLogRespVO> logPage(PageParam pageParam, Long skuId, Long spuId, String bizNo) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
                FROM product_stock_log l
                LEFT JOIN product_sku s ON s.id = l.sku_id
                LEFT JOIN product_spu p ON p.id = l.spu_id
                WHERE 1 = 1
                """);
        if (skuId != null) {
            where.append(" AND l.sku_id = ?");
            args.add(skuId);
        }
        if (spuId != null) {
            where.append(" AND l.spu_id = ?");
            args.add(spuId);
        }
        if (bizNo != null && !bizNo.isBlank()) {
            where.append(" AND l.biz_no LIKE ?");
            args.add("%" + bizNo.trim() + "%");
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add((pageParam.getPageNo() - 1) * pageParam.getPageSize());
        pageArgs.add(pageParam.getPageSize());
        List<InventoryStockLogRespVO> rows = jdbcTemplate.query("""
                SELECT l.id, l.sku_id, l.spu_id, s.sku_code, p.name product_name,
                       l.biz_type, l.biz_no, l.change_quantity, l.before_stock, l.after_stock,
                       l.operator_type, l.operator_id, l.remark, l.create_time
                """ + where + """
                ORDER BY l.create_time DESC, l.id DESC
                LIMIT ?, ?
                """, (rs, index) -> {
            InventoryStockLogRespVO row = new InventoryStockLogRespVO();
            row.setId(rs.getLong("id"));
            row.setSkuId(rs.getLong("sku_id"));
            row.setSpuId(rs.getLong("spu_id"));
            row.setSkuCode(rs.getString("sku_code"));
            row.setProductName(rs.getString("product_name"));
            row.setBizType(rs.getString("biz_type"));
            row.setBizNo(rs.getString("biz_no"));
            row.setChangeQuantity(rs.getInt("change_quantity"));
            row.setBeforeStock(rs.getInt("before_stock"));
            row.setAfterStock(rs.getInt("after_stock"));
            row.setOperatorType(rs.getString("operator_type"));
            row.setOperatorId(rs.getLong("operator_id"));
            row.setRemark(rs.getString("remark"));
            row.setCreateTime(DATE_TIME_FORMATTER.format(rs.getTimestamp("create_time").toLocalDateTime()));
            return row;
        }, pageArgs.toArray());
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    public InventoryReconcileRespVO reconcile() {
        InventoryReconcileRespVO response = new InventoryReconcileRespVO();
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM product_sku WHERE deleted = b'0'
                """, Integer.class);
        response.setTotalSkuCount(total == null ? 0 : total);
        List<InventoryReconcileRespVO.Row> rows = jdbcTemplate.query("""
                SELECT s.id sku_id, s.spu_id, s.sku_code, p.name product_name, s.stock current_stock,
                       COALESCE(l.ledger_stock, 0) ledger_stock,
                       s.stock - COALESCE(l.ledger_stock, 0) difference_value
                  FROM product_sku s
                  JOIN product_spu p ON p.id = s.spu_id AND p.deleted = b'0'
                  LEFT JOIN (
                    SELECT sku_id, SUM(change_quantity) ledger_stock
                      FROM product_stock_log
                     GROUP BY sku_id
                  ) l ON l.sku_id = s.id
                 WHERE s.deleted = b'0'
                   AND s.stock <> COALESCE(l.ledger_stock, 0)
                 ORDER BY ABS(s.stock - COALESCE(l.ledger_stock, 0)) DESC, s.id DESC
                 LIMIT 200
                """, (rs, index) -> {
            InventoryReconcileRespVO.Row row = new InventoryReconcileRespVO.Row();
            row.setSkuId(rs.getLong("sku_id"));
            row.setSpuId(rs.getLong("spu_id"));
            row.setSkuCode(rs.getString("sku_code"));
            row.setProductName(rs.getString("product_name"));
            row.setCurrentStock(rs.getInt("current_stock"));
            row.setLedgerStock(rs.getInt("ledger_stock"));
            row.setDifference(rs.getInt("difference_value"));
            return row;
        });
        response.setRows(rows);
        response.setMismatchCount(rows.size());
        return response;
    }

    private QueryParts inventoryWhere(String productName, String skuCode, String stockStatus, Boolean lowStockOnly) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                FROM product_sku s
                JOIN product_spu p ON p.id = s.spu_id AND p.deleted = b'0'
                LEFT JOIN product_category c ON c.id = p.category_id AND c.deleted = b'0'
                WHERE s.deleted = b'0'
                """);
        if (productName != null && !productName.isBlank()) {
            sql.append(" AND p.name LIKE ?");
            args.add("%" + productName.trim() + "%");
        }
        if (skuCode != null && !skuCode.isBlank()) {
            sql.append(" AND (s.sku_code LIKE ? OR CAST(s.id AS CHAR) = ?)");
            args.add("%" + skuCode.trim() + "%");
            args.add(skuCode.trim());
        }
        boolean lowOnly = Boolean.TRUE.equals(lowStockOnly) || "LOW_STOCK".equals(stockStatus);
        if ("OUT_OF_STOCK".equals(stockStatus)) {
            sql.append(" AND s.stock = 0");
        } else if (lowOnly) {
            sql.append(" AND s.stock > 0 AND s.warning_stock > 0 AND s.stock <= s.warning_stock");
        } else if ("NORMAL".equals(stockStatus)) {
            sql.append(" AND s.stock > 0 AND (s.warning_stock = 0 OR s.stock > s.warning_stock)");
        }
        return new QueryParts(sql.toString(), args);
    }

    private void applyStockStatus(InventorySkuRespVO row) {
        if (row.getStock() == null || row.getStock() == 0) {
            row.setStockStatus("OUT_OF_STOCK");
            row.setStockStatusName("缺货");
        } else if (row.getWarningStock() != null && row.getWarningStock() > 0
                && row.getStock() <= row.getWarningStock()) {
            row.setStockStatus("LOW_STOCK");
            row.setStockStatusName("低库存");
        } else {
            row.setStockStatus("NORMAL");
            row.setStockStatusName("正常");
        }
    }

    private String formatSpecName(String properties) {
        if (properties == null || properties.isBlank() || "[]".equals(properties.trim())) {
            return "默认规格";
        }
        try {
            List<?> values = OBJECT_MAPPER.readValue(properties, List.class);
            List<String> parts = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof Map<?, ?> map) {
                    Object rawName = map.get("name");
                    Object rawValueName = map.get("valueName");
                    String name = rawName == null ? "" : String.valueOf(rawName).trim();
                    String valueName = rawValueName == null ? "" : String.valueOf(rawValueName).trim();
                    if (!name.isBlank() && !valueName.isBlank()) {
                        parts.add(name + "：" + valueName);
                    }
                }
            }
            return parts.isEmpty() ? "默认规格" : String.join("；", parts);
        } catch (Exception exception) {
            return "默认规格";
        }
    }

    private InventorySkuRespVO getSku(Long skuId) {
        return jdbcTemplate.queryForObject("""
                SELECT s.id sku_id, s.spu_id, s.sku_code, s.properties, s.price, s.stock,
                       s.warning_stock, s.pic_url sku_pic_url, s.create_time,
                       p.name product_name, p.pic_url spu_pic_url, p.category_id,
                       c.name category_name
                  FROM product_sku s
                  JOIN product_spu p ON p.id = s.spu_id AND p.deleted = b'0'
                  LEFT JOIN product_category c ON c.id = p.category_id AND c.deleted = b'0'
                 WHERE s.id = ? AND s.deleted = b'0'
                """, (rs, index) -> mapInventorySku(rs), skuId);
    }

    private InventorySkuRespVO mapInventorySku(ResultSet rs) throws SQLException {
        InventorySkuRespVO row = new InventorySkuRespVO();
        row.setSkuId(rs.getLong("sku_id"));
        row.setSpuId(rs.getLong("spu_id"));
        row.setSkuCode(rs.getString("sku_code"));
        row.setProductName(rs.getString("product_name"));
        row.setSpecName(formatSpecName(rs.getString("properties")));
        row.setCategoryId(rs.getLong("category_id"));
        row.setCategoryName(rs.getString("category_name"));
        row.setPicUrl(firstNonBlank(rs.getString("sku_pic_url"), rs.getString("spu_pic_url")));
        row.setPrice(rs.getInt("price"));
        row.setStock(rs.getInt("stock"));
        row.setAvailableStock(rs.getInt("stock"));
        row.setLockedStock(0);
        row.setWarningStock(rs.getInt("warning_stock"));
        applyStockStatus(row);
        row.setCreateTime(DATE_TIME_FORMATTER.format(rs.getTimestamp("create_time").toLocalDateTime()));
        return row;
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private record QueryParts(String fromAndWhere, List<Object> args) {
    }

    private record StockRow(Long skuId, Long spuId, Integer stock) {
    }
}
