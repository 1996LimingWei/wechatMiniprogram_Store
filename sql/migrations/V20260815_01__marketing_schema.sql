-- ============================================================
-- 营销模块：优惠券模板、用户优惠券实例、满减规则、包邮规则
-- ============================================================

-- 优惠券模板
CREATE TABLE IF NOT EXISTS `marketing_coupon_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(128) NOT NULL COMMENT '券名称',
    `type` tinyint NOT NULL DEFAULT 1 COMMENT '1=满减券 2=新人券',
    `threshold_amount` int NOT NULL DEFAULT 0 COMMENT '满减门槛(分)，0=无门槛',
    `discount_amount` int NOT NULL COMMENT '优惠金额(分)',
    `total_count` int NOT NULL DEFAULT 0 COMMENT '发行总量，0=不限量',
    `claimed_count` int NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `per_user_limit` int NOT NULL DEFAULT 1 COMMENT '每人限领',
    `validity_type` tinyint NOT NULL DEFAULT 1 COMMENT '1=固定日期 2=领取后N天',
    `valid_start_time` datetime DEFAULT NULL COMMENT '有效期开始(固定日期)',
    `valid_end_time` datetime DEFAULT NULL COMMENT '有效期结束(固定日期)',
    `valid_days` int DEFAULT NULL COMMENT '领取后有效天数',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 用户优惠券实例
CREATE TABLE IF NOT EXISTS `marketing_coupon` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `template_id` bigint NOT NULL COMMENT '优惠券模板ID',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用 2=已过期',
    `order_id` bigint DEFAULT NULL COMMENT '使用的订单ID',
    `used_time` datetime DEFAULT NULL COMMENT '使用时间',
    `expire_time` datetime NOT NULL COMMENT '过期时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_template` (`template_id`),
    INDEX `idx_expire` (`expire_time`, `status`),
    INDEX `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券实例表';

-- 满减活动规则
CREATE TABLE IF NOT EXISTS `marketing_promotion_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(128) NOT NULL COMMENT '活动名称',
    `type` tinyint NOT NULL DEFAULT 1 COMMENT '1=全店满减',
    `threshold_amount` int NOT NULL COMMENT '满减门槛(分)',
    `discount_amount` int NOT NULL COMMENT '优惠金额(分)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `priority` int NOT NULL DEFAULT 0 COMMENT '排序优先级',
    `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    INDEX `idx_status_priority` (`status`, `priority` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满减活动规则表';

-- 包邮规则
CREATE TABLE IF NOT EXISTS `marketing_shipping_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(64) NOT NULL COMMENT '规则名称',
    `free_threshold` int NOT NULL COMMENT '包邮门槛(分)',
    `base_fee` int NOT NULL COMMENT '基础运费(分)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包邮规则表';

-- trade_order 新增营销关联字段
ALTER TABLE `trade_order`
    ADD COLUMN `coupon_id` bigint DEFAULT NULL COMMENT '使用的优惠券ID' AFTER `finish_time`,
    ADD COLUMN `discount_source` varchar(16) DEFAULT NULL COMMENT '优惠来源 coupon=优惠券 promotion=满减' AFTER `coupon_id`;

-- 默认包邮规则（与当前硬编码一致：满199包邮，基础运费10元）
INSERT INTO `marketing_shipping_rule` (`name`, `free_threshold`, `base_fee`, `status`)
SELECT '默认包邮规则', 19900, 1000, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_shipping_rule` WHERE `deleted` = b'0');

-- 演示满减规则
INSERT INTO `marketing_promotion_rule` (`id`, `name`, `type`, `threshold_amount`, `discount_amount`, `status`, `priority`)
SELECT 10001, '满100减10', 1, 10000, 1000, 1, 10
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_promotion_rule` WHERE `id` = 10001);

INSERT INTO `marketing_promotion_rule` (`id`, `name`, `type`, `threshold_amount`, `discount_amount`, `status`, `priority`)
SELECT 10002, '满200减30', 1, 20000, 3000, 1, 20
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_promotion_rule` WHERE `id` = 10002);

INSERT INTO `marketing_promotion_rule` (`id`, `name`, `type`, `threshold_amount`, `discount_amount`, `status`, `priority`)
SELECT 10003, '满500减100', 1, 50000, 10000, 1, 30
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_promotion_rule` WHERE `id` = 10003);

-- 演示优惠券模板
INSERT INTO `marketing_coupon_template` (`id`, `name`, `type`, `threshold_amount`, `discount_amount`, `total_count`, `per_user_limit`, `validity_type`, `valid_days`, `status`)
SELECT 10001, '新人专享满50减15', 2, 5000, 1500, 0, 1, 2, 30, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_coupon_template` WHERE `id` = 10001);

INSERT INTO `marketing_coupon_template` (`id`, `name`, `type`, `threshold_amount`, `discount_amount`, `total_count`, `per_user_limit`, `validity_type`, `valid_start_time`, `valid_end_time`, `status`)
SELECT 10002, '满100减20', 1, 10000, 2000, 1000, 2, 1, '2026-08-01 00:00:00', '2026-12-31 23:59:59', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_coupon_template` WHERE `id` = 10002);

INSERT INTO `marketing_coupon_template` (`id`, `name`, `type`, `threshold_amount`, `discount_amount`, `total_count`, `per_user_limit`, `validity_type`, `valid_start_time`, `valid_end_time`, `status`)
SELECT 10003, '满200减50', 1, 20000, 5000, 500, 1, 1, '2026-08-01 00:00:00', '2026-12-31 23:59:59', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `marketing_coupon_template` WHERE `id` = 10003);
