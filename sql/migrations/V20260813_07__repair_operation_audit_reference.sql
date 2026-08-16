-- 修复早期错误迁移记录可能遗漏的操作审计业务关联字段。
SET @has_business_ref := (
    SELECT COUNT(*)
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'sys_operation_log'
       AND column_name = 'business_ref'
);
SET @add_business_ref_sql := IF(
    @has_business_ref = 0,
    'ALTER TABLE `sys_operation_log` ADD COLUMN `business_ref` varchar(128) DEFAULT NULL COMMENT ''脱敏业务关联编号'' AFTER `request_uri`',
    'SELECT 1'
);
PREPARE add_business_ref_statement FROM @add_business_ref_sql;
EXECUTE add_business_ref_statement;
DEALLOCATE PREPARE add_business_ref_statement;

SET @has_business_ref_index := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'sys_operation_log'
       AND index_name = 'idx_business_ref'
);
SET @add_business_ref_index_sql := IF(
    @has_business_ref_index = 0,
    'ALTER TABLE `sys_operation_log` ADD INDEX `idx_business_ref` (`business_ref`)',
    'SELECT 1'
);
PREPARE add_business_ref_index_statement FROM @add_business_ref_index_sql;
EXECUTE add_business_ref_index_statement;
DEALLOCATE PREPARE add_business_ref_index_statement;
