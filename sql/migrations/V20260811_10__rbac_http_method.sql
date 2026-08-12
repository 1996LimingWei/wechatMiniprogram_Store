ALTER TABLE sys_permission
    ADD COLUMN http_method VARCHAR(16) NOT NULL DEFAULT '*' AFTER path_pattern;

UPDATE sys_permission SET http_method = 'GET' WHERE code = 'dashboard:view';

INSERT INTO sys_permission (id, code, name, path_pattern, http_method, status)
VALUES (7, 'auth:session', '管理自身后台会话', '/admin-api/auth/**', '*', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), path_pattern = VALUES(path_pattern),
    http_method = VALUES(http_method), status = 1, deleted = b'0';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT id, 7 FROM sys_role WHERE code = 'SUPER_ADMIN' AND deleted = b'0';
