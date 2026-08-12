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

CALL migration_add_column_if_missing('trade_after_sale', 'return_company', "varchar(64) NOT NULL DEFAULT '' COMMENT '退货物流公司'");
CALL migration_add_column_if_missing('trade_after_sale', 'return_no', "varchar(64) NOT NULL DEFAULT '' COMMENT '退货物流单号'");
CALL migration_add_column_if_missing('trade_after_sale', 'return_deadline', "datetime DEFAULT NULL COMMENT '买家最晚寄回时间'");
CALL migration_add_column_if_missing('trade_after_sale', 'return_time', "datetime DEFAULT NULL COMMENT '买家寄回时间'");
CALL migration_add_column_if_missing('trade_after_sale', 'receive_time', "datetime DEFAULT NULL COMMENT '商家确认收货时间'");
CALL migration_add_column_if_missing('trade_after_sale', 'receive_remark', "varchar(255) NOT NULL DEFAULT '' COMMENT '收货质检说明'");
CALL migration_add_column_if_missing('trade_after_sale', 'stock_recovered', "tinyint NOT NULL DEFAULT 0 COMMENT '退货库存是否已回补 1=是 0=否'");
CALL migration_add_column_if_missing('trade_order', 'refunded_amount', "int NOT NULL DEFAULT 0 COMMENT '累计已退款金额(分)'");
CALL migration_add_column_if_missing('pay_order', 'refunded_amount', "int NOT NULL DEFAULT 0 COMMENT '累计已退款金额(分)'");

CALL migration_add_column_if_missing('sys_admin_user', 'failed_login_count', "int NOT NULL DEFAULT 0 COMMENT '连续登录失败次数'");
CALL migration_add_column_if_missing('sys_admin_user', 'locked_until', "datetime DEFAULT NULL COMMENT '登录锁定截止时间'");
CALL migration_add_column_if_missing('sys_admin_user', 'last_login_time', "datetime DEFAULT NULL COMMENT '最后登录时间'");
CALL migration_add_column_if_missing('sys_admin_user', 'last_login_ip', "varchar(64) NOT NULL DEFAULT '' COMMENT '最后登录IP'");

DROP PROCEDURE migration_add_column_if_missing;

UPDATE `trade_order`
SET `refunded_amount` = `actual_price`
WHERE `pay_status` = 2 AND `refunded_amount` = 0;

UPDATE `pay_order`
SET `refunded_amount` = `amount`
WHERE `status` = 3 AND `refunded_amount` = 0;

CREATE TABLE IF NOT EXISTS `trade_after_sale_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `after_sale_id` bigint NOT NULL COMMENT '售后单ID',
    `order_item_id` bigint NOT NULL COMMENT '订单明细ID',
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
    `goods_name` varchar(128) NOT NULL COMMENT '商品名称快照',
    `spec_name` varchar(128) NOT NULL DEFAULT '' COMMENT '规格快照',
    `price` int NOT NULL COMMENT '成交单价(分)',
    `apply_count` int NOT NULL COMMENT '申请售后数量',
    `refund_amount` int NOT NULL COMMENT '本明细退款金额(分)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_order_item` (`after_sale_id`, `order_item_id`),
    KEY `idx_order_item_id` (`order_item_id`),
    KEY `idx_sku_id` (`sku_id`),
    CONSTRAINT `chk_after_sale_item_amount` CHECK (`price` > 0 AND `apply_count` > 0 AND `refund_amount` = `price` * `apply_count`)
) ENGINE=InnoDB COMMENT='售后商品明细表';

