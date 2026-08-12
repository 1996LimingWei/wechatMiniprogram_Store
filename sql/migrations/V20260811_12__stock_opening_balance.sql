-- 用当前库存扣除已有流水净变化，反推出迁移时的期初库存。
DROP PROCEDURE IF EXISTS migration_seed_stock_opening;

DELIMITER $$

CREATE PROCEDURE migration_seed_stock_opening()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM product_sku sku
        LEFT JOIN (
            SELECT sku_id, SUM(change_quantity) AS net_change
            FROM product_stock_log
            GROUP BY sku_id
        ) ledger ON ledger.sku_id = sku.id
        WHERE sku.deleted = b'0'
          AND sku.stock - COALESCE(ledger.net_change, 0) < 0
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '无法计算库存期初余额，请先核对库存流水';
    END IF;

    INSERT INTO product_stock_log
        (sku_id, spu_id, biz_type, biz_no, change_quantity,
         before_stock, after_stock, operator_type, operator_id, remark, create_time)
    SELECT sku.id, sku.spu_id, 'OPENING', 'ENTERPRISE-LEDGER-OPENING',
           sku.stock - COALESCE(ledger.net_change, 0),
           0, sku.stock - COALESCE(ledger.net_change, 0),
           'system', 0, '企业库存流水期初余额', '2000-01-01 00:00:00'
    FROM product_sku sku
    LEFT JOIN (
        SELECT sku_id, SUM(change_quantity) AS net_change
        FROM product_stock_log
        GROUP BY sku_id
    ) ledger ON ledger.sku_id = sku.id
    WHERE sku.deleted = b'0'
      AND sku.stock - COALESCE(ledger.net_change, 0) > 0
      AND NOT EXISTS (
          SELECT 1 FROM product_stock_log opening
          WHERE opening.sku_id = sku.id AND opening.biz_type = 'OPENING'
      );
END$$

DELIMITER ;

CALL migration_seed_stock_opening();
DROP PROCEDURE migration_seed_stock_opening;
