DROP PROCEDURE IF EXISTS migration_add_after_sale_order_unique;

DELIMITER $$

CREATE PROCEDURE migration_add_after_sale_order_unique()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND index_name = 'idx_order_id'
    ) THEN
        ALTER TABLE `trade_after_sale` DROP INDEX `idx_order_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND index_name = 'uk_order_id'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD UNIQUE INDEX `uk_order_id` (`order_id`);
    END IF;
END$$

DELIMITER ;

CALL migration_add_after_sale_order_unique();
DROP PROCEDURE migration_add_after_sale_order_unique;