CREATE TABLE IF NOT EXISTS `product_stock_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `sku_id` bigint NOT NULL COMMENT 'SKU ID',
    `spu_id` bigint NOT NULL COMMENT 'SPU ID',
    `biz_type` varchar(32) NOT NULL COMMENT '业务类型 ORDER/ORDER_CANCEL/AFTER_SALE/ADMIN_ADJUST',
    `biz_no` varchar(64) NOT NULL COMMENT '业务幂等单号',
    `change_quantity` int NOT NULL COMMENT '库存变化量，扣减为负数',
    `before_stock` int NOT NULL COMMENT '变化前库存',
    `after_stock` int NOT NULL COMMENT '变化后库存',
    `operator_type` varchar(16) NOT NULL DEFAULT 'system' COMMENT '操作人类型',
    `operator_id` bigint NOT NULL DEFAULT 0 COMMENT '操作人ID',
    `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '说明',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_biz_sku` (`biz_type`, `biz_no`, `sku_id`),
    KEY `idx_stock_sku_time` (`sku_id`, `create_time`, `id`),
    CONSTRAINT `chk_stock_log_balance` CHECK (`change_quantity` <> 0 AND `before_stock` >= 0 AND `after_stock` >= 0 AND `after_stock` = `before_stock` + `change_quantity`)
) ENGINE=InnoDB COMMENT='商品库存流水表';

INSERT IGNORE INTO `trade_after_sale_item`
    (`after_sale_id`, `order_item_id`, `spu_id`, `sku_id`, `goods_name`, `spec_name`,
     `price`, `apply_count`, `refund_amount`)
SELECT a.`id`, oi.`id`, oi.`spu_id`, oi.`sku_id`, oi.`goods_name`, oi.`spec_name`,
       oi.`price`, oi.`count`, oi.`total_price`
  FROM `trade_after_sale` a
  JOIN `trade_order_item` oi ON oi.`order_id` = a.`order_id` AND oi.`deleted` = b'0'
 WHERE a.`deleted` = b'0';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(64) NOT NULL,
    `name` varchar(64) NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `code` varchar(96) NOT NULL,
    `name` varchar(96) NOT NULL,
    `path_pattern` varchar(255) NOT NULL,
    `status` tinyint NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`code`)
) ENGINE=InnoDB COMMENT='系统权限表';

CREATE TABLE IF NOT EXISTS `sys_admin_user_role` (
    `admin_user_id` bigint NOT NULL,
    `role_id` bigint NOT NULL,
    PRIMARY KEY (`admin_user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB COMMENT='管理员角色关系表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `role_id` bigint NOT NULL,
    `permission_id` bigint NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB COMMENT='角色权限关系表';

CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `admin_user_id` bigint DEFAULT NULL,
    `username` varchar(64) NOT NULL,
    `success` tinyint NOT NULL,
    `ip` varchar(64) NOT NULL DEFAULT '',
    `user_agent` varchar(512) NOT NULL DEFAULT '',
    `message` varchar(255) NOT NULL DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_login_username_time` (`username`, `create_time`, `id`)
) ENGINE=InnoDB COMMENT='管理员登录日志表';

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `admin_user_id` bigint NOT NULL,
    `method` varchar(16) NOT NULL,
    `request_uri` varchar(255) NOT NULL,
    `success` tinyint NOT NULL,
    `ip` varchar(64) NOT NULL DEFAULT '',
    `duration_ms` bigint NOT NULL DEFAULT 0,
    `message` varchar(255) NOT NULL DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_operation_user_time` (`admin_user_id`, `create_time`, `id`),
    KEY `idx_operation_uri_time` (`request_uri`, `create_time`, `id`)
) ENGINE=InnoDB COMMENT='管理员操作日志表';

CREATE TABLE IF NOT EXISTS `sys_job_lock` (
    `lock_name` varchar(96) NOT NULL,
    `lock_owner` varchar(96) NOT NULL DEFAULT '',
    `locked_until` datetime NOT NULL,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB COMMENT='分布式定时任务锁表';

INSERT INTO `sys_role` (`id`, `code`, `name`, `status`)
VALUES (1, 'SUPER_ADMIN', '超级管理员', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 1, `deleted` = b'0';

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `status`) VALUES
(1, 'dashboard:view', '查看经营看板', '/admin-api/dashboard/**', 1),
(2, 'product:manage', '管理商品与分类', '/admin-api/product/**', 1),
(3, 'content:manage', '管理运营内容', '/admin-api/content/**', 1),
(4, 'trade:manage', '管理订单与售后', '/admin-api/trade/**', 1),
(5, 'system:manage', '管理系统配置', '/admin-api/system/**', 1),
(6, 'member:manage', '管理会员', '/admin-api/member/**', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`), `status` = 1, `deleted` = b'0';

INSERT IGNORE INTO `sys_admin_user_role` (`admin_user_id`, `role_id`)
SELECT `id`, 1 FROM `sys_admin_user` WHERE `username` = 'admin' AND `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission` WHERE `deleted` = b'0';
