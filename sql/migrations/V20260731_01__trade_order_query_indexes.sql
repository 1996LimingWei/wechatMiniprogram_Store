DROP PROCEDURE IF EXISTS migration_add_trade_order_index_if_missing;

DELIMITER $$

CREATE PROCEDURE migration_add_trade_order_index_if_missing(
    IN index_name_arg VARCHAR(64),
    IN definition_arg TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'trade_order'
          AND index_name = index_name_arg
    ) THEN
        SET @migration_sql = CONCAT(
            'ALTER TABLE `trade_order` ADD INDEX `', index_name_arg, '` ', definition_arg
        );
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$

DELIMITER ;

CALL migration_add_trade_order_index_if_missing(
    'idx_create_time_id',
    '(`create_time`, `id`)'
);
CALL migration_add_trade_order_index_if_missing(
    'idx_user_create_time_id',
    '(`user_id`, `create_time`, `id`)'
);
CALL migration_add_trade_order_index_if_missing(
    'idx_mobile_create_time_id',
    '(`mobile`, `create_time`, `id`)'
);
CALL migration_add_trade_order_index_if_missing(
    'idx_status_pay_create_time_id',
    '(`status`, `pay_status`, `create_time`, `id`)'
);
CALL migration_add_trade_order_index_if_missing(
    'idx_pay_status_create_time_id',
    '(`pay_status`, `create_time`, `id`)'
);

DROP PROCEDURE migration_add_trade_order_index_if_missing;
