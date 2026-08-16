SET NAMES utf8mb4;

INSERT INTO `sys_role` (`id`, `code`, `name`, `status`)
VALUES (6, 'READONLY', '只读账号', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 1, `deleted` = b'0';

UPDATE `sys_permission`
   SET `name` = '导出订单',
       `path_pattern` = '/admin-api/trade/order/export',
       `http_method` = 'GET',
       `status` = 1,
       `deleted` = b'0'
 WHERE `code` = 'trade:order-export';

UPDATE `sys_permission`
   SET `name` = '批量发货',
       `path_pattern` = '/admin-api/trade/order/batch-ship/**',
       `http_method` = '*',
       `status` = 1,
       `deleted` = b'0'
 WHERE `code` = 'trade:order-batch-ship';

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES
(48, 'trade:order-delivery-note', '打印发货单', '/admin-api/trade/order/delivery-note', 'GET', 1),
(49, 'trade:order-picking-list', '打印拣货单', '/admin-api/trade/order/picking-list', 'GET', 1),
(50, 'product:read', '查看商品、分类、库存和评论', '/admin-api/product/**', 'GET', 1),
(51, 'content:read', '查看运营内容', '/admin-api/content/**', 'GET', 1),
(52, 'material:read', '查看素材库', '/admin-api/material/**', 'GET', 1),
(53, 'marketing:manage', '管理营销规则', '/admin-api/marketing/**', '*', 1),
(54, 'marketing:read', '查看营销规则', '/admin-api/marketing/**', 'GET', 1),
(55, 'feedback:read', '查看用户反馈', '/admin-api/feedback/**', 'GET', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
                        `http_method` = VALUES(`http_method`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN'
   AND p.code IN ('trade:order-delivery-note', 'trade:order-picking-list',
                  'product:read', 'content:read', 'material:read',
                  'marketing:manage', 'marketing:read', 'feedback:read')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'PRODUCT_OPERATOR'
   AND p.code IN ('product:manage', 'product:read', 'content:manage', 'content:read',
                  'material:manage', 'material:read', 'marketing:manage', 'marketing:read',
                  'feedback:manage', 'feedback:read', 'auth:session')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'ORDER_CUSTOMER_SERVICE'
   AND p.code IN ('auth:session', 'trade:order-read', 'trade:order-detail',
                  'trade:order-ship', 'trade:logistics-read', 'trade:order-export',
                  'trade:order-batch-ship', 'trade:order-remark',
                  'trade:order-delivery-note', 'trade:order-picking-list',
                  'trade:after-sale-read')
   AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'READONLY'
   AND p.code IN ('auth:session', 'dashboard:view',
                  'product:read', 'content:read', 'material:read', 'marketing:read',
                  'feedback:read', 'trade:order-read', 'trade:order-detail',
                  'trade:logistics-read', 'trade:after-sale-read',
                  'trade:payment-read', 'trade:refund-read', 'trade:reconcile-read')
   AND p.deleted = b'0';

DELETE rp FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.id = rp.role_id
JOIN `sys_permission` p ON p.id = rp.permission_id
WHERE r.code IN ('PRODUCT_OPERATOR', 'ORDER_CUSTOMER_SERVICE', 'AFTER_SALE_REVIEWER', 'READONLY')
  AND p.code IN ('trade:manage', 'trade:payment-sync', 'trade:payment-handle',
                 'trade:refund-handle', 'trade:reconcile-trigger',
                 'trade:reconcile-export', 'trade:reconcile-handle',
                 'trade:order-export-sensitive', 'system:admin-user', 'system:role');

DELETE rp FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.id = rp.role_id
JOIN `sys_permission` p ON p.id = rp.permission_id
WHERE r.code = 'FINANCE'
  AND p.code IN ('trade:order-ship', 'trade:order-batch-ship', 'trade:order-remark',
                 'product:manage', 'content:manage', 'material:manage', 'marketing:manage');

DELETE rp FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.id = rp.role_id
JOIN `sys_permission` p ON p.id = rp.permission_id
WHERE r.code = 'READONLY'
  AND ((p.http_method <> 'GET' AND p.code <> 'auth:session')
       OR p.code IN ('trade:order-export', 'trade:order-export-sensitive', 'trade:reconcile-export'));
