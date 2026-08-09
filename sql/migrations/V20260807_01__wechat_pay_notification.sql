DROP PROCEDURE IF EXISTS migration_add_wechat_pay_notification;

DELIMITER $$

CREATE PROCEDURE migration_add_wechat_pay_notification()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'pay_order'
           AND column_name = 'channel_trade_no'
    ) THEN
        ALTER TABLE `pay_order`
            ADD COLUMN `channel_trade_no` varchar(64) DEFAULT NULL COMMENT '支付渠道交易号'
            AFTER `channel`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'pay_order'
           AND index_name = 'uk_channel_trade_no'
    ) THEN
        ALTER TABLE `pay_order`
            ADD UNIQUE INDEX `uk_channel_trade_no` (`channel_trade_no`);
    END IF;
END$$

DELIMITER ;

CALL migration_add_wechat_pay_notification();
DROP PROCEDURE migration_add_wechat_pay_notification;

CREATE TABLE IF NOT EXISTS `pay_notify_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `notification_id` varchar(64) NOT NULL COMMENT '微信支付通知ID',
    `pay_order_id` bigint DEFAULT NULL COMMENT '支付单ID',
    `pay_sn` varchar(32) DEFAULT '' COMMENT '商户支付单号',
    `channel_trade_no` varchar(64) DEFAULT '' COMMENT '微信支付交易号',
    `event_type` varchar(64) DEFAULT '' COMMENT '通知事件类型',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0=已接收 1=已处理',
    `message` varchar(255) DEFAULT '' COMMENT '处理说明',
    `raw_body` longtext COMMENT '原始加密通知体',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_id` (`notification_id`),
    KEY `idx_pay_order_id` (`pay_order_id`),
    KEY `idx_pay_sn` (`pay_sn`)
) ENGINE=InnoDB COMMENT='支付通知流水表';
