SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `trade_reconcile_batch` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `reconcile_date` date NOT NULL COMMENT '对账日期',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '对账状态 0=处理中 1=完成 2=失败',
    `source` varchar(32) NOT NULL DEFAULT 'LOCAL_SNAPSHOT' COMMENT '渠道账单来源',
    `local_pay_count` int NOT NULL DEFAULT 0 COMMENT '本地支付笔数',
    `local_pay_amount` int NOT NULL DEFAULT 0 COMMENT '本地支付金额(分)',
    `local_refund_count` int NOT NULL DEFAULT 0 COMMENT '本地退款笔数',
    `local_refund_amount` int NOT NULL DEFAULT 0 COMMENT '本地退款金额(分)',
    `local_net_amount` int NOT NULL DEFAULT 0 COMMENT '本地净收入(分)',
    `wechat_pay_count` int NOT NULL DEFAULT 0 COMMENT '微信支付笔数',
    `wechat_pay_amount` int NOT NULL DEFAULT 0 COMMENT '微信支付金额(分)',
    `wechat_refund_count` int NOT NULL DEFAULT 0 COMMENT '微信退款笔数',
    `wechat_refund_amount` int NOT NULL DEFAULT 0 COMMENT '微信退款金额(分)',
    `wechat_net_amount` int NOT NULL DEFAULT 0 COMMENT '微信净收入(分)',
    `fee_amount` int DEFAULT NULL COMMENT '渠道手续费(分)',
    `difference_count` int NOT NULL DEFAULT 0 COMMENT '差异数量',
    `trade_bill_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '微信交易账单下载地址',
    `fund_bill_url` varchar(1024) NOT NULL DEFAULT '' COMMENT '微信资金账单下载地址',
    `trigger_type` varchar(16) NOT NULL DEFAULT 'MANUAL' COMMENT '触发类型 MANUAL/JOB',
    `trigger_admin_id` bigint DEFAULT NULL COMMENT '触发管理员ID',
    `message` varchar(255) NOT NULL DEFAULT '' COMMENT '对账说明',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reconcile_date` (`reconcile_date`),
    KEY `idx_status_date` (`status`, `reconcile_date`, `id`)
) ENGINE=InnoDB COMMENT='日终对账批次表';

CREATE TABLE IF NOT EXISTS `trade_reconcile_difference` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `batch_id` bigint NOT NULL COMMENT '对账批次ID',
    `reconcile_date` date NOT NULL COMMENT '对账日期',
    `diff_type` varchar(32) NOT NULL COMMENT '差异类型 BALANCED/LOCAL_MORE/WECHAT_MORE/AMOUNT_MISMATCH/STATUS_MISMATCH/MISSING_ORDER',
    `business_type` varchar(16) NOT NULL COMMENT '业务类型 PAY/REFUND/SUMMARY',
    `business_sn` varchar(64) NOT NULL DEFAULT '' COMMENT '业务单号',
    `order_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '订单号',
    `local_amount` int DEFAULT NULL COMMENT '本地金额(分)',
    `channel_amount` int DEFAULT NULL COMMENT '渠道金额(分)',
    `local_status` varchar(32) NOT NULL DEFAULT '' COMMENT '本地状态',
    `channel_status` varchar(32) NOT NULL DEFAULT '' COMMENT '渠道状态',
    `reason` varchar(255) NOT NULL DEFAULT '' COMMENT '差异原因',
    `handled` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0=待处理 1=已处理',
    `handle_remark` varchar(255) NOT NULL DEFAULT '' COMMENT '处理备注',
    `handle_admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID',
    `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_batch_type` (`batch_id`, `diff_type`, `handled`, `id`),
    KEY `idx_reconcile_date` (`reconcile_date`, `business_type`, `business_sn`),
    KEY `idx_order_sn` (`order_sn`)
) ENGINE=InnoDB COMMENT='日终对账差异表';

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES
(44, 'trade:reconcile-read', '查看日终对账', '/admin-api/trade/reconcile/*/**', 'GET', 1),
(45, 'trade:reconcile-trigger', '手动触发日终对账', '/admin-api/trade/reconcile/run', 'POST', 1),
(46, 'trade:reconcile-export', '导出日终对账结果', '/admin-api/trade/reconcile/export', 'GET', 1),
(47, 'trade:reconcile-handle', '处理日终对账差异', '/admin-api/trade/reconcile/difference/handle', 'POST', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
                        `http_method` = VALUES(`http_method`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN'
   AND p.code IN ('trade:reconcile-read', 'trade:reconcile-trigger', 'trade:reconcile-export', 'trade:reconcile-handle')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'FINANCE'
   AND p.code IN ('trade:reconcile-read', 'trade:reconcile-trigger', 'trade:reconcile-export', 'trade:reconcile-handle')
   AND p.deleted = b'0';
