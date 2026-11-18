-- 只更新指定旧品牌文案，既有迁移保持不可变。
UPDATE content_topic SET title = '陕铺养生指南' WHERE title = '药食同源养生指南';
