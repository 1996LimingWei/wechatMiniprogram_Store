DELIMITER $$

CREATE PROCEDURE migration_add_index_if_missing(
    IN table_name_arg VARCHAR(64),
    IN index_name_arg VARCHAR(64),
    IN definition_arg TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_arg
          AND index_name = index_name_arg
    ) THEN
        SET @migration_sql = CONCAT(
            'ALTER TABLE `', table_name_arg, '` ADD UNIQUE INDEX `', index_name_arg, '` ', definition_arg
        );
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$

DELIMITER ;

CALL migration_add_index_if_missing('pay_order', 'uk_order_id', '(order_id)');

ALTER TABLE `pay_order`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0=待支付 1=已支付 2=已关闭 3=已退款';

DROP PROCEDURE migration_add_index_if_missing;
