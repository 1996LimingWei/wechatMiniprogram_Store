-- 订单与售后管理演示数据，仅用于 local/dev 环境。
-- 脚本只清理 810000-839999 预留 ID 区间，可重复执行。

SET NAMES utf8mb4;

START TRANSACTION;

DELETE FROM `trade_order_log` WHERE `order_id` BETWEEN 810000 AND 819999;
DELETE FROM `trade_after_sale` WHERE `id` BETWEEN 830000 AND 839999;
DELETE FROM `trade_order_logistics` WHERE `id` BETWEEN 825000 AND 829999;
DELETE FROM `pay_order` WHERE `id` BETWEEN 820000 AND 824999;
DELETE FROM `trade_order_item` WHERE `id` BETWEEN 815000 AND 819999;
DELETE FROM `trade_order` WHERE `id` BETWEEN 810000 AND 814999;
DELETE FROM `member_user` WHERE `id` BETWEEN 810000 AND 819999;

INSERT INTO `member_user`
    (`id`, `openid`, `mobile`, `nickname`, `avatar`, `status`, `create_time`, `update_time`, `deleted`)
VALUES
    (810001, 'demo_admin_trade_001', '13800008101', '林小满', '', 1, NOW() - INTERVAL 90 DAY, NOW(), b'0'),
    (810002, 'demo_admin_trade_002', '13800008102', '周知夏', '', 1, NOW() - INTERVAL 75 DAY, NOW(), b'0'),
    (810003, 'demo_admin_trade_003', '13800008103', '陈青禾', '', 1, NOW() - INTERVAL 60 DAY, NOW(), b'0'),
    (810004, 'demo_admin_trade_004', '13800008104', '苏念安', '', 1, NOW() - INTERVAL 45 DAY, NOW(), b'0'),
    (810005, 'demo_admin_trade_005', '13800008105', '顾言川', '', 1, NOW() - INTERVAL 30 DAY, NOW(), b'0'),
    (810006, 'demo_admin_trade_006', '13800008106', '沈星遥', '', 1, NOW() - INTERVAL 15 DAY, NOW(), b'0');

INSERT INTO `trade_order`
    (`id`, `order_sn`, `request_id`, `user_id`, `status`, `pay_status`,
     `goods_price`, `freight_price`, `coupon_price`, `order_price`, `actual_price`,
     `consignee`, `mobile`, `full_region`, `address`, `pay_time`, `expire_time`,
     `close_time`, `close_reason`, `create_time`, `update_time`, `deleted`)
