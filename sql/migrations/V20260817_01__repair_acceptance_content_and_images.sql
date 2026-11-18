-- 修复验收发现的早期演示内容乱码与已失效的外部图片地址。
-- 仅覆盖确定的演示记录和 3 个已返回 404 的 Unsplash 图片 ID，可重复执行。

SET NAMES utf8mb4;

START TRANSACTION;

UPDATE `content_banner`
   SET `title` = '滋补养生好物',
       `update_time` = NOW()
 WHERE `id` = 3
   AND `title` = '??????';

UPDATE `content_channel`
   SET `name` = '新品首发',
       `update_time` = NOW()
 WHERE `id` = 4
   AND `name` = '????';

UPDATE `content_brand`
   SET `name` = CASE `id`
       WHEN 3 THEN '东阿阿胶'
       WHEN 4 THEN '同仁堂'
       ELSE `name`
   END,
       `update_time` = NOW()
 WHERE `id` IN (3, 4)
   AND `name` LIKE '?%';

UPDATE `content_topic`
   SET `title` = CASE `id`
       WHEN 3 THEN '药食同源养生指南'
       WHEN 4 THEN '四季养生茶饮'
       ELSE `title`
   END,
       `subtitle` = CASE `id`
       WHEN 3 THEN '传统中医智慧，现代健康生活'
       WHEN 4 THEN '顺应时节，调养身心'
       ELSE `subtitle`
   END,
       `update_time` = NOW()
 WHERE `id` IN (3, 4)
   AND `title` LIKE '?%';

UPDATE `product_category`
   SET `icon` = REPLACE(REPLACE(REPLACE(
       `icon`,
       'photo-1534149711956-f9b7d528f64d', 'photo-1595855759920-86582396756a'),
       'photo-1514733670139-4d87a19b179d', 'photo-1540420773420-3366772f4999'),
       'photo-1607619056574-7b8f304b3c93', 'photo-1616679911721-eff6eec18fcd'),
       `update_time` = NOW()
 WHERE `icon` REGEXP 'photo-(1534149711956-f9b7d528f64d|1514733670139-4d87a19b179d|1607619056574-7b8f304b3c93)';

UPDATE `product_spu`
   SET `pic_url` = REPLACE(REPLACE(REPLACE(
       `pic_url`,
       'photo-1534149711956-f9b7d528f64d', 'photo-1595855759920-86582396756a'),
       'photo-1514733670139-4d87a19b179d', 'photo-1540420773420-3366772f4999'),
       'photo-1607619056574-7b8f304b3c93', 'photo-1616679911721-eff6eec18fcd'),
       `slider_pic_urls` = REPLACE(REPLACE(REPLACE(
       `slider_pic_urls`,
       'photo-1534149711956-f9b7d528f64d', 'photo-1595855759920-86582396756a'),
       'photo-1514733670139-4d87a19b179d', 'photo-1540420773420-3366772f4999'),
       'photo-1607619056574-7b8f304b3c93', 'photo-1616679911721-eff6eec18fcd'),
       `update_time` = NOW()
 WHERE CONCAT(`pic_url`, ' ', `slider_pic_urls`) REGEXP 'photo-(1534149711956-f9b7d528f64d|1514733670139-4d87a19b179d|1607619056574-7b8f304b3c93)';

UPDATE `product_sku`
   SET `pic_url` = REPLACE(REPLACE(REPLACE(
       `pic_url`,
       'photo-1534149711956-f9b7d528f64d', 'photo-1595855759920-86582396756a'),
       'photo-1514733670139-4d87a19b179d', 'photo-1540420773420-3366772f4999'),
       'photo-1607619056574-7b8f304b3c93', 'photo-1616679911721-eff6eec18fcd'),
       `update_time` = NOW()
 WHERE `pic_url` REGEXP 'photo-(1534149711956-f9b7d528f64d|1514733670139-4d87a19b179d|1607619056574-7b8f304b3c93)';

UPDATE `content_channel`
   SET `icon_url` = REPLACE(REPLACE(REPLACE(
       `icon_url`,
       'photo-1534149711956-f9b7d528f64d', 'photo-1595855759920-86582396756a'),
       'photo-1514733670139-4d87a19b179d', 'photo-1540420773420-3366772f4999'),
       'photo-1607619056574-7b8f304b3c93', 'photo-1616679911721-eff6eec18fcd'),
       `update_time` = NOW()
 WHERE `icon_url` REGEXP 'photo-(1534149711956-f9b7d528f64d|1514733670139-4d87a19b179d|1607619056574-7b8f304b3c93)';

COMMIT;
