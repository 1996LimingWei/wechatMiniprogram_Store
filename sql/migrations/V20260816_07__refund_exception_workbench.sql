SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migration_add_column_if_missing;

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

DELIMITER ;

CALL migration_add_column_if_missing('trade_after_sale', 'refund_channel_state',
    "varchar(32) NOT NULL DEFAULT '' COMMENT '最近渠道退款状态' AFTER `refund_last_error`");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_exception_code',
    "varchar(64) NOT NULL DEFAULT '' COMMENT '退款异常编码' AFTER `refund_channel_state`");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_exception_message',
    "varchar(255) NOT NULL DEFAULT '' COMMENT '退款异常说明' AFTER `refund_exception_code`");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_handled',
    "tinyint NOT NULL DEFAULT 0 COMMENT '退款异常处理状态 0=未处理 1=已处理' AFTER `refund_exception_message`");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_handle_remark',
    "varchar(255) NOT NULL DEFAULT '' COMMENT '退款异常处理备注' AFTER `refund_handled`");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_handle_admin_id',
    "bigint DEFAULT NULL COMMENT '退款异常处理管理员ID' AFTER `refund_handle_remark`");
CALL migration_add_column_if_missing('trade_after_sale', 'refund_handle_time',
    "datetime DEFAULT NULL COMMENT '退款异常处理时间' AFTER `refund_handle_admin_id`");

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES
(40, 'trade:refund-read', '查看退款单与退款异常', '/admin-api/trade/refund/**', 'GET', 1),
(41, 'trade:refund-sync', '人工同步退款状态', '/admin-api/trade/refund/sync', 'POST', 1),
(42, 'trade:refund-retry', '人工重试退款', '/admin-api/trade/refund/retry', 'POST', 1),
(43, 'trade:refund-handle', '处理退款异常', '/admin-api/trade/refund/handle', 'POST', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
                        `http_method` = VALUES(`http_method`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN'
   AND p.code IN ('trade:refund-read', 'trade:refund-sync', 'trade:refund-retry', 'trade:refund-handle')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'FINANCE'
   AND p.code IN ('trade:refund-read', 'trade:refund-sync', 'trade:refund-retry', 'trade:refund-handle')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'AFTER_SALE_REVIEWER'
   AND p.code IN ('trade:refund-read', 'trade:refund-sync', 'trade:refund-retry')
   AND p.deleted = b'0';

DROP PROCEDURE migration_add_column_if_missing;
