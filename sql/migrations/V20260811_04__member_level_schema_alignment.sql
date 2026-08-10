SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migration_align_member_level;

DELIMITER $$

CREATE PROCEDURE migration_align_member_level()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'member_user'
          AND column_name = 'member_level'
    ) THEN
        ALTER TABLE `member_user`
            ADD COLUMN `member_level` tinyint NOT NULL DEFAULT 1
                COMMENT '会员等级 1=白银会员 2=黄金会员'
            AFTER `status`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'member_user'
          AND index_name = 'idx_mobile'
    ) THEN
        ALTER TABLE `member_user` ADD INDEX `idx_mobile` (`mobile`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'member_user'
          AND constraint_name = 'chk_member_user_level'
          AND constraint_type = 'CHECK'
    ) THEN
        ALTER TABLE `member_user`
            ADD CONSTRAINT `chk_member_user_level` CHECK (`member_level` IN (1, 2));
    END IF;
END$$

DELIMITER ;

CALL migration_align_member_level();
DROP PROCEDURE migration_align_member_level;
