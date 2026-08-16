INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES (8, 'feedback:manage', '管理用户反馈', '/admin-api/feedback/**', '*', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
    `http_method` = VALUES(`http_method`), `status` = 1, `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT `id`, 8 FROM `sys_role` WHERE `code` = 'SUPER_ADMIN' AND `deleted` = b'0';
