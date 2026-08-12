DROP PROCEDURE IF EXISTS migration_add_admin_avatar_if_missing;

DELIMITER $$

CREATE PROCEDURE migration_add_admin_avatar_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'sys_admin_user'
           AND column_name = 'avatar'
    ) THEN
        ALTER TABLE `sys_admin_user`
        ADD COLUMN `avatar` varchar(512) NOT NULL DEFAULT '' COMMENT '头像地址' AFTER `nickname`;
    END IF;
END$$

DELIMITER ;

CALL migration_add_admin_avatar_if_missing();
DROP PROCEDURE migration_add_admin_avatar_if_missing;
