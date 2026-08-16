CREATE TABLE IF NOT EXISTS `refund_notify_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `notification_id` varchar(64) NOT NULL COMMENT '微信退款通知ID',
    `after_sale_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '售后单号',
    `provider_refund_no` varchar(64) NOT NULL DEFAULT '' COMMENT '微信退款单号',
    `pay_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '商户支付单号',
    `event_type` varchar(64) NOT NULL DEFAULT '' COMMENT '通知事件类型',
    `refund_status` varchar(32) NOT NULL DEFAULT '' COMMENT '微信退款状态',
    `refund_amount` int NOT NULL DEFAULT 0 COMMENT '退款金额，单位分',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0=已接收 1=已处理',
    `message` varchar(255) NOT NULL DEFAULT '' COMMENT '处理说明',
    `raw_body` longtext COMMENT '原始加密通知体',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_notification_id` (`notification_id`),
    KEY `idx_refund_notify_after_sale_sn` (`after_sale_sn`),
    KEY `idx_refund_notify_provider_refund_no` (`provider_refund_no`),
    KEY `idx_refund_notify_pay_sn` (`pay_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款通知流水表';

CREATE TABLE IF NOT EXISTS `refund_notify_failure_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `notification_id` varchar(64) NOT NULL DEFAULT '' COMMENT '微信退款通知ID',
    `after_sale_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '售后单号',
    `provider_refund_no` varchar(64) NOT NULL DEFAULT '' COMMENT '微信退款单号',
    `pay_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '商户支付单号',
    `wechatpay_serial` varchar(128) NOT NULL DEFAULT '' COMMENT '微信支付平台证书序列号',
    `request_timestamp` varchar(32) NOT NULL DEFAULT '' COMMENT '回调请求时间戳',
    `body_sha256` char(64) NOT NULL COMMENT '原始通知体 SHA-256 摘要',
    `error_message` varchar(255) NOT NULL DEFAULT '' COMMENT '失败原因',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_refund_notify_failure_time` (`create_time`),
    KEY `idx_refund_notify_failure_notification` (`notification_id`),
    KEY `idx_refund_notify_failure_after_sale_sn` (`after_sale_sn`),
    KEY `idx_refund_notify_failure_provider_refund_no` (`provider_refund_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款通知失败审计';
