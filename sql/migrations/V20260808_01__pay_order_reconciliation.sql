DROP PROCEDURE IF EXISTS migration_add_pay_order_reconciliation;

DELIMITER $$

CREATE PROCEDURE migration_add_pay_order_reconciliation()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'pay_order'
           AND column_name = 'last_query_time'
    ) THEN
        ALTER TABLE `pay_order`
            ADD COLUMN `last_query_time` datetime DEFAULT NULL COMMENT '最近主动查单时间'
            AFTER `pay_time`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'pay_order'
           AND index_name = 'idx_status_channel_query'
    ) THEN
        ALTER TABLE `pay_order`
            ADD INDEX `idx_status_channel_query` (`status`, `channel`, `last_query_time`, `id`);
    END IF;
END$$

DELIMITER ;

CALL migration_add_pay_order_reconciliation();
DROP PROCEDURE migration_add_pay_order_reconciliation;
