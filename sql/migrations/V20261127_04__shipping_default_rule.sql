SET NAMES utf8mb4;

-- 把原先只写在 .env（trade.freight.free-threshold / base-fee）的默认运费迁入数据库，
-- 使运费完全由后台「营销 → 包邮规则」维护。
-- 仅当库中没有任何有效规则时插入，已在用的线上库不受影响。
INSERT INTO `marketing_shipping_rule` (`name`, `free_threshold`, `base_fee`, `status`)
SELECT '默认运费规则', 19900, 1000, 1
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM `marketing_shipping_rule` WHERE `deleted` = b'0'
   );
