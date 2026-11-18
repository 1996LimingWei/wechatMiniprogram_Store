-- 修复本地演示数据导入时由客户端字符集不一致造成的中文乱码。
-- 可重复执行，仅覆盖预留演示数据区间和早期管理端测试商品快照。

SET NAMES utf8mb4;

START TRANSACTION;

UPDATE `product_spu`
   SET `name` = '管理端交易测试商品',
       `introduction` = '管理端交易测试',
       `description` = '<p>管理端交易测试商品。</p>',
       `update_time` = NOW()
 WHERE `id` = 3;

UPDATE `trade_order_item`
   SET `goods_name` = '管理端交易测试商品',
       `update_time` = NOW()
 WHERE `id` BETWEEN 13 AND 16;

UPDATE `member_user`
   SET `nickname` = CASE `id`
       WHEN 810001 THEN '林小满'
       WHEN 810002 THEN '周知夏'
       WHEN 810003 THEN '陈青禾'
       WHEN 810004 THEN '苏念安'
       WHEN 810005 THEN '顾言川'
       WHEN 810006 THEN '沈星遥'
       ELSE `nickname`
   END,
       `update_time` = NOW()
 WHERE `id` BETWEEN 810001 AND 810006;

UPDATE `trade_order`
   SET `consignee` = CASE `user_id`
       WHEN 810001 THEN '林小满'
       WHEN 810002 THEN '周知夏'
       WHEN 810003 THEN '陈青禾'
       WHEN 810004 THEN '苏念安'
       WHEN 810005 THEN '顾言川'
       WHEN 810006 THEN '沈星遥'
       ELSE `consignee`
   END,
       `full_region` = CASE `user_id`
       WHEN 810001 THEN '浙江省 杭州市 西湖区'
       WHEN 810002 THEN '江苏省 南京市 鼓楼区'
       WHEN 810003 THEN '广东省 深圳市 南山区'
       WHEN 810004 THEN '四川省 成都市 锦江区'
       WHEN 810005 THEN '湖北省 武汉市 武昌区'
       WHEN 810006 THEN '北京市 北京市 朝阳区'
       ELSE `full_region`
   END,
       `address` = CASE `user_id`
       WHEN 810001 THEN '文三路 88 号'
       WHEN 810002 THEN '中山北路 16 号'
       WHEN 810003 THEN '科技园南路 9 号'
       WHEN 810004 THEN '春熙路 26 号'
       WHEN 810005 THEN '东湖路 77 号'
       WHEN 810006 THEN '望京街 5 号'
       ELSE `address`
   END,
       `close_reason` = CASE WHEN `id` = 810007 THEN '用户取消订单' ELSE `close_reason` END,
       `update_time` = NOW()
 WHERE `id` BETWEEN 810001 AND 810013;

UPDATE `trade_order_item`
   SET `goods_name` = '药食同源养生组合装',
       `spec_name` = '标准装',
       `update_time` = NOW()
 WHERE `id` BETWEEN 815001 AND 815013;

UPDATE `trade_order_logistics`
   SET `logistics_company` = CASE `id`
       WHEN 825004 THEN '顺丰速运'
       WHEN 825005 THEN '中通快递'
       WHEN 825006 THEN '京东物流'
       WHEN 825009 THEN '圆通速递'
       WHEN 825013 THEN '邮政 EMS'
       ELSE `logistics_company`
   END,
       `update_time` = NOW()
 WHERE `id` BETWEEN 825000 AND 829999;

UPDATE `trade_after_sale`
   SET `reason` = CASE `id`
       WHEN 830001 THEN '商品不符合预期'
       WHEN 830002 THEN '商品破损'
       WHEN 830003 THEN '重复购买'
       WHEN 830004 THEN '临时不需要'
       WHEN 830005 THEN '价格变化'
       WHEN 830006 THEN '不想要了'
       ELSE `reason`
   END,
       `apply_remark` = CASE `id`
       WHEN 830001 THEN '包装未拆封，希望申请退款'
       WHEN 830002 THEN '收到后发现外包装破损，申请退货退款'
       WHEN 830003 THEN '申请整单退款'
       WHEN 830004 THEN '商品未发货'
       WHEN 830005 THEN '希望按活动价退款'
       WHEN 830006 THEN '申请后决定继续保留商品'
       ELSE `apply_remark`
   END,
       `reject_reason` = CASE WHEN `id` = 830005 THEN '订单价格与下单时一致，不符合退款条件' ELSE `reject_reason` END,
       `refund_message` = CASE `id`
       WHEN 830003 THEN 'Mock 渠道正在处理'
       WHEN 830004 THEN 'Mock 退款成功'
       ELSE `refund_message`
   END,
       `update_time` = NOW()
 WHERE `id` BETWEEN 830001 AND 830006;

UPDATE `trade_order_log`
   SET `remark` = CASE `action`
       WHEN 'CREATE_ORDER' THEN '提交演示订单'
       WHEN 'PAY_SUCCESS' THEN 'Mock 支付成功'
       WHEN 'APPLY_AFTER_SALE' THEN (
           SELECT afterSale.`reason`
             FROM `trade_after_sale` afterSale
            WHERE afterSale.`order_id` = `trade_order_log`.`order_id`
              AND afterSale.`deleted` = b'0'
            ORDER BY afterSale.`id` DESC
            LIMIT 1
       )
       WHEN 'REFUND_SUCCESS' THEN 'Mock 退款成功'
       WHEN 'REFUND_PROCESSING' THEN 'Mock 渠道正在处理'
       WHEN 'REJECT_AFTER_SALE' THEN '订单价格与下单时一致，不符合退款条件'
       WHEN 'CANCEL_AFTER_SALE' THEN '用户撤销售后申请'
       ELSE `remark`
   END,
       `update_time` = NOW()
 WHERE `order_id` BETWEEN 810001 AND 810013
   AND `action` IN (
       'CREATE_ORDER',
       'PAY_SUCCESS',
       'APPLY_AFTER_SALE',
       'REFUND_SUCCESS',
       'REFUND_PROCESSING',
       'REJECT_AFTER_SALE',
       'CANCEL_AFTER_SALE'
   );

UPDATE `trade_order_log` logs
  JOIN `trade_order_logistics` logistics ON logistics.`order_id` = logs.`order_id`
   SET logs.`remark` = CONCAT('物流公司：', logistics.`logistics_company`, '，物流单号：', logistics.`logistics_no`),
       logs.`update_time` = NOW()
 WHERE logs.`order_id` BETWEEN 810001 AND 810013
   AND logs.`action` = 'SHIP_ORDER';

COMMIT;
