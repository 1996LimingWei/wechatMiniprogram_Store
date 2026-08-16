ALTER TABLE `sys_operation_log`
    ADD COLUMN `business_ref` varchar(128) DEFAULT NULL COMMENT '脱敏业务关联编号' AFTER `request_uri`,
    ADD INDEX `idx_business_ref` (`business_ref`);

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`) VALUES
(14, 'trade:order-read', '查看订单列表', '/admin-api/trade/order/list', 'GET', 1),
(15, 'trade:order-detail', '查看订单详情', '/admin-api/trade/order/detail', 'GET', 1),
(16, 'trade:order-ship', '订单发货', '/admin-api/trade/order/ship', 'POST', 1),
(17, 'trade:logistics-read', '查看订单物流', '/admin-api/trade/logistics/**', 'GET', 1),
(18, 'trade:after-sale-read', '查看售后订单', '/admin-api/trade/after-sale/list', 'GET', 1),
(19, 'trade:after-sale-process', '处理售后订单', '/admin-api/trade/after-sale/**', 'POST', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
    `http_method` = VALUES(`http_method`), `status` = 1, `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN' AND p.id BETWEEN 14 AND 19 AND p.deleted = b'0';

DELETE rp FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.id = rp.role_id
JOIN `sys_permission` p ON p.id = rp.permission_id
WHERE r.code IN ('ORDER_CUSTOMER_SERVICE', 'AFTER_SALE_REVIEWER') AND p.code = 'trade:manage';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'ORDER_CUSTOMER_SERVICE'
   AND p.code IN ('auth:session', 'trade:order-read', 'trade:order-detail', 'trade:order-ship', 'trade:logistics-read')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'AFTER_SALE_REVIEWER'
   AND p.code IN ('auth:session', 'trade:after-sale-read', 'trade:after-sale-process')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'PRODUCT_OPERATOR' AND p.code = 'auth:session' AND p.deleted = b'0';
