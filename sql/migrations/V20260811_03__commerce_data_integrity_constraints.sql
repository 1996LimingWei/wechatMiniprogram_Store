SET NAMES utf8mb4;

UPDATE `product_spu` p
SET p.`stock` = (
    SELECT COALESCE(SUM(s.`stock`), 0)
    FROM `product_sku` s
    WHERE s.`spu_id` = p.`id` AND s.`deleted` = b'0'
)
WHERE p.`deleted` = b'0';

UPDATE `product_spu` p
SET p.`sales_count` = (
    SELECT COALESCE(SUM(oi.`count`), 0)
    FROM `trade_order_item` oi
    JOIN `trade_order` o ON o.`id` = oi.`order_id`
    WHERE oi.`spu_id` = p.`id`
      AND oi.`deleted` = b'0'
      AND o.`deleted` = b'0'
      AND o.`pay_status` = 1
)
WHERE p.`deleted` = b'0';

UPDATE `trade_order` o
JOIN `trade_after_sale` a
  ON a.`order_id` = o.`id`
 AND a.`status` = 1
 AND a.`deleted` = b'0'
SET o.`status` = 5,
    o.`pay_status` = 2
WHERE o.`deleted` = b'0'
  AND o.`pay_status` <> 2;

UPDATE `pay_order` p
JOIN `trade_after_sale` a
  ON a.`order_id` = p.`order_id`
 AND a.`status` = 1
 AND a.`deleted` = b'0'
SET p.`status` = 3
WHERE p.`deleted` = b'0'
  AND p.`status` <> 3;

DROP PROCEDURE IF EXISTS migration_add_check_constraint;
DROP PROCEDURE IF EXISTS migration_add_unique_index;
DROP PROCEDURE IF EXISTS migration_add_index;

DELIMITER $$

CREATE PROCEDURE migration_add_check_constraint(
    IN p_target_table VARCHAR(64),
    IN p_constraint_name VARCHAR(64),
    IN p_check_expression VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = p_target_table
          AND constraint_name = p_constraint_name
          AND constraint_type = 'CHECK'
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_target_table, '` ADD CONSTRAINT `',
                          p_constraint_name, '` CHECK (', p_check_expression, ')');
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$

CREATE PROCEDURE migration_add_unique_index(
    IN p_target_table VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_target_table
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_target_table, '` ADD UNIQUE INDEX `',
                          p_index_name, '` (', p_index_columns, ')');
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$

CREATE PROCEDURE migration_add_index(
    IN p_target_table VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_columns VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_target_table
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_target_table, '` ADD INDEX `',
                          p_index_name, '` (', p_index_columns, ')');
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$

DELIMITER ;

CALL migration_add_check_constraint('product_spu', 'chk_product_spu_amount_stock',
    '`price` > 0 AND `stock` >= 0 AND `sales_count` >= 0 AND (`market_price` IS NULL OR `market_price` = 0 OR `market_price` >= `price`)');
CALL migration_add_check_constraint('product_sku', 'chk_product_sku_amount_stock',
    '`price` > 0 AND `stock` >= 0 AND (`market_price` IS NULL OR `market_price` = 0 OR `market_price` >= `price`)');
CALL migration_add_check_constraint('trade_cart', 'chk_trade_cart_quantity_amount',
    '`count` BETWEEN 1 AND 99 AND `price` > 0 AND `checked` IN (0, 1)');
CALL migration_add_check_constraint('trade_order', 'chk_trade_order_amounts',
    '`goods_price` >= 0 AND `freight_price` >= 0 AND `coupon_price` >= 0 AND `coupon_price` <= `goods_price` + `freight_price` AND `order_price` = `goods_price` + `freight_price` - `coupon_price` AND `actual_price` = `order_price` AND `actual_price` > 0');
CALL migration_add_check_constraint('trade_order_item', 'chk_trade_order_item_amounts',
    '`price` > 0 AND `count` BETWEEN 1 AND 99 AND `total_price` = `price` * `count`');
CALL migration_add_check_constraint('pay_order', 'chk_pay_order_amount', '`amount` > 0');
CALL migration_add_check_constraint('trade_after_sale', 'chk_trade_after_sale_amount', '`refund_amount` > 0');

CALL migration_add_unique_index('trade_order_logistics', 'uk_order_id', '`order_id`');
CALL migration_add_index('trade_order_item', 'idx_spu_id', '`spu_id`');
CALL migration_add_index('trade_order_item', 'idx_sku_id', '`sku_id`');
CALL migration_add_index('trade_cart', 'idx_spu_id', '`spu_id`');

DROP PROCEDURE migration_add_check_constraint;
DROP PROCEDURE migration_add_unique_index;
DROP PROCEDURE migration_add_index;
