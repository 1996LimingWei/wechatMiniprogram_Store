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

CALL migration_add_column_if_missing('trade_order', 'admin_remark',
    "varchar(255) NOT NULL DEFAULT '' COMMENT '客服内部备注，小程序不可见' AFTER `finish_time`");

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES
(33, 'trade:order-export', '导出订单与批量发货', '/admin-api/trade/order/**', 'GET', 1),
(34, 'trade:order-batch-ship', '批量发货', '/admin-api/trade/order/batch-ship/**', 'POST', 1),
(35, 'trade:order-remark', '维护订单内部备注', '/admin-api/trade/order/remark', 'POST', 1),
(36, 'trade:order-export-sensitive', '导出完整收货信息', '/admin-api/trade/order/export', 'GET', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
                        `http_method` = VALUES(`http_method`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN'
   AND p.code IN ('trade:order-export', 'trade:order-batch-ship', 'trade:order-remark', 'trade:order-export-sensitive')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'ORDER_CUSTOMER_SERVICE'
   AND p.code IN ('trade:order-export', 'trade:order-batch-ship', 'trade:order-remark')
   AND p.deleted = b'0';

DROP PROCEDURE migration_add_column_if_missing;
