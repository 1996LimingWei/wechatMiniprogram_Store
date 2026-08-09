DROP PROCEDURE IF EXISTS migration_allow_repeated_after_sale;

DELIMITER $$

CREATE PROCEDURE migration_allow_repeated_after_sale()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND index_name = 'uk_order_id'
    ) THEN
        ALTER TABLE `trade_after_sale` DROP INDEX `uk_order_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND index_name = 'idx_order_id'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD INDEX `idx_order_id` (`order_id`);
    END IF;
END$$

DELIMITER ;

CALL migration_allow_repeated_after_sale();
DROP PROCEDURE migration_allow_repeated_after_sale;
