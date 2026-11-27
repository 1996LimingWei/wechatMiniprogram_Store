SET NAMES utf8mb4;

-- 单品运费规则：两个字段都为空时跟随全局包邮规则；
-- 任一字段有值时该商品使用自己的规则，一单多商品取候选运费的最大值，避免运费叠加。
DROP PROCEDURE IF EXISTS migration_add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE migration_add_column_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE statement FROM @ddl;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END$$
DELIMITER ;

CALL migration_add_column_if_missing('product_spu', 'freight_base_fee',
    "int DEFAULT NULL COMMENT '单品基础运费(分)，NULL=跟随全局' AFTER `stock`");
CALL migration_add_column_if_missing('product_spu', 'freight_free_threshold',
    "int DEFAULT NULL COMMENT '单品包邮门槛(分)，NULL=跟随全局' AFTER `freight_base_fee`");

DROP PROCEDURE IF EXISTS migration_add_column_if_missing;
