SET NAMES utf8mb4;

-- 订单履约状态：与 status（交易状态）分离，便于后台按备货-揽收-签收逐段流转，
-- 并让小程序按履约进度展示文案。
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

CALL migration_add_column_if_missing('trade_order', 'fulfillment_status',
    "tinyint DEFAULT NULL COMMENT '履约状态 0=待备货 1=备货中 2=待揽收 3=已发货 4=已签收 5=物流异常' AFTER `status`");
CALL migration_add_column_if_missing('trade_order', 'fulfillment_update_time',
    "datetime DEFAULT NULL COMMENT '履约状态最近更新时间' AFTER `fulfillment_status`");

DROP PROCEDURE IF EXISTS migration_add_column_if_missing;

-- 历史已支付订单按原交易状态回填，避免后台出现空履约状态
UPDATE trade_order
   SET fulfillment_status = CASE status
                                WHEN 1 THEN 0
                                WHEN 2 THEN 3
                                WHEN 3 THEN 4
                                ELSE NULL
                            END,
       fulfillment_update_time = NOW()
 WHERE fulfillment_status IS NULL
   AND pay_status = 1
   AND status IN (1, 2, 3);
