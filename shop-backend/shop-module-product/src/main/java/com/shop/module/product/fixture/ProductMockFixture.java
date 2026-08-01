package com.shop.module.product.fixture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 商品模块可复现的 Mock 只读种子。 */
public final class ProductMockFixture {
    public static final List<Map<String, Object>> GOODS = initGoods();
    public static final Map<Long, String> CATEGORY_BANNERS = Map.of(
            1L, "https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600&auto=format&fit=crop",
            2L, "https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=600&auto=format&fit=crop",
            3L, "https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=600&auto=format&fit=crop",
            4L, "https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=600&auto=format&fit=crop",
            5L, "https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=600&auto=format&fit=crop");
    public static final Map<Long, String> CATEGORY_NAMES = Map.of(
            1L, "滋补养生", 2L, "茶饮花茶", 3L, "零食坚果", 4L, "保健食品", 5L, "药膳食材");

    private ProductMockFixture() {
    }

    public static Map<String, Object> requireGoods(long id) {
        return GOODS.stream().filter(item -> ((Number) item.get("id")).longValue() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mock 商品不存在: " + id));
    }

    private static List<Map<String, Object>> initGoods() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(goods(1, 1, "东阿阿胶糕", "补气养血，美容养颜，传统手工熬制", "99.90", "https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=400", 1, 1));
        list.add(goods(2, 1, "同仁堂枸杞", "宁夏特级免洗枸杞，粒大饱满，甘甜可口", "24.50", "https://images.unsplash.com/photo-1509358271058-acd22cc93898?w=400", 0, 1));
        list.add(goods(3, 1, "长白山人参", "整枝鲜参，长白山道地直供，元气满满", "199.00", "https://images.unsplash.com/photo-1514733670139-4d87a19b179d?w=400", 1, 0));
        list.add(goods(4, 1, "铁皮石斛", "正宗霍山铁皮石斛，胶质浓郁，养阴清热", "159.50", "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?w=400", 0, 0));
        list.add(goods(5, 1, "百花蜂蜜", "农家天然土蜂蜜，质地浓稠，蜜香浓郁", "39.90", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400", 1, 0));
        list.add(goods(6, 2, "金边玫瑰花茶", "云南墨红玫瑰，花香浓郁，疏肝理气", "28.00", "https://images.unsplash.com/photo-1506084868230-bb9d95c24759?w=400", 1, 1));
        list.add(goods(7, 2, "胎菊王菊花茶", "桐乡特级胎菊，清热明目，汤色金黄", "32.00", "https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=400", 0, 1));
        list.add(goods(8, 3, "有机黑芝麻丸", "九蒸九晒黑芝麻丸，软糯浓郁", "39.90", "https://images.unsplash.com/photo-1595855759920-86582396756a?w=400", 0, 1));
        list.add(goods(9, 3, "手剥夏威夷果", "大颗粒果仁，酥脆香甜", "45.00", "https://images.unsplash.com/photo-1534149711956-f9b7d528f64d?w=400", 1, 1));
        list.add(goods(10, 4, "江中健胃消食片", "消食导滞，日常肠胃养护", "31.80", "https://images.unsplash.com/photo-1607619056574-7b8f304b3c93?w=400", 0, 1));
        list.add(goods(11, 4, "天然维生素C片", "补充每日维生素 C", "29.90", "https://images.unsplash.com/photo-1616679911721-eff6eec18fcd?w=400", 1, 0));
        list.add(goods(12, 5, "经典当归补血汤料包", "传统当归黄芪配方，药膳煲汤", "45.00", "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400", 1, 1));
        list.add(goods(13, 5, "黄金草虫草花", "特级无根虫草花，汤汁鲜美", "35.00", "https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?w=400", 0, 1));
        return List.copyOf(list);
    }

    private static Map<String, Object> goods(long id, long categoryId, String name, String brief,
                                              String price, String picUrl, int isNew, int isHot) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("categoryId", categoryId);
        item.put("name", name);
        item.put("goodsBrief", brief);
        item.put("goodsDesc", "<p>" + brief + "</p>");
        item.put("retailPrice", price);
        item.put("counterPrice", price);
        item.put("listPicUrl", picUrl);
        item.put("isNew", isNew);
        item.put("isHot", isHot);
        item.put("sellVolume", isHot == 1 ? 100 : 0);
        return Map.copyOf(item);
    }
}