VALUES
    (810001, 'DEMO202608060001', 'demo-order-001', 810001, 0, 0, 2990, 0, 0, 2990, 2990,
     '林小满', '13800008101', '浙江省 杭州市 西湖区', '文三路 88 号', NULL, NOW() + INTERVAL 30 MINUTE,
     NULL, '', NOW() - INTERVAL 20 MINUTE, NOW(), b'0'),
    (810002, 'DEMO202608060002', 'demo-order-002', 810002, 1, 1, 2990, 0, 0, 2990, 2990,
     '周知夏', '13800008102', '江苏省 南京市 鼓楼区', '中山北路 16 号', NOW() - INTERVAL 2 HOUR, NULL,
     NULL, '', NOW() - INTERVAL 3 HOUR, NOW(), b'0'),
    (810003, 'DEMO202608060003', 'demo-order-003', 810003, 1, 1, 5980, 0, 500, 5480, 5480,
     '陈青禾', '13800008103', '广东省 深圳市 南山区', '科技园南路 9 号', NOW() - INTERVAL 6 HOUR, NULL,
     NULL, '', NOW() - INTERVAL 7 HOUR, NOW(), b'0'),
    (810004, 'DEMO202608060004', 'demo-order-004', 810004, 2, 1, 2990, 0, 0, 2990, 2990,
     '苏念安', '13800008104', '四川省 成都市 锦江区', '春熙路 26 号', NOW() - INTERVAL 1 DAY, NULL,
     NULL, '', NOW() - INTERVAL 1 DAY, NOW(), b'0'),
    (810005, 'DEMO202608060005', 'demo-order-005', 810005, 2, 1, 5980, 0, 0, 5980, 5980,
     '顾言川', '13800008105', '湖北省 武汉市 武昌区', '东湖路 77 号', NOW() - INTERVAL 2 DAY, NULL,
     NULL, '', NOW() - INTERVAL 2 DAY, NOW(), b'0'),
    (810006, 'DEMO202608060006', 'demo-order-006', 810006, 3, 1, 2990, 0, 0, 2990, 2990,
     '沈星遥', '13800008106', '北京市 北京市 朝阳区', '望京街 5 号', NOW() - INTERVAL 5 DAY, NULL,
     NULL, '', NOW() - INTERVAL 5 DAY, NOW(), b'0'),
    (810007, 'DEMO202608060007', 'demo-order-007', 810001, 4, 0, 2990, 0, 0, 2990, 2990,
     '林小满', '13800008101', '浙江省 杭州市 西湖区', '文三路 88 号', NULL, NOW() - INTERVAL 6 DAY,
     NOW() - INTERVAL 6 DAY, '用户取消订单', NOW() - INTERVAL 6 DAY, NOW(), b'0'),
    (810008, 'DEMO202608060008', 'demo-order-008', 810002, 5, 1, 2990, 0, 0, 2990, 2990,
     '周知夏', '13800008102', '江苏省 南京市 鼓楼区', '中山北路 16 号', NOW() - INTERVAL 1 DAY, NULL,
     NULL, '', NOW() - INTERVAL 1 DAY, NOW(), b'0'),
    (810009, 'DEMO202608060009', 'demo-order-009', 810003, 5, 1, 5980, 0, 0, 5980, 5980,
     '陈青禾', '13800008103', '广东省 深圳市 南山区', '科技园南路 9 号', NOW() - INTERVAL 3 DAY, NULL,
     NULL, '', NOW() - INTERVAL 3 DAY, NOW(), b'0'),
    (810010, 'DEMO202608060010', 'demo-order-010', 810004, 5, 1, 2990, 0, 0, 2990, 2990,
     '苏念安', '13800008104', '四川省 成都市 锦江区', '春熙路 26 号', NOW() - INTERVAL 2 DAY, NULL,
     NULL, '', NOW() - INTERVAL 2 DAY, NOW(), b'0'),
    (810011, 'DEMO202608060011', 'demo-order-011', 810005, 5, 2, 2990, 0, 0, 2990, 2990,
     '顾言川', '13800008105', '湖北省 武汉市 武昌区', '东湖路 77 号', NOW() - INTERVAL 8 DAY, NULL,
     NULL, '', NOW() - INTERVAL 8 DAY, NOW(), b'0'),
    (810012, 'DEMO202608060012', 'demo-order-012', 810006, 1, 1, 2990, 0, 0, 2990, 2990,
     '沈星遥', '13800008106', '北京市 北京市 朝阳区', '望京街 5 号', NOW() - INTERVAL 4 DAY, NULL,
     NULL, '', NOW() - INTERVAL 4 DAY, NOW(), b'0'),
    (810013, 'DEMO202608060013', 'demo-order-013', 810001, 2, 1, 2990, 0, 0, 2990, 2990,
     '林小满', '13800008101', '浙江省 杭州市 西湖区', '文三路 88 号', NOW() - INTERVAL 4 DAY, NULL,
     NULL, '', NOW() - INTERVAL 4 DAY, NOW(), b'0');

INSERT INTO `trade_order_item`
    (`id`, `order_id`, `user_id`, `spu_id`, `sku_id`, `goods_name`, `goods_pic_url`,
     `spec_name`, `price`, `count`, `total_price`, `create_time`, `update_time`, `deleted`)
SELECT 815000 + (`id` - 810000), `id`, `user_id`, 3, 3000000001,
       '药食同源养生组合装', 'https://example.com/admin-trade-test.png', '标准装',
       2990, CASE WHEN `goods_price` = 5980 THEN 2 ELSE 1 END, `goods_price`,
       `create_time`, NOW(), b'0'
  FROM `trade_order`
 WHERE `id` BETWEEN 810001 AND 810013;

