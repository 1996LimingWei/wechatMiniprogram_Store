DELETE cart
FROM `trade_cart` cart
LEFT JOIN `product_sku` sku
    ON sku.`id` = cart.`sku_id`
   AND sku.`spu_id` = cart.`spu_id`
   AND sku.`deleted` = b'0'
WHERE cart.`deleted` = b'1'
   OR sku.`id` IS NULL;

DROP PROCEDURE IF EXISTS migration_normalize_trade_cart_unique_index;
DROP PROCEDURE IF EXISTS migration_add_trade_order_request_id;
DROP PROCEDURE IF EXISTS migration_add_trade_order_request_index;

DELIMITER $$

CREATE PROCEDURE migration_normalize_trade_cart_unique_index()
BEGIN
    DECLARE current_columns VARCHAR(255);

    SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')
      INTO current_columns
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'trade_cart'
       AND index_name = 'uk_user_sku';

    IF current_columns IS NOT NULL AND current_columns <> 'user_id,sku_id' THEN
        ALTER TABLE `trade_cart` DROP INDEX `uk_user_sku`;
        SET current_columns = NULL;
    END IF;

    IF current_columns IS NULL THEN
        ALTER TABLE `trade_cart`
            ADD UNIQUE INDEX `uk_user_sku` (`user_id`, `sku_id`);
    END IF;
END$$

CREATE PROCEDURE migration_add_trade_order_request_id()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_order'
           AND column_name = 'request_id'
    ) THEN
        ALTER TABLE `trade_order`
            ADD COLUMN `request_id` varchar(64) DEFAULT NULL COMMENT '客户端下单幂等标识'
            AFTER `order_sn`;
    END IF;
END$$

CREATE PROCEDURE migration_add_trade_order_request_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_order'
           AND index_name = 'uk_user_request_id'
    ) THEN
        ALTER TABLE `trade_order`
            ADD UNIQUE INDEX `uk_user_request_id` (`user_id`, `request_id`);
    END IF;
END$$

DELIMITER ;

CALL migration_normalize_trade_cart_unique_index();
CALL migration_add_trade_order_request_id();
CALL migration_add_trade_order_request_index();

DROP PROCEDURE migration_normalize_trade_cart_unique_index;
DROP PROCEDURE migration_add_trade_order_request_id;
DROP PROCEDURE migration_add_trade_order_request_index;
