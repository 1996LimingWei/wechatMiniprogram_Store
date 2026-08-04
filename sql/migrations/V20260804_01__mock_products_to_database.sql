-- 将原 Mock 商品合并到数据库，统一使用 database provider。
-- ID 5-13 为原 ProductMockFixture 中的商品，补充到 product_spu / product_sku。

INSERT INTO product_spu (id, category_id, name, keyword, introduction, description, pic_url, slider_pic_urls, type, price, market_price, stock, sales_count, sort, status, deleted) VALUES
(5,  5, '同仁堂枸杞',        '枸杞 滋补',       '宁夏特级免洗枸杞，粒大饱满，甘甜可口',         '<p>宁夏特级免洗枸杞，粒大饱满，甘甜可口。</p>', 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=600', '["https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=600"]', 1, 2450,  3200,  150, 210, 85, 1, b'0'),
(6,  5, '长白山人参',        '人参 滋补 鲜参',   '整枝鲜参，长白山道地直供，元气满满',           '<p>整枝鲜参，长白山道地直供，元气满满。</p>', 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600', '["https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600"]', 1, 19900, 25800, 60,  156, 95, 1, b'0'),
(7,  5, '铁皮石斛',          '石斛 滋补 霍山',   '正宗霍山铁皮石斛，胶质浓郁，养阴清热',         '<p>正宗霍山铁皮石斛，胶质浓郁，养阴清热。</p>', 'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600', '["https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600"]', 1, 15950, 19800, 45,  88,  75, 1, b'0'),
(8,  5, '百花蜂蜜',          '蜂蜜 天然 土蜂蜜', '农家天然土蜂蜜，质地浓稠，蜜香浓郁',           '<p>农家天然土蜂蜜，质地浓稠，蜜香浓郁。</p>', 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600', '["https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600"]', 1, 3990,  5200,  200, 320, 65, 1, b'0'),
(9,  6, '金边玫瑰花茶',      '玫瑰 花茶',        '云南墨红玫瑰，花香浓郁，疏肝理气',             '<p>云南墨红玫瑰，花香浓郁，疏肝理气。</p>', 'https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=600', '["https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=600"]', 1, 2800,  3800,  180, 264, 88, 1, b'0'),
(10, 6, '胎菊王菊花茶',      '菊花 胎菊 花茶',   '桐乡特级胎菊，清热明目，汤色金黄',             '<p>桐乡特级胎菊，清热明目，汤色金黄。</p>', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600', '["https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600"]', 1, 3200,  4200,  120, 198, 82, 1, b'0'),
(11, 240131, '有机黑芝麻丸',    '黑芝麻丸 零食',   '九蒸九晒黑芝麻丸，软糯浓郁',                   '<p>九蒸九晒黑芝麻丸，软糯浓郁。</p>', 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=600', '["https://images.unsplash.com/photo-1595855759920-86582396756a?w=600"]', 1, 3990,  5200,  160, 280, 78, 1, b'0'),
(12, 240131, '手剥夏威夷果',    '夏威夷果 坚果',   '大颗粒果仁，酥脆香甜',                         '<p>大颗粒果仁，酥脆香甜。</p>', 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600', '["https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600"]', 1, 4500,  5800,  100, 176, 72, 1, b'0'),
(13, 7, '江中健胃消食片',    '消食片 保健',      '消食导滞，日常肠胃养护',                         '<p>消食导滞，日常肠胃养护。</p>', 'https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=600', '["https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=600"]', 1, 3180,  4200,  250, 410, 68, 1, b'0')
ON DUPLICATE KEY UPDATE name=VALUES(name), keyword=VALUES(keyword), introduction=VALUES(introduction), description=VALUES(description), pic_url=VALUES(pic_url), slider_pic_urls=VALUES(slider_pic_urls), price=VALUES(price), market_price=VALUES(market_price), stock=VALUES(stock), sales_count=VALUES(sales_count), sort=VALUES(sort), status=VALUES(status);

INSERT INTO product_sku (spu_id, properties, price, market_price, stock, pic_url, weight, deleted) VALUES
(5,  '[{"id":1,"name":"规格","valueId":5,"valueName":"100g"}]',  2450,  3200,  150, 'https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=600', 0.10, b'0'),
(6,  '[{"id":1,"name":"规格","valueId":6,"valueName":"整枝"}]',  19900, 25800, 60,  'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600', 0.30, b'0'),
(7,  '[{"id":1,"name":"规格","valueId":7,"valueName":"50g"}]',   15950, 19800, 45,  'https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=600', 0.05, b'0'),
(8,  '[{"id":1,"name":"规格","valueId":8,"valueName":"500g"}]',  3990,  5200,  200, 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600', 0.50, b'0'),
(9,  '[{"id":1,"name":"规格","valueId":9,"valueName":"30g"}]',   2800,  3800,  180, 'https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=600', 0.03, b'0'),
(10, '[{"id":1,"name":"规格","valueId":10,"valueName":"20朵"}]', 3200,  4200,  120, 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600', 0.04, b'0'),
(11, '[{"id":1,"name":"规格","valueId":11,"valueName":"10丸"}]', 3990,  5200,  160, 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=600', 0.15, b'0'),
(12, '[{"id":1,"name":"规格","valueId":12,"valueName":"250g"}]', 4500,  5800,  100, 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600', 0.25, b'0'),
(13, '[{"id":1,"name":"规格","valueId":13,"valueName":"32片"}]', 3180,  4200,  250, 'https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=600', 0.05, b'0');
