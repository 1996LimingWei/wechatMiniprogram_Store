CREATE TABLE IF NOT EXISTS `trade_after_sale_item` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `after_sale_id` bigint NOT NULL COMMENT '售后单ID',
    `order_item_id` bigint NOT NULL COMMENT '订单明细ID',
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
    `goods_name` varchar(128) NOT NULL COMMENT '商品名称快照',
    `spec_name` varchar(128) NOT NULL DEFAULT '' COMMENT '规格快照',
    `price` int NOT NULL COMMENT '成交单价(分)',
    `apply_count` int NOT NULL COMMENT '申请售后数量',
    `refund_amount` int NOT NULL COMMENT '本明细退款金额(分)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_after_sale_order_item` (`after_sale_id`, `order_item_id`),
    KEY `idx_order_item_id` (`order_item_id`),
    KEY `idx_sku_id` (`sku_id`),
    CONSTRAINT `chk_after_sale_item_amount` CHECK (`price` > 0 AND `apply_count` > 0 AND `refund_amount` = `price` * `apply_count`)
) ENGINE=InnoDB COMMENT='售后商品明细表';

INSERT IGNORE INTO `trade_after_sale_item`
    (`after_sale_id`, `order_item_id`, `spu_id`, `sku_id`, `goods_name`, `spec_name`,
     `price`, `apply_count`, `refund_amount`)
SELECT a.`id`, oi.`id`, oi.`spu_id`, oi.`sku_id`, oi.`goods_name`, oi.`spec_name`,
       oi.`price`, oi.`count`, oi.`total_price`
  FROM `trade_after_sale` a
  JOIN `trade_order_item` oi ON oi.`order_id` = a.`order_id` AND oi.`deleted` = b'0'
 WHERE a.`deleted` = b'0';