INSERT INTO `pay_order`
    (`id`, `pay_sn`, `order_id`, `user_id`, `amount`, `channel`, `status`, `pay_time`,
     `create_time`, `update_time`, `deleted`)
SELECT 820000 + (`id` - 810000), CONCAT('P', `order_sn`), `id`, `user_id`, `actual_price`, 'mock',
       CASE WHEN `id` = 810001 THEN 0 WHEN `id` = 810007 THEN 2 WHEN `id` = 810011 THEN 3 ELSE 1 END,
       `pay_time`, `create_time`, NOW(), b'0'
  FROM `trade_order`
 WHERE `id` BETWEEN 810001 AND 810013;

INSERT INTO `trade_order_logistics`
    (`id`, `order_id`, `logistics_company`, `logistics_no`, `delivery_time`,
     `create_time`, `update_time`, `deleted`)
VALUES
    (825004, 810004, '顺丰速运', 'SFDEMO810004', NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 18 HOUR, NOW(), b'0'),
    (825005, 810005, '中通快递', 'ZTDEMO810005', NOW() - INTERVAL 36 HOUR, NOW() - INTERVAL 36 HOUR, NOW(), b'0'),
    (825006, 810006, '京东物流', 'JDDEMO810006', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY, NOW(), b'0'),
    (825009, 810009, '圆通速递', 'YTDEMO810009', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW(), b'0'),
    (825013, 810013, '邮政 EMS', 'EMSDEMO810013', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY, NOW(), b'0');

INSERT INTO `trade_after_sale`
    (`id`, `order_id`, `user_id`, `after_sale_sn`, `type`, `status`, `refund_amount`,
     `reason`, `apply_remark`, `before_order_status`, `reject_reason`, `refund_provider`,
     `provider_refund_no`, `refund_message`, `apply_time`, `audit_time`, `refund_time`,
     `reject_time`, `cancel_time`, `create_time`, `update_time`, `deleted`)
VALUES
    (830001, 810008, 810002, 'RDEMO202608060001', 1, 0, 2990,
     '商品不符合预期', '包装未拆封，希望申请退款', 1, '', '', '', '',
     NOW() - INTERVAL 2 HOUR, NULL, NULL, NULL, NULL, NOW() - INTERVAL 2 HOUR, NOW(), b'0'),
    (830002, 810009, 810003, 'RDEMO202608060002', 2, 0, 5980,
     '商品破损', '收到后发现外包装破损，申请退货退款', 2, '', '', '', '',
     NOW() - INTERVAL 3 HOUR, NULL, NULL, NULL, NULL, NOW() - INTERVAL 3 HOUR, NOW(), b'0'),
    (830003, 810010, 810004, 'RDEMO202608060003', 1, 4, 2990,
     '重复购买', '申请整单退款', 1, '', 'mock', 'MOCK-RDEMO202608060003', 'Mock 渠道正在处理',
     NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 20 HOUR, NULL, NULL, NULL, NOW() - INTERVAL 1 DAY, NOW(), b'0'),
    (830004, 810011, 810005, 'RDEMO202608060004', 1, 1, 2990,
     '临时不需要', '商品未发货', 1, '', 'mock', 'MOCK-RDEMO202608060004', 'Mock 退款成功',
     NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY, NULL, NULL,
     NOW() - INTERVAL 7 DAY, NOW(), b'0'),
    (830005, 810012, 810006, 'RDEMO202608060005', 1, 2, 2990,
     '价格变化', '希望按活动价退款', 1, '订单价格与下单时一致，不符合退款条件', '', '', '',
     NOW() - INTERVAL 3 DAY, NULL, NULL, NOW() - INTERVAL 2 DAY, NULL, NOW() - INTERVAL 3 DAY, NOW(), b'0'),
    (830006, 810013, 810001, 'RDEMO202608060006', 2, 3, 2990,
     '不想要了', '申请后决定继续保留商品', 2, '', '', '', '',
     NOW() - INTERVAL 2 DAY, NULL, NULL, NULL, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 2 DAY, NOW(), b'0');

