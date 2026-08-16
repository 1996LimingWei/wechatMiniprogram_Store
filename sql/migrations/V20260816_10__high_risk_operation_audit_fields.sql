SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migration_add_audit_column_if_missing;
DELIMITER //
CREATE PROCEDURE migration_add_audit_column_if_missing(
    IN table_name_param varchar(64),
    IN column_name_param varchar(64),
    IN column_definition_param text
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = table_name_param
           AND column_name = column_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD COLUMN `', column_name_param, '` ', column_definition_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL migration_add_audit_column_if_missing('sys_operation_log', 'admin_role_codes', "varchar(255) NOT NULL DEFAULT '' COMMENT '操作人角色编码快照' AFTER `admin_user_id`");
CALL migration_add_audit_column_if_missing('sys_operation_log', 'operation_type', "varchar(64) NOT NULL DEFAULT '' COMMENT '操作类型' AFTER `request_uri`");
CALL migration_add_audit_column_if_missing('sys_operation_log', 'high_risk', "tinyint NOT NULL DEFAULT 0 COMMENT '是否高风险操作' AFTER `operation_type`");
CALL migration_add_audit_column_if_missing('sys_operation_log', 'user_agent', "varchar(512) NOT NULL DEFAULT '' COMMENT '客户端 User-Agent' AFTER `ip`");
CALL migration_add_audit_column_if_missing('sys_operation_log', 'before_snapshot', "varchar(1024) NOT NULL DEFAULT '' COMMENT '变更前关键字段摘要' AFTER `message`");
CALL migration_add_audit_column_if_missing('sys_operation_log', 'after_snapshot', "varchar(1024) NOT NULL DEFAULT '' COMMENT '变更后关键字段摘要' AFTER `before_snapshot`");

DROP PROCEDURE IF EXISTS migration_add_audit_column_if_missing;

SET @has_operation_type_index := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'sys_operation_log'
       AND index_name = 'idx_operation_type_time'
);
SET @add_operation_type_index_sql := IF(
    @has_operation_type_index = 0,
    'ALTER TABLE `sys_operation_log` ADD INDEX `idx_operation_type_time` (`operation_type`, `create_time`, `id`)',
    'SELECT 1'
);
PREPARE add_operation_type_index_statement FROM @add_operation_type_index_sql;
EXECUTE add_operation_type_index_statement;
DEALLOCATE PREPARE add_operation_type_index_statement;

SET @has_high_risk_index := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'sys_operation_log'
       AND index_name = 'idx_high_risk_time'
);
SET @add_high_risk_index_sql := IF(
    @has_high_risk_index = 0,
    'ALTER TABLE `sys_operation_log` ADD INDEX `idx_high_risk_time` (`high_risk`, `create_time`, `id`)',
    'SELECT 1'
);
PREPARE add_high_risk_index_statement FROM @add_high_risk_index_sql;
EXECUTE add_high_risk_index_statement;
DEALLOCATE PREPARE add_high_risk_index_statement;
