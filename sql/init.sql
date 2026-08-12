-- ============================================
-- 药食同源小程序商城 - 初始化SQL (Demo阶段)
-- ============================================

CREATE DATABASE IF NOT EXISTS `shop` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `shop`;

-- ============ 会员相关 ============

CREATE TABLE `member_user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `openid` varchar(64) DEFAULT NULL COMMENT '微信openid',
    `unionid` varchar(64) DEFAULT NULL COMMENT '微信unionid',
    `session_key` varchar(128) DEFAULT NULL COMMENT '微信session_key',
    `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
    `nickname` varchar(64) DEFAULT '' COMMENT '昵称',
    `avatar` varchar(512) DEFAULT '' COMMENT '头像URL',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=正常 0=禁用',
    `member_level` tinyint NOT NULL DEFAULT 1 COMMENT '会员等级 1=白银会员 2=黄金会员',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_mobile` (`mobile`)
) ENGINE=InnoDB COMMENT='会员用户表';

CREATE TABLE `member_privacy_consent` (
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

-- ============ 商品相关 ============

CREATE TABLE `product_category` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类ID(0=一级)',
    `name` varchar(64) NOT NULL COMMENT '分类名称',
    `icon` varchar(512) DEFAULT '' COMMENT '图标URL',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越大越前)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='商品分类表';

CREATE TABLE `product_spu` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `name` varchar(128) NOT NULL COMMENT '商品名称',
    `keyword` varchar(256) DEFAULT '' COMMENT '关键词(搜索用)',
    `introduction` varchar(256) DEFAULT '' COMMENT '简介',
    `description` text COMMENT '详情(富文本)',
    `pic_url` varchar(512) NOT NULL COMMENT '主图URL',
    `slider_pic_urls` varchar(2048) DEFAULT '[]' COMMENT '轮播图JSON数组',
    `video_url` varchar(512) DEFAULT '' COMMENT '视频URL',
    `type` tinyint NOT NULL DEFAULT 1 COMMENT '类型 1=实物 2=虚拟(课程)',
    `price` int NOT NULL COMMENT '最低价格(分)',
    `market_price` int DEFAULT NULL COMMENT '划线价(分)',
    `stock` int NOT NULL DEFAULT 0 COMMENT '总库存',
    `sales_count` int NOT NULL DEFAULT 0 COMMENT '实际销量',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0=下架 1=上架',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='商品SPU表';

CREATE TABLE `product_sku` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `properties` varchar(512) DEFAULT '[]' COMMENT '属性JSON [{id,name,valueId,valueName}]',
    `price` int NOT NULL COMMENT '价格(分)',
    `market_price` int DEFAULT NULL COMMENT '划线价(分)',
    `stock` int NOT NULL DEFAULT 0 COMMENT '库存',
    `pic_url` varchar(512) DEFAULT '' COMMENT 'SKU图片',
    `weight` double DEFAULT NULL COMMENT '重量(kg)',
    `volume` double DEFAULT NULL COMMENT '体积(m3)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB COMMENT='商品SKU表';

-- ============ 系统相关 ============

CREATE TABLE `sys_admin_user` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `username` varchar(64) NOT NULL COMMENT '用户名',
    `password` varchar(128) NOT NULL COMMENT '密码(BCrypt)',
    `nickname` varchar(64) DEFAULT '' COMMENT '昵称',
    `avatar` varchar(512) NOT NULL DEFAULT '' COMMENT '头像地址',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=正常 0=禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='管理员用户表';

-- 插入默认管理员 (密码: admin123)
INSERT INTO `sys_admin_user` (`username`, `password`, `nickname`)
VALUES ('admin', '$2a$10$5ajKqxodMJAf3NeshVkMa.0C2CRpXnzos8ylffU07tsRVq.F4q/fO', '超级管理员');

-- 插入Demo分类
INSERT INTO `product_category` (`name`, `icon`, `sort`, `status`) VALUES
('农副产品', '', 100, 1),
('保健品', '', 90, 1),
('课程研学', '', 80, 1),
('组合套装', '', 70, 1);

INSERT INTO `product_category` (`parent_id`, `name`, `icon`, `sort`, `status`) VALUES
(1, '滋补养生', '', 100, 1), (1, '茶饮花茶', '', 90, 1),
(2, '营养补充', '', 100, 1), (2, '药膳食材', '', 90, 1);

INSERT INTO `product_spu` (`category_id`, `name`, `keyword`, `introduction`, `description`, `pic_url`, `slider_pic_urls`, `price`, `market_price`, `stock`, `sales_count`, `sort`, `status`) VALUES
(5, '东阿阿胶糕 250g', '阿胶 滋补', '甄选阿胶与核桃芝麻', '<p>传统工艺熬制，滋补养生。</p>', 'https://picsum.photos/seed/ejiao/600/600', '["https://picsum.photos/seed/ejiao/600/600"]', 9980, 12800, 120, 328, 100, 1),
(6, '枸杞菊花茶 120g', '枸杞 花茶', '清润回甘，独立小袋装', '<p>每日一杯，清润舒心。</p>', 'https://picsum.photos/seed/tea/600/600', '["https://picsum.photos/seed/tea/600/600"]', 3980, 5980, 160, 246, 90, 1),
(7, '维生素C咀嚼片', '维生素 营养', '每日营养补充', '<p>清新橙味，便携易食。</p>', 'https://picsum.photos/seed/vitamin/600/600', '["https://picsum.photos/seed/vitamin/600/600"]', 5980, 7980, 200, 180, 80, 1),
(8, '黄芪片 200g', '黄芪 药膳', '优选黄芪切片', '<p>汤饮皆宜，片型完整。</p>', 'https://picsum.photos/seed/huangqi/600/600', '["https://picsum.photos/seed/huangqi/600/600"]', 4580, 6800, 80, 96, 70, 1);

INSERT INTO `product_sku` (`spu_id`, `properties`, `price`, `market_price`, `stock`, `pic_url`) VALUES
(1, '[{"id":1,"name":"规格","valueId":1,"valueName":"250g"}]', 9980, 12800, 120, 'https://picsum.photos/seed/ejiao/600/600'),
(2, '[{"id":1,"name":"规格","valueId":2,"valueName":"120g"}]', 3980, 5980, 160, 'https://picsum.photos/seed/tea/600/600'),
(3, '[{"id":1,"name":"规格","valueId":3,"valueName":"60片"}]', 5980, 7980, 200, 'https://picsum.photos/seed/vitamin/600/600'),
(4, '[{"id":1,"name":"规格","valueId":4,"valueName":"200g"}]', 4580, 6800, 80, 'https://picsum.photos/seed/huangqi/600/600');

-- ============ 内容相关 ============

CREATE TABLE `content_banner` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `title` varchar(128) NOT NULL COMMENT '标题',
    `pic_url` varchar(512) NOT NULL COMMENT '图片URL',
    `url` varchar(512) DEFAULT '' COMMENT '跳转链接',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='轮播图表';

CREATE TABLE `member_collect` (`id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL, `spu_id` bigint NOT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (`id`), UNIQUE KEY `uk_user_spu` (`user_id`,`spu_id`,`deleted`), KEY `idx_user_id` (`user_id`)) ENGINE=InnoDB COMMENT='会员商品收藏表';
CREATE TABLE `member_footprint` (`id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL, `spu_id` bigint NOT NULL, `browse_date` date NOT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (`id`), UNIQUE KEY `uk_user_spu_date` (`user_id`,`spu_id`,`browse_date`,`deleted`), KEY `idx_user_date` (`user_id`,`browse_date`)) ENGINE=InnoDB COMMENT='会员浏览足迹表';
CREATE TABLE `product_comment` (`id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL, `spu_id` bigint NOT NULL, `content` varchar(1000) NOT NULL, `status` tinyint NOT NULL DEFAULT 1, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (`id`), KEY `idx_spu_time` (`spu_id`,`create_time`), KEY `idx_user_id` (`user_id`)) ENGINE=InnoDB COMMENT='商品评论表';
CREATE TABLE `product_search_history` (`id` bigint NOT NULL AUTO_INCREMENT, `user_id` bigint NOT NULL, `keyword` varchar(64) NOT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (`id`), UNIQUE KEY `uk_user_keyword` (`user_id`,`keyword`), KEY `idx_user_update_time` (`user_id`,`update_time`)) ENGINE=InnoDB COMMENT='会员商品搜索历史表';

