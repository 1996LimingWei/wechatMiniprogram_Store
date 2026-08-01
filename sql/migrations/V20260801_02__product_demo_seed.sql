-- Issue #24：稳定、可重放的商品与内容演示种子。

INSERT INTO product_category (id, parent_id, name, icon, sort, status, deleted) VALUES
(240101, 0, '药食滋补', 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=160', 240, 1, b'0'),
(240102, 0, '健康茶饮', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=160', 230, 1, b'0'),
(240103, 0, '营养食品', 'https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=160', 220, 1, b'0'),
(240111, 240101, '胶类滋补', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=160', 240, 1, b'0'),
(240112, 240101, '药膳食材', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=160', 230, 1, b'0'),
(240121, 240102, '花果茶', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=160', 240, 1, b'0'),
(240131, 240103, '轻养零食', 'https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=160', 240, 1, b'0')
ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id), name=VALUES(name), icon=VALUES(icon), sort=VALUES(sort), status=VALUES(status), deleted=b'0';

INSERT INTO product_spu (id, category_id, name, keyword, introduction, description, pic_url, slider_pic_urls, type, price, market_price, stock, sales_count, sort, status, deleted) VALUES
(240001, 240111, '玫瑰阿胶糕礼盒', '阿胶 玫瑰 礼盒 多规格', '东阿阿胶、核桃与玫瑰花瓣，软糯独立装', '<p>玫瑰阿胶糕，红蓝礼盒与两种净含量可选。</p>', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600', '["https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600","https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600"]', 1, 12800, 16800, 16, 520, 240, 1, b'0'),
(240002, 240112, '长白山西洋参切片', '西洋参 人参 切片 缺货', '原枝切片，参香浓郁；演示全部缺货状态', '<p>长白山西洋参切片，当前批次售罄。</p>', 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600', '["https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600"]', 1, 8800, 10800, 0, 188, 220, 1, b'0'),
(240003, 240121, '金边玫瑰枸杞茶', '玫瑰 枸杞 花茶 热销', '云南金边玫瑰搭配宁夏枸杞，花香清甜', '<p>花果茶独立袋装，杯泡方便。</p>', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600', '["https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600"]', 1, 3900, 5200, 50, 860, 230, 1, b'0'),
(240004, 240131, '九蒸九晒黑芝麻丸', '黑芝麻丸 新品 轻养零食', '黑芝麻与黑豆低糖配方，软糯醇香', '<p>每日两丸，独立包装。</p>', 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=600', '["https://images.unsplash.com/photo-1595855759920-86582396756a?w=600"]', 1, 4200, 5600, 40, 36, 250, 1, b'0'),
(240005, 240112, '岷县当归黄芪汤包', '当归 黄芪 药膳 汤包', '当归与黄芪科学配比，家庭煲汤装', '<p>道地药膳食材，建议每周煲汤一至两次。</p>', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600', '["https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600"]', 1, 4600, 6200, 30, 128, 210, 1, b'0'),
(240006, 240121, '桂圆红枣姜茶试制装', '桂圆 红枣 姜茶 下架', '配方调整中的演示下架商品', '<p>该商品仅用于验证下架过滤。</p>', 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600', '["https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600"]', 1, 2900, 3900, 10, 12, 200, 0, b'0')
ON DUPLICATE KEY UPDATE category_id=VALUES(category_id), name=VALUES(name), keyword=VALUES(keyword), introduction=VALUES(introduction), description=VALUES(description), pic_url=VALUES(pic_url), slider_pic_urls=VALUES(slider_pic_urls), type=VALUES(type), price=VALUES(price), market_price=VALUES(market_price), stock=VALUES(stock), sales_count=VALUES(sales_count), sort=VALUES(sort), status=VALUES(status), deleted=b'0';

INSERT INTO product_sku (id, spu_id, properties, price, market_price, stock, pic_url, weight, deleted) VALUES
(240011, 240001, '[{"id":10,"name":"净含量","valueId":101,"valueName":"250g"},{"id":20,"name":"礼盒颜色","valueId":201,"valueName":"玫瑰红"}]', 12800, 16800, 8, 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600', 0.25, b'0'),
(240012, 240001, '[{"id":10,"name":"净含量","valueId":101,"valueName":"250g"},{"id":20,"name":"礼盒颜色","valueId":202,"valueName":"黛青蓝"}]', 13200, 17200, 0, 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600', 0.25, b'0'),
(240013, 240001, '[{"id":10,"name":"净含量","valueId":102,"valueName":"500g"},{"id":20,"name":"礼盒颜色","valueId":201,"valueName":"玫瑰红"}]', 14800, 18800, 5, 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=600', 0.50, b'0'),
(240014, 240001, '[{"id":10,"name":"净含量","valueId":102,"valueName":"500g"},{"id":20,"name":"礼盒颜色","valueId":202,"valueName":"黛青蓝"}]', 15200, 19200, 3, 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600', 0.50, b'0'),
(240021, 240002, '[{"id":10,"name":"净含量","valueId":101,"valueName":"100g"}]', 8800, 10800, 0, 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600', 0.10, b'0'),
(240022, 240002, '[{"id":10,"name":"净含量","valueId":102,"valueName":"200g"}]', 15800, 18800, 0, 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600', 0.20, b'0'),
(240031, 240003, '[{"id":10,"name":"规格","valueId":101,"valueName":"20袋"}]', 3900, 5200, 50, 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600', 0.12, b'0'),
(240041, 240004, '[{"id":10,"name":"规格","valueId":101,"valueName":"30丸"}]', 4200, 5600, 40, 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=600', 0.30, b'0'),
(240051, 240005, '[{"id":10,"name":"规格","valueId":101,"valueName":"5包"}]', 4600, 6200, 30, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600', 0.25, b'0'),
(240061, 240006, '[{"id":10,"name":"规格","valueId":101,"valueName":"10袋"}]', 2900, 3900, 10, 'https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600', 0.15, b'0')
ON DUPLICATE KEY UPDATE spu_id=VALUES(spu_id), properties=VALUES(properties), price=VALUES(price), market_price=VALUES(market_price), stock=VALUES(stock), pic_url=VALUES(pic_url), weight=VALUES(weight), deleted=b'0';

INSERT INTO content_banner (id, title, pic_url, url, sort, status, deleted) VALUES
(240201, '玫瑰阿胶糕多规格礼盒', 'https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=1200', '/pages/goods/goods?id=240001', 240, 1, b'0'),
(240202, '四季花果茶清润专题', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=1200', '/pages/topic/topic?id=240231', 230, 1, b'0')
ON DUPLICATE KEY UPDATE title=VALUES(title), pic_url=VALUES(pic_url), url=VALUES(url), sort=VALUES(sort), status=VALUES(status), deleted=b'0';

INSERT INTO content_channel (id, name, icon_url, url, sort, status, deleted) VALUES
(240211, '演示新品', 'https://images.unsplash.com/photo-1595855759920-86582396756a?w=160', '/pages/newGoods/newGoods', 240, 1, b'0'),
(240212, '演示热销', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=160', '/pages/hotGoods/hotGoods', 230, 1, b'0'),
(240213, '药食分类', 'https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=160', '/pages/catalog/catalog', 220, 1, b'0')
ON DUPLICATE KEY UPDATE name=VALUES(name), icon_url=VALUES(icon_url), url=VALUES(url), sort=VALUES(sort), status=VALUES(status), deleted=b'0';

INSERT INTO content_brand (id, name, pic_url, floor_price, sort, status, deleted) VALUES
(240221, '道地药食严选', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600', 4600, 240, 1, b'0'),
(240222, '清润茶坊', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600', 3900, 230, 1, b'0')
ON DUPLICATE KEY UPDATE name=VALUES(name), pic_url=VALUES(pic_url), floor_price=VALUES(floor_price), sort=VALUES(sort), status=VALUES(status), deleted=b'0';

INSERT INTO content_topic (id, title, subtitle, pic_url, price_info, sort, status, deleted) VALUES
(240231, '四季花果茶搭配指南', '从玫瑰、枸杞到红枣，按季节选择清润茶饮', 'https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=900', '39元起', 240, 1, b'0'),
(240232, '家庭药膳煲汤入门', '当归黄芪的日常搭配与用量建议', 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=900', '46元起', 230, 1, b'0')
ON DUPLICATE KEY UPDATE title=VALUES(title), subtitle=VALUES(subtitle), pic_url=VALUES(pic_url), price_info=VALUES(price_info), sort=VALUES(sort), status=VALUES(status), deleted=b'0';

INSERT INTO member_user (id, openid, nickname, avatar, status, deleted) VALUES
(240301, 'demo_seed_reviewer_240301', '演示体验官', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=160', 1, b'0')
ON DUPLICATE KEY UPDATE openid=VALUES(openid), nickname=VALUES(nickname), avatar=VALUES(avatar), status=VALUES(status), deleted=b'0';

INSERT INTO product_comment (id, user_id, spu_id, content, status, deleted) VALUES
(240401, 240301, 240001, '玫瑰香气自然，礼盒图片与实物风格一致，多规格切换清楚。', 1, b'0')
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), spu_id=VALUES(spu_id), content=VALUES(content), status=VALUES(status), deleted=b'0';