INSERT INTO `trade_order_log`
    (`order_id`, `user_id`, `operator_type`, `operator_id`, `action`, `from_status`,
     `to_status`, `from_pay_status`, `to_pay_status`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT `id`, `user_id`, 'user', `user_id`, 'CREATE_ORDER', NULL, 0, NULL, 0,
       '提交演示订单', `create_time`, NOW(), b'0'
  FROM `trade_order` WHERE `id` BETWEEN 810001 AND 810013;

INSERT INTO `trade_order_log`
    (`order_id`, `user_id`, `operator_type`, `operator_id`, `action`, `from_status`,
     `to_status`, `from_pay_status`, `to_pay_status`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT `id`, `user_id`, 'system', 0, 'PAY_SUCCESS', 0, 1, 0, 1,
       'Mock 支付成功', `pay_time`, NOW(), b'0'
  FROM `trade_order`
 WHERE `id` BETWEEN 810001 AND 810013 AND `pay_time` IS NOT NULL;

INSERT INTO `trade_order_log`
    (`order_id`, `user_id`, `operator_type`, `operator_id`, `action`, `from_status`,
     `to_status`, `from_pay_status`, `to_pay_status`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT logistics.`order_id`, orders.`user_id`, 'admin', 1, 'SHIP_ORDER', 1, 2, 1, 1,
       CONCAT('物流公司：', logistics.`logistics_company`, '，物流单号：', logistics.`logistics_no`),
       logistics.`delivery_time`, NOW(), b'0'
  FROM `trade_order_logistics` logistics
  JOIN `trade_order` orders ON orders.`id` = logistics.`order_id`
 WHERE logistics.`id` BETWEEN 825000 AND 829999;

INSERT INTO `trade_order_log`
    (`order_id`, `user_id`, `operator_type`, `operator_id`, `action`, `from_status`,
     `to_status`, `from_pay_status`, `to_pay_status`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT afterSale.`order_id`, afterSale.`user_id`, 'user', afterSale.`user_id`, 'APPLY_AFTER_SALE',
       afterSale.`before_order_status`, 5, 1, 1, afterSale.`reason`, afterSale.`apply_time`, NOW(), b'0'
  FROM `trade_after_sale` afterSale WHERE afterSale.`id` BETWEEN 830000 AND 839999;

INSERT INTO `trade_order_log`
    (`order_id`, `user_id`, `operator_type`, `operator_id`, `action`, `from_status`,
     `to_status`, `from_pay_status`, `to_pay_status`, `remark`, `create_time`, `update_time`, `deleted`)
SELECT `order_id`, `user_id`, 'admin', 1,
       CASE `status` WHEN 1 THEN 'REFUND_SUCCESS' WHEN 2 THEN 'REJECT_AFTER_SALE'
                     WHEN 3 THEN 'CANCEL_AFTER_SALE' ELSE 'REFUND_PROCESSING' END,
       5, CASE WHEN `status` IN (2, 3) THEN `before_order_status` ELSE 5 END,
       1, CASE WHEN `status` = 1 THEN 2 ELSE 1 END,
       CASE WHEN `status` = 1 THEN `refund_message`
            WHEN `status` = 2 THEN `reject_reason`
            WHEN `status` = 3 THEN '用户撤销售后申请'
            ELSE `refund_message` END,
       COALESCE(`refund_time`, `reject_time`, `cancel_time`, `audit_time`), NOW(), b'0'
  FROM `trade_after_sale`
 WHERE `id` BETWEEN 830000 AND 839999 AND `status` IN (1, 2, 3, 4);

COMMIT;

SELECT `status`, COUNT(*) AS `order_count`
  FROM `trade_order` WHERE `id` BETWEEN 810000 AND 814999 GROUP BY `status` ORDER BY `status`;
SELECT `status`, COUNT(*) AS `after_sale_count`
  FROM `trade_after_sale` WHERE `id` BETWEEN 830000 AND 839999 GROUP BY `status` ORDER BY `status`;