CREATE TABLE `content_channel` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(64) NOT NULL COMMENT '频道名称',
    `icon_url` varchar(512) NOT NULL DEFAULT '' COMMENT '图标URL',
    `url` varchar(512) NOT NULL DEFAULT '' COMMENT '跳转链接',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越大越前)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='首页频道表';

CREATE TABLE `content_brand` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(128) NOT NULL COMMENT '品牌名称',
    `pic_url` varchar(512) NOT NULL DEFAULT '' COMMENT '图片URL',
    `floor_price` int NOT NULL DEFAULT 0 COMMENT '起售价(分)',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越大越前)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='首页品牌表';

CREATE TABLE `content_topic` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `title` varchar(128) NOT NULL COMMENT '专题标题',
    `subtitle` varchar(255) NOT NULL DEFAULT '' COMMENT '专题副标题',
    `pic_url` varchar(512) NOT NULL DEFAULT '' COMMENT '场景图片URL',
    `price_info` varchar(64) NOT NULL DEFAULT '' COMMENT '价格说明',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越大越前)',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=开启 0=关闭',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='首页专题表';

INSERT INTO `content_banner` (`title`, `pic_url`, `url`, `sort`, `status`) VALUES
('滋补养生好物', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=750&auto=format&fit=crop', '/pages/goods/goods?id=1', 100, 1),
('四季养生专题', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=750&auto=format&fit=crop', '/pages/topic/topic', 90, 1);

INSERT INTO `content_channel` (`name`, `icon_url`, `url`, `sort`, `status`) VALUES
('新品首发', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=96', '/pages/newGoods/newGoods', 100, 1),
('人气推荐', 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=96', '/pages/hotGoods/hotGoods', 90, 1),
('全部分类', 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=96', '/pages/catalog/catalog', 80, 1);

INSERT INTO `content_brand` (`name`, `pic_url`, `floor_price`, `sort`, `status`) VALUES
('东阿阿胶', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600&auto=format&fit=crop', 9900, 100, 1),
('同仁堂', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop', 5900, 90, 1);

INSERT INTO `content_topic` (`title`, `subtitle`, `pic_url`, `price_info`, `sort`, `status`) VALUES
('药食同源养生指南', '传统中医智慧，现代健康生活', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600&auto=format&fit=crop', '49.9', 100, 1),
('四季养生茶饮', '顺应时节，调养身心', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop', '29.9', 90, 1);

-- ============ 交易闭环相关 ============

CREATE TABLE `member_address` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `user_name` varchar(64) NOT NULL COMMENT '收货人',
    `tel_number` varchar(20) NOT NULL COMMENT '手机号',
    `province_id` bigint DEFAULT 0 COMMENT '省份ID',
    `city_id` bigint DEFAULT 0 COMMENT '城市ID',
    `district_id` bigint DEFAULT 0 COMMENT '区县ID',
    `province_name` varchar(64) DEFAULT '' COMMENT '省份名称',
    `city_name` varchar(64) DEFAULT '' COMMENT '城市名称',
    `district_name` varchar(64) DEFAULT '' COMMENT '区县名称',
    `full_region` varchar(255) DEFAULT '' COMMENT '省市区完整名称',
    `detail_info` varchar(255) NOT NULL COMMENT '详细地址',
    `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认 1=是 0=否',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='会员收货地址表';

CREATE TABLE `trade_cart` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
    `goods_name` varchar(128) NOT NULL COMMENT '商品名称快照',
    `goods_pic_url` varchar(512) DEFAULT '' COMMENT '商品图片快照',
    `spec_name` varchar(128) DEFAULT '' COMMENT '规格快照',
    `price` int NOT NULL COMMENT '单价(分)',
    `count` int NOT NULL COMMENT '数量',
    `checked` tinyint NOT NULL DEFAULT 1 COMMENT '是否选中 1=是 0=否',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='购物车表';

CREATE TABLE `trade_order` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_sn` varchar(32) NOT NULL COMMENT '订单号',
    `request_id` varchar(64) DEFAULT NULL COMMENT '客户端下单幂等标识',
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态 0=待付款 1=待发货 2=待收货 3=已完成 4=已取消 5=退款中',
    `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0=未支付 1=已支付 2=已退款',
    `goods_price` int NOT NULL DEFAULT 0 COMMENT '商品总价(分)',
    `freight_price` int NOT NULL DEFAULT 0 COMMENT '运费(分)',
    `coupon_price` int NOT NULL DEFAULT 0 COMMENT '优惠金额(分)',
    `order_price` int NOT NULL DEFAULT 0 COMMENT '订单总价(分)',
    `actual_price` int NOT NULL DEFAULT 0 COMMENT '实付金额(分)',
    `address_id` bigint DEFAULT NULL COMMENT '地址ID',
    `consignee` varchar(64) DEFAULT '' COMMENT '收货人快照',
    `mobile` varchar(20) DEFAULT '' COMMENT '手机号快照',
    `full_region` varchar(255) DEFAULT '' COMMENT '省市区快照',
    `address` varchar(255) DEFAULT '' COMMENT '详细地址快照',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `expire_time` datetime DEFAULT NULL COMMENT '待付款超时关闭时间',
    `close_time` datetime DEFAULT NULL COMMENT '订单关闭时间',
    `close_reason` varchar(128) DEFAULT '' COMMENT '订单关闭原因',
    `finish_time` datetime DEFAULT NULL COMMENT '订单完成时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    UNIQUE KEY `uk_user_request_id` (`user_id`, `request_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_status` (`status`, `pay_status`, `expire_time`),
    KEY `idx_create_time_id` (`create_time`, `id`),
    KEY `idx_user_create_time_id` (`user_id`, `create_time`, `id`),
    KEY `idx_mobile_create_time_id` (`mobile`, `create_time`, `id`),
    KEY `idx_status_pay_create_time_id` (`status`, `pay_status`, `create_time`, `id`),
    KEY `idx_pay_status_create_time_id` (`pay_status`, `create_time`, `id`)
) ENGINE=InnoDB COMMENT='交易订单表';

CREATE TABLE `trade_order_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
    `goods_name` varchar(128) NOT NULL COMMENT '商品名称快照',
    `goods_pic_url` varchar(512) DEFAULT '' COMMENT '商品图片快照',
    `spec_name` varchar(128) DEFAULT '' COMMENT '规格快照',
    `price` int NOT NULL COMMENT '单价(分)',
    `count` int NOT NULL COMMENT '数量',
    `total_price` int NOT NULL COMMENT '小计(分)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='交易订单明细表';

CREATE TABLE `trade_order_log` (
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

CREATE TABLE `pay_order` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `pay_sn` varchar(32) NOT NULL COMMENT '支付单号',
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `amount` int NOT NULL COMMENT '支付金额(分)',
    `channel` varchar(32) NOT NULL DEFAULT 'mock' COMMENT '支付渠道 mock/wx_lite',
    `channel_trade_no` varchar(64) DEFAULT NULL COMMENT '支付渠道交易号',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0=待支付 1=已支付 2=已关闭 3=已退款',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `last_query_time` datetime DEFAULT NULL COMMENT '最近主动查单时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_sn` (`pay_sn`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    UNIQUE KEY `uk_channel_trade_no` (`channel_trade_no`),
    KEY `idx_status_channel_query` (`status`, `channel`, `last_query_time`, `id`)
) ENGINE=InnoDB COMMENT='支付单表';

CREATE TABLE `pay_notify_log` (
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

CREATE TABLE `trade_order_logistics` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `logistics_company` varchar(64) DEFAULT '' COMMENT '物流公司',
    `logistics_no` varchar(64) DEFAULT '' COMMENT '物流单号',
    `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB COMMENT='订单物流表';

CREATE TABLE `trade_after_sale` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `user_id` bigint NOT NULL COMMENT '会员用户ID',
    `after_sale_sn` varchar(32) NOT NULL COMMENT '售后单号',
    `type` tinyint NOT NULL DEFAULT 1 COMMENT '售后类型 1=仅退款 2=退货退款',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '售后状态 0=待审核 1=已退款 2=已拒绝 3=已撤销 4=退款处理中 5=退款失败',
    `refund_amount` int NOT NULL DEFAULT 0 COMMENT '退款金额(分)',
    `reason` varchar(128) DEFAULT '' COMMENT '申请原因',
    `apply_remark` varchar(255) DEFAULT '' COMMENT '申请说明',
    `before_order_status` tinyint DEFAULT NULL COMMENT '申请售后前订单状态',
    `reject_reason` varchar(255) DEFAULT '' COMMENT '拒绝原因',
    `refund_provider` varchar(32) DEFAULT '' COMMENT '退款提供方',
    `provider_refund_no` varchar(64) DEFAULT '' COMMENT '渠道退款单号',
    `refund_message` varchar(255) DEFAULT '' COMMENT '退款渠道说明',
    `refund_attempt_count` int NOT NULL DEFAULT 0 COMMENT '退款渠道调用次数',
    `refund_last_attempt_time` datetime DEFAULT NULL COMMENT '退款最近调用时间',
    `refund_next_attempt_time` datetime DEFAULT NULL COMMENT '退款下次调用时间',
    `refund_claim_until` datetime DEFAULT NULL COMMENT '退款任务占用截止时间',
    `refund_last_error` varchar(255) NOT NULL DEFAULT '' COMMENT '退款最近调用错误',
    `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
    `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
    `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间',
    `reject_time` datetime DEFAULT NULL COMMENT '拒绝时间',
    `cancel_time` datetime DEFAULT NULL COMMENT '撤销时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_sn` (`after_sale_sn`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_status_create_time_id` (`status`, `create_time`, `id`),
    KEY `idx_refund_retry` (`status`, `refund_next_attempt_time`, `refund_claim_until`, `refund_attempt_count`, `id`)
) ENGINE=InnoDB COMMENT='交易售后表';
