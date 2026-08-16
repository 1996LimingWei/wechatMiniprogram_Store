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
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
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
    IN p_definition VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` ', p_definition);
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$

DELIMITER ;

CALL migration_add_column_if_missing('marketing_shipping_rule', 'start_time',
    "datetime DEFAULT NULL COMMENT '生效时间，空表示立即生效' AFTER `status`");
CALL migration_add_column_if_missing('marketing_shipping_rule', 'end_time',
    "datetime DEFAULT NULL COMMENT '停用时间，空表示长期有效' AFTER `start_time`");
CALL migration_add_index_if_missing('marketing_shipping_rule', 'idx_shipping_active_window',
    "(`status`, `start_time`, `end_time`, `id`)");

DROP PROCEDURE migration_add_column_if_missing;
DROP PROCEDURE migration_add_index_if_missing;
