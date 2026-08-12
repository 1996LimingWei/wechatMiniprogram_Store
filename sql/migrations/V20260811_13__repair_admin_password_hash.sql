-- 修复历史初始化脚本中长度不足、无法校验的默认管理员 BCrypt 哈希。
-- 仅匹配已知坏值，绝不覆盖企业自行设置的管理员密码。
UPDATE `sys_admin_user`
SET `password` = '$2a$10$5ajKqxodMJAf3NeshVkMa.0C2CRpXnzos8ylffU07tsRVq.F4q/fO',
    `failed_login_count` = 0,
    `locked_until` = NULL
WHERE `username` = 'admin'
  AND `password` = '$2a$10$YMpimV4T/3Cq.UoFqMFJ6eOPHoGRTnr9X8tJLBXvBL7Uh3LQFX6G'
  AND `deleted` = b'0';
