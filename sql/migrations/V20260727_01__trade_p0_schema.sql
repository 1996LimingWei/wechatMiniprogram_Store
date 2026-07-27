DELIMITER $$

CREATE PROCEDURE migration_add_column_if_missing(
    IN table_name_arg VARCHAR(64),
    IN column_name_arg VARCHAR(64),
    IN definition_arg TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_arg
          AND column_name = column_name_arg
    ) THEN
        SET @migration_sql = CONCAT(
            'ALTER TABLE `', table_name_arg, '` ADD COLUMN `', column_name_arg, '` ', definition_arg
        );
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$

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
            'ALTER TABLE `', table_name_arg, '` ADD INDEX `', index_name_arg, '` ', definition_arg
        );
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$

DELIMITER ;

CALL migration_add_column_if_missing('trade_order', 'expire_time', 'datetime DEFAULT NULL COMMENT ''待付款超时关闭时间''');
CALL migration_add_column_if_missing('trade_order', 'close_time', 'datetime DEFAULT NULL COMMENT ''订单关闭时间''');
CALL migration_add_column_if_missing('trade_order', 'close_reason', 'varchar(128) DEFAULT '''' COMMENT ''订单关闭原因''');
CALL migration_add_index_if_missing('trade_order', 'idx_expire_status', '(status, pay_status, expire_time)');

ALTER TABLE `trade_order`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态 0=待付款 1=待发货 2=待收货 3=已完成 4=已取消 5=退款中';
ALTER TABLE `pay_order`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0=待支付 1=已支付 2=已关闭 3=已退款';

CREATE TABLE IF NOT EXISTS `trade_order_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `operator_type` varchar(32) NOT NULL COMMENT '操作人类型 user/system/admin',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人ID，系统操作为0',
    `action` varchar(64) NOT NULL COMMENT '操作动作',
    `from_status` tinyint DEFAULT NULL COMMENT '变更前订单状态',
    `to_status` tinyint DEFAULT NULL COMMENT '变更后订单状态',
    `from_pay_status` tinyint DEFAULT NULL COMMENT '变更前支付状态',
    `to_pay_status` tinyint DEFAULT NULL COMMENT '变更后支付状态',
    `remark` varchar(255) DEFAULT '' COMMENT '操作说明',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='交易订单操作日志表';

CREATE TABLE IF NOT EXISTS `trade_after_sale` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `after_sale_sn` varchar(32) NOT NULL COMMENT '售后单号',
    `type` tinyint NOT NULL DEFAULT 1 COMMENT '售后类型 1=仅退款 2=退货退款',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '售后状态 0=处理中 1=已退款 2=已拒绝 3=已撤销',
    `refund_amount` int NOT NULL DEFAULT 0 COMMENT '退款金额(分)',
    `reason` varchar(128) DEFAULT '' COMMENT '申请原因',
    `apply_remark` varchar(255) DEFAULT '' COMMENT '申请说明',
    `before_order_status` tinyint DEFAULT NULL COMMENT '申请售后前订单状态',
    `reject_reason` varchar(255) DEFAULT '' COMMENT '拒绝原因',
    `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
    `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
    `reject_time` datetime DEFAULT NULL COMMENT '拒绝时间',
    `cancel_time` datetime DEFAULT NULL COMMENT '撤销时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_sn` (`after_sale_sn`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='交易售后表';

DROP PROCEDURE migration_add_column_if_missing;
DROP PROCEDURE migration_add_index_if_missing;
