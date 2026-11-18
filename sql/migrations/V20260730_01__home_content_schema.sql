CREATE TABLE IF NOT EXISTS `content_channel` (
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

CREATE TABLE IF NOT EXISTS `content_brand` (
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

CREATE TABLE IF NOT EXISTS `content_topic` (
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

INSERT INTO `content_banner` (`title`, `pic_url`, `url`, `sort`, `status`)
SELECT '滋补养生好物', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=750&auto=format&fit=crop', '/pages/goods/goods?id=1', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_banner` WHERE `title` = '滋补养生好物');
INSERT INTO `content_banner` (`title`, `pic_url`, `url`, `sort`, `status`)
SELECT '四季养生专题', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=750&auto=format&fit=crop', '/pages/topic/topic', 90, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_banner` WHERE `title` = '四季养生专题');

INSERT INTO `content_channel` (`name`, `icon_url`, `url`, `sort`, `status`)
SELECT '新品首发', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=96', '/pages/newGoods/newGoods', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_channel` WHERE `name` = '新品首发');
INSERT INTO `content_channel` (`name`, `icon_url`, `url`, `sort`, `status`)
SELECT '人气推荐', 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=96', '/pages/hotGoods/hotGoods', 90, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_channel` WHERE `name` = '人气推荐');
INSERT INTO `content_channel` (`name`, `icon_url`, `url`, `sort`, `status`)
SELECT '全部分类', 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=96', '/pages/catalog/catalog', 80, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_channel` WHERE `name` = '全部分类');

INSERT INTO `content_brand` (`name`, `pic_url`, `floor_price`, `sort`, `status`)
SELECT '东阿阿胶', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600&auto=format&fit=crop', 9900, 100, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_brand` WHERE `name` = '东阿阿胶');
INSERT INTO `content_brand` (`name`, `pic_url`, `floor_price`, `sort`, `status`)
SELECT '同仁堂', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop', 5900, 90, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_brand` WHERE `name` = '同仁堂');

INSERT INTO `content_topic` (`title`, `subtitle`, `pic_url`, `price_info`, `sort`, `status`)
SELECT '药食同源养生指南', '传统中医智慧，现代健康生活', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600&auto=format&fit=crop', '49.9', 100, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_topic` WHERE `title` = '药食同源养生指南');
INSERT INTO `content_topic` (`title`, `subtitle`, `pic_url`, `price_info`, `sort`, `status`)
SELECT '四季养生茶饮', '顺应时节，调养身心', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop', '29.9', 90, 1
WHERE NOT EXISTS (SELECT 1 FROM `content_topic` WHERE `title` = '四季养生茶饮');
