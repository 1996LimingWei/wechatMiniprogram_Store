SET NAMES utf8mb4;

-- 正式首页入口已经由 V20260811_02 统一维护，停用早期演示迁移留下的同路由重复入口。
UPDATE `content_channel` AS legacy
INNER JOIN `content_channel` AS canonical
    ON canonical.`id` IN (110011, 110012, 110013)
   AND canonical.`url` = legacy.`url`
   AND canonical.`status` = 1
   AND canonical.`deleted` = b'0'
SET legacy.`status` = 0,
    legacy.`deleted` = b'1'
WHERE legacy.`id` IN (1, 2, 3, 240211, 240212, 240213);
