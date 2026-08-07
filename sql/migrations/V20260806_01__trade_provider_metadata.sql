DROP PROCEDURE IF EXISTS migration_add_after_sale_provider_metadata;

DELIMITER $$

CREATE PROCEDURE migration_add_after_sale_provider_metadata()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND column_name = 'refund_provider'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD COLUMN `refund_provider` varchar(32) DEFAULT '' COMMENT '退款提供方'
            AFTER `reject_reason`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND column_name = 'provider_refund_no'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD COLUMN `provider_refund_no` varchar(64) DEFAULT '' COMMENT '渠道退款单号'
            AFTER `refund_provider`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND column_name = 'refund_message'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD COLUMN `refund_message` varchar(255) DEFAULT '' COMMENT '退款渠道说明'
            AFTER `provider_refund_no`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND column_name = 'refund_time'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD COLUMN `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间'
            AFTER `audit_time`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'trade_after_sale'
           AND index_name = 'idx_status_create_time_id'
    ) THEN
        ALTER TABLE `trade_after_sale`
            ADD INDEX `idx_status_create_time_id` (`status`, `create_time`, `id`);
    END IF;
END$$

DELIMITER ;

CALL migration_add_after_sale_provider_metadata();
DROP PROCEDURE migration_add_after_sale_provider_metadata;
