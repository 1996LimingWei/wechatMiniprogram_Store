CREATE TABLE IF NOT EXISTS `product_search_history` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `keyword` varchar(64) NOT NULL COMMENT '搜索关键词',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_keyword` (`user_id`, `keyword`),
    KEY `idx_user_update_time` (`user_id`, `update_time`)
) ENGINE=InnoDB COMMENT='会员商品搜索历史表';

