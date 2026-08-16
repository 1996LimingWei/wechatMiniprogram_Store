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

CALL migration_add_column_if_missing('pay_order', 'wechat_trade_state',
    "varchar(32) NOT NULL DEFAULT '' COMMENT '最近微信查单状态' AFTER `last_query_time`");
CALL migration_add_column_if_missing('pay_order', 'wechat_amount',
    "int DEFAULT NULL COMMENT '最近微信查单金额(分)' AFTER `wechat_trade_state`");
CALL migration_add_column_if_missing('pay_order', 'sync_message',
    "varchar(255) NOT NULL DEFAULT '' COMMENT '最近查单同步说明' AFTER `wechat_amount`");
CALL migration_add_index_if_missing('pay_order', 'idx_pay_order_pay_sn_status_time',
    "(`pay_sn`, `status`, `create_time`, `id`)");

CREATE TABLE IF NOT EXISTS `pay_exception` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `pay_order_id` bigint DEFAULT NULL COMMENT '支付单ID',
    `pay_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '商户支付单号',
    `order_id` bigint DEFAULT NULL COMMENT '订单ID',
    `order_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '订单号',
    `user_id` bigint DEFAULT NULL COMMENT '会员用户ID',
    `reason_code` varchar(64) NOT NULL COMMENT '异常编码',
    `reason` varchar(255) NOT NULL COMMENT '异常原因',
    `wechat_trade_state` varchar(32) NOT NULL DEFAULT '' COMMENT '微信交易状态',
    `wechat_amount` int DEFAULT NULL COMMENT '微信返回金额(分)',
    `channel_trade_no` varchar(64) NOT NULL DEFAULT '' COMMENT '微信支付交易号',
    `local_status` tinyint DEFAULT NULL COMMENT '本地支付单状态',
    `order_pay_status` tinyint DEFAULT NULL COMMENT '本地订单支付状态',
    `handled` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0=待处理 1=已处理',
    `handle_result` varchar(32) NOT NULL DEFAULT '' COMMENT '处理结果 AUTO_FIXED/MANUAL_CONFIRMED',
    `handle_remark` varchar(255) NOT NULL DEFAULT '' COMMENT '处理备注',
    `handle_admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID',
    `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
    `last_detect_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近发现时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_pay_exception_status_time` (`handled`, `create_time`, `id`),
    KEY `idx_pay_exception_pay_order` (`pay_order_id`, `handled`),
    KEY `idx_pay_exception_reason` (`reason_code`, `handled`, `last_detect_time`),
    KEY `idx_pay_exception_order` (`order_sn`, `pay_sn`)
) ENGINE=InnoDB COMMENT='支付异常与处理记录表';

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES
(37, 'trade:payment-read', '查看支付单与支付异常', '/admin-api/trade/pay/**', 'GET', 1),
(38, 'trade:payment-sync', '人工同步支付状态', '/admin-api/trade/pay/order/sync', 'POST', 1),
(39, 'trade:payment-handle', '处理支付异常', '/admin-api/trade/pay/exception/handle', 'POST', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
                        `http_method` = VALUES(`http_method`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT INTO `sys_role` (`id`, `code`, `name`, `status`)
VALUES (5, 'FINANCE', '财务', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN'
   AND p.code IN ('trade:payment-read', 'trade:payment-sync', 'trade:payment-handle')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'FINANCE'
   AND p.code IN ('auth:session', 'trade:payment-read', 'trade:payment-sync', 'trade:payment-handle')
   AND p.deleted = b'0';

DROP PROCEDURE migration_add_column_if_missing;
DROP PROCEDURE migration_add_index_if_missing;
