-- 将小程序前端原 ProductMockFixture 展示商品同步到数据库。
-- 目标：小程序端看到的商品，管理后台商品列表一定能管理到。

SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO `product_category`
    (`id`, `parent_id`, `name`, `icon`, `sort`, `status`, `deleted`)
VALUES
    (1, 0, '滋补养生', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600&auto=format&fit=crop', 500, 1, b'0'),
    (2, 0, '茶饮花茶', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop', 490, 1, b'0'),
    (3, 0, '零食坚果', 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600&auto=format&fit=crop', 480, 1, b'0'),
    (4, 0, '保健食品', 'https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=600&auto=format&fit=crop', 470, 1, b'0'),
    (5, 0, '药膳食材', 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600&auto=format&fit=crop', 460, 1, b'0')
ON DUPLICATE KEY UPDATE
    `parent_id` = VALUES(`parent_id`),
    `name` = VALUES(`name`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

INSERT INTO `product_spu`
    (`id`, `category_id`, `name`, `keyword`, `introduction`, `description`, `pic_url`,
     `slider_pic_urls`, `type`, `price`, `market_price`, `stock`, `sales_count`, `sort`,
     `status`, `create_time`, `update_time`, `deleted`)
VALUES
    (1, 1, '东阿阿胶糕', '阿胶 滋补 补气养血 美容养颜 热销 新品',
     '补气养血，美容养颜，传统手工熬制',
     '<p>补气养血，美容养颜，传统手工熬制。</p>',
     'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600',
     '["https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600"]',
     1, 9990, 12800, 120, 380, 500, 1, NOW() - INTERVAL 1 HOUR, NOW(), b'0'),
    (2, 1, '同仁堂枸杞', '枸杞 滋补 宁夏 热销',
     '宁夏特级免洗枸杞，粒大饱满，甘甜可口',
     '<p>宁夏特级免洗枸杞，粒大饱满，甘甜可口。</p>',
     'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=600',
     '["https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=600"]',
     1, 2450, 3200, 150, 260, 490, 1, NOW() - INTERVAL 10 DAY, NOW(), b'0'),
    (3, 1, '长白山人参', '人参 滋补 鲜参 新品',
     '整枝鲜参，长白山道地直供，元气满满',
     '<p>整枝鲜参，长白山道地直供，元气满满。</p>',
     'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600',
     '["https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600"]',
     1, 19900, 25800, 60, 156, 480, 1, NOW() - INTERVAL 2 HOUR, NOW(), b'0'),
    (4, 1, '铁皮石斛', '石斛 滋补 霍山',
     '正宗霍山铁皮石斛，胶质浓郁，养阴清热',
     '<p>正宗霍山铁皮石斛，胶质浓郁，养阴清热。</p>',
     'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600',
     '["https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600"]',
     1, 15950, 19800, 45, 88, 470, 1, NOW() - INTERVAL 12 DAY, NOW(), b'0'),
    (5, 1, '百花蜂蜜', '蜂蜜 天然 土蜂蜜 新品',
     '农家天然土蜂蜜，质地浓稠，蜜香浓郁',
     '<p>农家天然土蜂蜜，质地浓稠，蜜香浓郁。</p>',
     'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600',
     '["https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600"]',
     1, 3990, 5200, 200, 120, 460, 1, NOW() - INTERVAL 3 HOUR, NOW(), b'0'),
    (6, 2, '金边玫瑰花茶', '玫瑰 花茶 热销 新品',
     '云南墨红玫瑰，花香浓郁，疏肝理气',
     '<p>云南墨红玫瑰，花香浓郁，疏肝理气。</p>',
     'https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=600',
     '["https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=600"]',
     1, 2800, 3800, 180, 300, 450, 1, NOW() - INTERVAL 4 HOUR, NOW(), b'0'),
    (7, 2, '胎菊王菊花茶', '菊花 胎菊 花茶 热销',
     '桐乡特级胎菊，清热明目，汤色金黄',
     '<p>桐乡特级胎菊，清热明目，汤色金黄。</p>',
     'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600',
     '["https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600"]',
     1, 3200, 4200, 120, 240, 440, 1, NOW() - INTERVAL 14 DAY, NOW(), b'0'),
    (8, 3, '有机黑芝麻丸', '黑芝麻丸 零食 热销',
     '九蒸九晒黑芝麻丸，软糯浓郁',
     '<p>九蒸九晒黑芝麻丸，软糯浓郁。</p>',
     'https://images.unsplash.com/photo-1595855759920-86582396756a?w=600',
     '["https://images.unsplash.com/photo-1595855759920-86582396756a?w=600"]',
     1, 3990, 5200, 160, 280, 430, 1, NOW() - INTERVAL 8 DAY, NOW(), b'0'),
    (9, 3, '手剥夏威夷果', '夏威夷果 坚果 热销 新品',
     '大颗粒果仁，酥脆香甜',
     '<p>大颗粒果仁，酥脆香甜。</p>',
     'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600',
     '["https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600"]',
     1, 4500, 5800, 100, 220, 420, 1, NOW() - INTERVAL 5 HOUR, NOW(), b'0'),
    (10, 4, '江中健胃消食片', '消食片 保健 热销',
     '消食导滞，日常肠胃养护',
     '<p>消食导滞，日常肠胃养护。</p>',
     'https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=600',
     '["https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=600"]',
     1, 3180, 4200, 250, 410, 410, 1, NOW() - INTERVAL 20 DAY, NOW(), b'0'),
    (11, 4, '天然维生素C片', '维生素 营养 新品',
     '补充每日维生素 C',
     '<p>补充每日维生素 C，日常健康管理。</p>',
     'https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=600',
     '["https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=600"]',
     1, 2990, 3900, 200, 96, 400, 1, NOW() - INTERVAL 6 HOUR, NOW(), b'0'),
    (12, 5, '经典当归补血汤料包', '当归 黄芪 药膳 热销 新品',
     '传统当归黄芪配方，药膳煲汤',
     '<p>传统当归黄芪配方，药膳煲汤。</p>',
     'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600',
     '["https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600"]',
     1, 4500, 6200, 80, 180, 390, 1, NOW() - INTERVAL 7 HOUR, NOW(), b'0'),
    (13, 5, '黄金草虫草花', '虫草花 药膳 热销',
     '特级无根虫草花，汤汁鲜美',
     '<p>特级无根虫草花，汤汁鲜美。</p>',
     'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?w=600',
     '["https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?w=600"]',
     1, 3500, 4600, 110, 160, 380, 1, NOW() - INTERVAL 18 DAY, NOW(), b'0')
ON DUPLICATE KEY UPDATE
    `category_id` = VALUES(`category_id`),
    `name` = VALUES(`name`),
    `keyword` = VALUES(`keyword`),
    `introduction` = VALUES(`introduction`),
    `description` = VALUES(`description`),
    `pic_url` = VALUES(`pic_url`),
    `slider_pic_urls` = VALUES(`slider_pic_urls`),
    `type` = VALUES(`type`),
    `price` = VALUES(`price`),
    `market_price` = VALUES(`market_price`),
    `stock` = VALUES(`stock`),
    `sales_count` = VALUES(`sales_count`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `create_time` = VALUES(`create_time`),
    `update_time` = NOW(),
    `deleted` = b'0';

DELETE FROM `product_sku`
 WHERE `spu_id` BETWEEN 1 AND 13
   AND `id` NOT BETWEEN 1 AND 13;

INSERT INTO `product_sku`
    (`id`, `spu_id`, `properties`, `price`, `market_price`, `stock`, `pic_url`,
     `weight`, `volume`, `deleted`)
VALUES
    (1, 1, '[{"id":1,"name":"规格","valueId":1,"valueName":"250g"}]', 9990, 12800, 120, 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600', 0.25, 0.001, b'0'),
    (2, 2, '[{"id":1,"name":"规格","valueId":2,"valueName":"100g"}]', 2450, 3200, 150, 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=600', 0.10, 0.001, b'0'),
    (3, 3, '[{"id":1,"name":"规格","valueId":3,"valueName":"整枝"}]', 19900, 25800, 60, 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600', 0.30, 0.002, b'0'),
    (4, 4, '[{"id":1,"name":"规格","valueId":4,"valueName":"50g"}]', 15950, 19800, 45, 'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600', 0.05, 0.001, b'0'),
    (5, 5, '[{"id":1,"name":"规格","valueId":5,"valueName":"500g"}]', 3990, 5200, 200, 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600', 0.50, 0.002, b'0'),
    (6, 6, '[{"id":1,"name":"规格","valueId":6,"valueName":"30g"}]', 2800, 3800, 180, 'https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=600', 0.03, 0.001, b'0'),
    (7, 7, '[{"id":1,"name":"规格","valueId":7,"valueName":"20朵"}]', 3200, 4200, 120, 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600', 0.04, 0.001, b'0'),
    (8, 8, '[{"id":1,"name":"规格","valueId":8,"valueName":"10丸"}]', 3990, 5200, 160, 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=600', 0.15, 0.001, b'0'),
    (9, 9, '[{"id":1,"name":"规格","valueId":9,"valueName":"250g"}]', 4500, 5800, 100, 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600', 0.25, 0.001, b'0'),
    (10, 10, '[{"id":1,"name":"规格","valueId":10,"valueName":"32片"}]', 3180, 4200, 250, 'https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=600', 0.05, 0.001, b'0'),
    (11, 11, '[{"id":1,"name":"规格","valueId":11,"valueName":"60片"}]', 2990, 3900, 200, 'https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=600', 0.08, 0.001, b'0'),
    (12, 12, '[{"id":1,"name":"规格","valueId":12,"valueName":"3包"}]', 4500, 6200, 80, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600', 0.20, 0.001, b'0'),
    (13, 13, '[{"id":1,"name":"规格","valueId":13,"valueName":"100g"}]', 3500, 4600, 110, 'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?w=600', 0.10, 0.001, b'0')
ON DUPLICATE KEY UPDATE
    `spu_id` = VALUES(`spu_id`),
    `properties` = VALUES(`properties`),
    `price` = VALUES(`price`),
    `market_price` = VALUES(`market_price`),
    `stock` = VALUES(`stock`),
    `pic_url` = VALUES(`pic_url`),
    `weight` = VALUES(`weight`),
    `volume` = VALUES(`volume`),
    `update_time` = NOW(),
    `deleted` = b'0';

INSERT INTO `content_banner`
    (`id`, `title`, `pic_url`, `url`, `sort`, `status`, `deleted`)
VALUES
    (110001, '东阿阿胶糕滋补礼盒', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=1200', '/pages/goods/goods?id=1', 500, 1, b'0'),
    (110002, '四季花茶清润上新', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=1200', '/pages/topic/topic?id=110031', 490, 1, b'0')
ON DUPLICATE KEY UPDATE
    `title` = VALUES(`title`),
    `pic_url` = VALUES(`pic_url`),
    `url` = VALUES(`url`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

INSERT INTO `content_channel`
    (`id`, `name`, `icon_url`, `url`, `sort`, `status`, `deleted`)
VALUES
    (110011, '新品首发', 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=160', '/pages/newGoods/newGoods', 500, 1, b'0'),
    (110012, '热销爆款', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=160', '/pages/hotGoods/hotGoods', 490, 1, b'0'),
    (110013, '全部分类', 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=160', '/pages/catalog/catalog', 480, 1, b'0')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `icon_url` = VALUES(`icon_url`),
    `url` = VALUES(`url`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

INSERT INTO `content_brand`
    (`id`, `name`, `pic_url`, `floor_price`, `sort`, `status`, `deleted`)
VALUES
    (110021, '东阿阿胶', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=900', 9990, 500, 1, b'0'),
    (110022, '同仁堂', 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=900', 2450, 490, 1, b'0'),
    (110023, '药膳严选', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=900', 3500, 480, 1, b'0')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `pic_url` = VALUES(`pic_url`),
    `floor_price` = VALUES(`floor_price`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

INSERT INTO `content_topic`
    (`id`, `title`, `subtitle`, `pic_url`, `price_info`, `sort`, `status`, `deleted`)
VALUES
    (110031, '药食同源养生指南', '滋补、茶饮、药膳食材一站式搭配', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=900', '24.5元起', 500, 1, b'0'),
    (110032, '四季花茶搭配', '玫瑰、胎菊与枸杞的清润组合', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=900', '28元起', 490, 1, b'0')
ON DUPLICATE KEY UPDATE
    `title` = VALUES(`title`),
    `subtitle` = VALUES(`subtitle`),
    `pic_url` = VALUES(`pic_url`),
    `price_info` = VALUES(`price_info`),
    `sort` = VALUES(`sort`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

INSERT INTO `content_topic_product`
    (`topic_id`, `spu_id`, `sort`)
VALUES
    (110031, 1, 500),
    (110031, 3, 490),
    (110031, 12, 480),
    (110031, 13, 470),
    (110032, 2, 500),
    (110032, 6, 490),
    (110032, 7, 480)
ON DUPLICATE KEY UPDATE
    `sort` = VALUES(`sort`);

INSERT INTO `member_user`
    (`id`, `openid`, `mobile`, `nickname`, `avatar`, `status`, `deleted`)
VALUES
    (110101, 'frontend_seed_reviewer_110101', '13800001101', '养生体验官', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160', 1, b'0')
ON DUPLICATE KEY UPDATE
    `openid` = VALUES(`openid`),
    `mobile` = VALUES(`mobile`),
    `nickname` = VALUES(`nickname`),
    `avatar` = VALUES(`avatar`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

INSERT INTO `product_comment`
    (`id`, `user_id`, `spu_id`, `content`, `status`, `deleted`)
VALUES
    (110201, 110101, 1, '阿胶糕口感细腻，甜度刚好，礼盒送人也体面。', 1, b'0'),
    (110202, 110101, 6, '玫瑰花香很自然，日常泡水颜色也漂亮。', 1, b'0')
ON DUPLICATE KEY UPDATE
    `user_id` = VALUES(`user_id`),
    `spu_id` = VALUES(`spu_id`),
    `content` = VALUES(`content`),
    `status` = VALUES(`status`),
    `deleted` = b'0';

COMMIT;
