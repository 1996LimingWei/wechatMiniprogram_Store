SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migration_add_column_if_missing;
DROP PROCEDURE IF EXISTS migration_add_index_if_missing;

DELIMITER $$

CREATE PROCEDURE migration_add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$

CREATE PROCEDURE migration_add_index_if_missing(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')');
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$

DELIMITER ;

CALL migration_add_column_if_missing('trade_order', 'finish_time',
    "datetime DEFAULT NULL COMMENT '订单完成时间' AFTER close_reason");

UPDATE trade_order
   SET finish_time = update_time
 WHERE status = 3 AND finish_time IS NULL;

CALL migration_add_column_if_missing('trade_after_sale', 'refund_attempt_count',
    "int NOT NULL DEFAULT 0 COMMENT '退款渠道调用次数' AFTER refund_message");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_last_attempt_time',
    "datetime DEFAULT NULL COMMENT '退款最近调用时间' AFTER refund_attempt_count");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_next_attempt_time',
    "datetime DEFAULT NULL COMMENT '退款下次调用时间' AFTER refund_last_attempt_time");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_claim_until',
    "datetime DEFAULT NULL COMMENT '退款任务占用截止时间' AFTER refund_next_attempt_time");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_last_error',
    "varchar(255) NOT NULL DEFAULT '' COMMENT '退款最近调用错误' AFTER refund_claim_until");
CALL migration_add_index_if_missing('trade_after_sale', 'idx_refund_retry',
    '`status`, `refund_next_attempt_time`, `refund_claim_until`, `refund_attempt_count`, `id`');

UPDATE trade_after_sale
   SET refund_next_attempt_time = COALESCE(update_time, NOW())
 WHERE status = 4 AND refund_next_attempt_time IS NULL;

DROP PROCEDURE migration_add_column_if_missing;
DROP PROCEDURE migration_add_index_if_missing;
