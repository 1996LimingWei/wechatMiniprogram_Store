CREATE TABLE IF NOT EXISTS `material_asset` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `url` varchar(1024) NOT NULL COMMENT '素材公开访问地址',
    `object_key` varchar(512) NOT NULL COMMENT '存储对象键',
    `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
    `content_type` varchar(100) NOT NULL COMMENT '文件 MIME 类型',
    `file_size` bigint NOT NULL COMMENT '文件大小，单位字节',
    `width` int DEFAULT NULL COMMENT '图片宽度',
    `height` int DEFAULT NULL COMMENT '图片高度',
    `biz_type` varchar(32) NOT NULL DEFAULT 'common' COMMENT '业务类型 common/product/content',
    `reference_count` int NOT NULL DEFAULT 0 COMMENT '业务引用数量',
    `created_by` bigint DEFAULT NULL COMMENT '上传管理员ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_object_key` (`object_key`),
    KEY `idx_biz_time` (`biz_type`, `create_time`, `id`),
    KEY `idx_deleted_time` (`deleted`, `create_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台素材库';

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES (20, 'material:manage', '管理素材库', '/admin-api/material/**', '*', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
    `http_method` = VALUES(`http_method`), `status` = 1, `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'SUPER_ADMIN' AND p.code = 'material:manage' AND p.deleted = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code = 'PRODUCT_OPERATOR' AND p.code = 'material:manage' AND p.deleted = b'0';
