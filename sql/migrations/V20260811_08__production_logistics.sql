-- 生产物流查询所需编码与缓存，避免高频调用供应商接口。
ALTER TABLE trade_order_logistics
    ADD COLUMN logistics_code VARCHAR(32) NOT NULL DEFAULT '' AFTER logistics_company,
    ADD COLUMN last_query_time DATETIME NULL AFTER delivery_time,
    ADD COLUMN traces_json TEXT NULL AFTER last_query_time,
    ADD COLUMN query_message VARCHAR(255) NULL AFTER traces_json;

UPDATE trade_order_logistics
SET logistics_code = CASE logistics_company
    WHEN '顺丰速运' THEN 'shunfeng'
    WHEN '中通快递' THEN 'zhongtong'
    WHEN '圆通速递' THEN 'yuantong'
    WHEN '韵达快递' THEN 'yunda'
    WHEN '极兔速递' THEN 'jtexpress'
    WHEN '申通快递' THEN 'shentong'
    WHEN '京东物流' THEN 'jd'
    WHEN '邮政 EMS' THEN 'ems'
    ELSE ''
END
WHERE logistics_code = '';
