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
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_mobile` (`mobile`)
) ENGINE=InnoDB COMMENT='会员用户表';

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
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1=正常 0=禁用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='管理员用户表';

-- 插入默认管理员 (密码: admin123)
INSERT INTO `sys_admin_user` (`username`, `password`, `nickname`)
VALUES ('admin', '$2a$10$YMpimV4T/3Cq.UoFqMFJ6eOPHoGRTnr9X8tJLBXvBL7Uh3LQFX6G', '超级管理员');

-- 插入Demo分类
INSERT INTO `product_category` (`name`, `icon`, `sort`, `status`) VALUES
('农副产品', '', 100, 1),
('保健品', '', 90, 1),
('课程研学', '', 80, 1),
('组合套装', '', 70, 1);

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
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`, `deleted`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='购物车表';

CREATE TABLE `trade_order` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `order_sn` varchar(32) NOT NULL COMMENT '订单号',
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
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_status` (`status`, `pay_status`, `expire_time`)
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
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态 0=待支付 1=已支付 2=已关闭 3=已退款',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_sn` (`pay_sn`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB COMMENT='支付单表';

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
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '售后状态 0=处理中 1=已退款 2=已拒绝',
    `refund_amount` int NOT NULL DEFAULT 0 COMMENT '退款金额(分)',
    `reason` varchar(128) DEFAULT '' COMMENT '申请原因',
    `apply_remark` varchar(255) DEFAULT '' COMMENT '申请说明',
    `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
    `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_sn` (`after_sale_sn`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='交易售后表';
