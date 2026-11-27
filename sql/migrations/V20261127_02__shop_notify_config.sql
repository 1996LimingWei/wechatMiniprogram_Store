SET NAMES utf8mb4;

-- 商家新订单通知配置：单行配置表（id 固定为 1），由超管在后台维护。
-- SMTP 授权码按需填写，接口读取时统一掩码返回。
CREATE TABLE IF NOT EXISTS `shop_notify_config` (
    `id` tinyint NOT NULL COMMENT '固定为 1',
    `enabled` tinyint NOT NULL DEFAULT 0 COMMENT '总开关 1=开启 0=关闭',
    `mail_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '邮件渠道 1=开启 0=关闭',
    `mail_recipients` varchar(1024) NOT NULL DEFAULT '' COMMENT '收件人，多个用逗号分隔',
    `smtp_host` varchar(255) NOT NULL DEFAULT '' COMMENT 'SMTP 服务器',
    `smtp_port` int NOT NULL DEFAULT 465 COMMENT 'SMTP 端口',
    `smtp_username` varchar(255) NOT NULL DEFAULT '' COMMENT 'SMTP 登录账号',
    `smtp_password` varchar(512) NOT NULL DEFAULT '' COMMENT 'SMTP 授权码',
    `mail_from` varchar(255) NOT NULL DEFAULT '' COMMENT '发件人显示地址，留空取 SMTP 账号',
    `ssl_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '1=SSL/TLS 直连 0=明文（本地调试用）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家新订单通知配置表';

INSERT IGNORE INTO `shop_notify_config` (`id`) VALUES (1);

-- 通知发送记录：便于排查漏发与在后台追踪最近推送结果
CREATE TABLE IF NOT EXISTS `shop_order_notify_log` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `order_sn` varchar(32) NOT NULL DEFAULT '' COMMENT '订单号',
    `channel` varchar(16) NOT NULL DEFAULT 'email' COMMENT '通知渠道',
    `receiver` varchar(1024) NOT NULL DEFAULT '' COMMENT '接收方',
    `success` tinyint NOT NULL DEFAULT 0 COMMENT '1=成功 0=失败',
    `message` varchar(512) NOT NULL DEFAULT '' COMMENT '失败原因或响应摘要',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家订单通知发送记录表';
