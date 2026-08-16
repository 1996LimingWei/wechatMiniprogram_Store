INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`) VALUES
(9, 'system:admin-user', '管理后台账号', '/admin-api/system/admin-user/**', '*', 1),
(10, 'system:role', '管理角色权限', '/admin-api/system/role/**', '*', 1),
(11, 'system:permission', '查看权限清单', '/admin-api/system/permission/**', 'GET', 1),
(12, 'system:audit', '查看审计日志', '/admin-api/system/audit/**', 'GET', 1),
(13, 'system:password', '修改后台密码', '/admin-api/system/password/**', 'POST', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
    `http_method` = VALUES(`http_method`), `status` = 1, `deleted` = b'0';

INSERT INTO `sys_role` (`id`, `code`, `name`, `status`) VALUES
(2, 'PRODUCT_OPERATOR', '商品运营', 1),
(3, 'ORDER_CUSTOMER_SERVICE', '订单客服', 1),
(4, 'AFTER_SALE_REVIEWER', '售后审核', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `status` = 1, `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN' AND r.deleted = b'0' AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'PRODUCT_OPERATOR' AND p.code IN ('product:manage', 'content:manage', 'feedback:manage') AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'ORDER_CUSTOMER_SERVICE' AND p.code IN ('trade:manage') AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'AFTER_SALE_REVIEWER' AND p.code IN ('trade:manage') AND p.deleted = b'0';
