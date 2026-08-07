CREATE TABLE IF NOT EXISTS `member_privacy_consent` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `privacy_version` varchar(32) NOT NULL COMMENT '隐私政策版本',
    `agreement_version` varchar(32) NOT NULL COMMENT '用户协议版本',
    `consent_time` datetime NOT NULL COMMENT '同意时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_versions` (`user_id`, `privacy_version`, `agreement_version`)
) ENGINE=InnoDB COMMENT='会员协议与隐私同意记录';
