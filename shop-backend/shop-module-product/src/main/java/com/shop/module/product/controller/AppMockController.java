package com.shop.module.product.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 分类/专题/购物车等页面 mock 接口 - 开发阶段使用
 */
@RestController
public class AppMockController {

    private static final String IMG = MockData.CATEGORY_BANNERS.get(1L);

    private Map<String, Object> getGoodsFromList(long id) {
        return MockData.getGoodsById(id);
    }

    private Map<String, Object> buildCategory(Long id) {
        String name = getCategoryName(id);
        String bannerUrl = MockData.CATEGORY_BANNERS.getOrDefault(id, "https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=600");
        List<Map<String, Object>> subList = List.of(
                Map.of("id", id * 10 + 1, "name", name + "-精选", "wapBannerUrl", bannerUrl),
                Map.of("id", id * 10 + 2, "name", name + "-经典", "wapBannerUrl", bannerUrl),
                Map.of("id", id * 10 + 3, "name", name + "-新品", "wapBannerUrl", bannerUrl)
        );
        return Map.of("id", id, "name", name,
                "frontName", name + "精选好物",
                "wapBannerUrl", bannerUrl,
                "subCategoryList", subList);
    }

    @RequestMapping("/app-api/mock/catalog/index")
    public Map<String, Object> catalogIndex() {
        List<Map<String, Object>> categoryList = List.of(
                Map.of("id", 1, "name", "滋补养生"),
                Map.of("id", 2, "name", "茶饮花茶"),
                Map.of("id", 3, "name", "零食坚果"),
                Map.of("id", 4, "name", "保健食品"),
                Map.of("id", 5, "name", "药膳食材")
        );
        return ok(Map.of("categoryList", categoryList, "currentCategory", buildCategory(1L)));
    }

    @RequestMapping("/app-api/mock/catalog/current")
    public Map<String, Object> catalogCurrent(@RequestParam(value = "id", defaultValue = "1") Long id) {
        return ok(Map.of("currentCategory", buildCategory(id)));
    }

    @RequestMapping("/app-api/mock/catalog/{id}")
    public Map<String, Object> catalogDetail(@PathVariable Long id) {
        return ok(Map.of("currentCategory", buildCategory(id)));
    }

    @RequestMapping("/app-api/mock/goods/count")
    public Map<String, Object> goodsCount() {
        return ok(Map.of("goodsCount", 68));
    }

    @RequestMapping("/app-api/mock/goods/category")
    public Map<String, Object> goodsCategory(@RequestParam(value = "id", defaultValue = "1") Long id) {
        List<Map<String, Object>> allCats = List.of(
                Map.of("id", 1, "name", "滋补养生"),
                Map.of("id", 2, "name", "茶饮花茶"),
                Map.of("id", 3, "name", "零食坚果"),
                Map.of("id", 4, "name", "保健食品"),
                Map.of("id", 5, "name", "药膳食材")
        );
        Map<String, Object> current = Map.of("id", id, "name", getCategoryName(id),
                "frontName", getCategoryName(id) + "精选好物");
        return ok(Map.of("brotherCategory", allCats, "currentCategory", current));
    }

    @RequestMapping("/app-api/mock/goods/list")
    public Map<String, Object> goodsList(
            @RequestParam(value = "categoryId", defaultValue = "0") Long categoryId,
            @RequestParam(value = "brandId", defaultValue = "0") Long brandId,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            @RequestParam(value = "isHot", defaultValue = "0") int isHot,
            @RequestParam(value = "isNew", defaultValue = "0") int isNew,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "default") String sort,
            @RequestParam(value = "order", defaultValue = "desc") String order) {
        
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> g : MockData.GOODS_LIST) {
            boolean matches = true;
            if (categoryId != 0 && Long.valueOf(g.get("categoryId").toString()) != categoryId) {
                matches = false;
            }
            if (isNew == 1 && Integer.valueOf(g.get("isNew").toString()) != 1) {
                matches = false;
            }
            if (isHot == 1 && Integer.valueOf(g.get("isHot").toString()) != 1) {
                matches = false;
            }
            if (matches) {
                filtered.add(g);
            }
        }

        int total = filtered.size();
        int pages = (total + size - 1) / size;
        int fromIndex = Math.min((page - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        
        List<Map<String, Object>> records = filtered.subList(fromIndex, toIndex);

        List<Map<String, Object>> filterCategory = List.of(
                Map.of("id", 1, "name", "滋补养生", "checked", categoryId == 1),
                Map.of("id", 2, "name", "茶饮花茶", "checked", categoryId == 2),
                Map.of("id", 3, "name", "零食坚果", "checked", categoryId == 3),
                Map.of("id", 4, "name", "保健食品", "checked", categoryId == 4),
                Map.of("id", 5, "name", "药膳食材", "checked", categoryId == 5)
        );

        Map<String, Object> goodsListMap = Map.of(
                "records", records,
                "current", page,
                "size", size,
                "total", total,
                "pages", pages
        );
        return ok(Map.of("goodsList", goodsListMap, "filterCategory", filterCategory));
    }

    // =========== 热销/新品 ===========

    @RequestMapping("/app-api/goods/hot")
    public Map<String, Object> goodsHot() {
        Map<String, Object> bannerInfo = Map.of(
                "imgUrl", "https://images.unsplash.com/photo-1596701062351-8c2c14d1fdd0?w=750&auto=format&fit=crop",
                "name", "热销爆款"
        );
        return ok(Map.of("bannerInfo", bannerInfo));
    }

    @RequestMapping("/app-api/goods/new")
    public Map<String, Object> goodsNew() {
        Map<String, Object> bannerInfo = Map.of(
                "imgUrl", "https://images.unsplash.com/photo-1597481499750-3e6b22637e12?w=750&auto=format&fit=crop",
                "name", "新品推荐"
        );
        return ok(Map.of("bannerInfo", bannerInfo));
    }

    // =========== 商品详情 ===========

    @RequestMapping("/app-api/mock/goods/detail")
    public Map<String, Object> goodsDetail(@RequestParam(value = "id", defaultValue = "1") Long id) {
        String name = "药食同源好物 #" + id;
        String brief = "精选优质原料，传统工艺制作";
        double price = 29.9 + (id * 7.3) % 200;

        List<Map<String, Object>> gallery = List.of(
                Map.of("id", 1, "imgUrl", "https://picsum.photos/seed/dg1/600/600"),
                Map.of("id", 2, "imgUrl", "https://picsum.photos/seed/dg2/600/600"),
                Map.of("id", 3, "imgUrl", "https://picsum.photos/seed/dg3/600/600")
        );

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("goodsBrief", brief);
        info.put("retailPrice", String.format("%.2f", price));
        info.put("counterPrice", String.format("%.2f", price * 1.5));
        info.put("sellVolume", 328);
        info.put("listPicUrl", "https://picsum.photos/seed/dg1/600/600");
        info.put("goodsDesc", "<p><img src='https://picsum.photos/seed/desc1/750/400'/></p><p>" + name + "，源自道地产区，严格筛选，品质保证。</p><p><img src='https://picsum.photos/seed/desc2/750/400'/></p>");

        Map<String, Object> brand = Map.of("id", 1, "name", "药食同源精选");

        List<Map<String, Object>> attribute = List.of(
                Map.of("name", "产地", "value", "中国"),
                Map.of("name", "品牌", "value", "药食同源精选"),
                Map.of("name", "规格", "value", "250g/罐"),
                Map.of("name", "保质期", "value", "24个月"),
                Map.of("name", "储存方式", "value", "密封、阴凉、干燥处")
        );

        List<Map<String, Object>> issue = List.of(
                Map.of("id", 1, "question", "如何保存？", "answer", "请密封后放置于阴凉干燥处，避免阳光直射。"),
                Map.of("id", 2, "question", "保质期多久？", "answer", "保质期为24个月，请在有效期内食用。"),
                Map.of("id", 3, "question", "如何食用？", "answer", "可直接食用，也可泡水或煲汤，建议每日10-20g。")
        );

        List<Map<String, Object>> specValueList = new ArrayList<>();
        specValueList.add(new LinkedHashMap<>(Map.of("id", 1, "specificationId", 1, "value", "250g", "checked", false)));
        specValueList.add(new LinkedHashMap<>(Map.of("id", 2, "specificationId", 1, "value", "500g", "checked", false)));

        List<Map<String, Object>> specificationList = List.of(
                Map.of("specificationId", 1, "name", "规格", "valueList", specValueList)
        );

        List<Map<String, Object>> productList = List.of(
                Map.of("id", 101, "goodsSpecificationIds", "1", "goodsNumber", 99),
                Map.of("id", 102, "goodsSpecificationIds", "2", "goodsNumber", 50)
        );

        Map<String, Object> comment = Map.of(
                "count", 12,
                "data", Map.of(
                        "avatar", "https://picsum.photos/seed/user1/100/100",
                        "nickname", "养生达人",
                        "addTime", "2025-12-01",
                        "content", "品质很好，味道纯正，推荐购买！",
                        "picList", List.of()
                )
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("info", info);
        data.put("gallery", gallery);
        data.put("brand", brand);
        data.put("attribute", attribute);
        data.put("issue", issue);
        data.put("specificationList", specificationList);
        data.put("productList", productList);
        data.put("comment", comment);
        data.put("userHasCollect", 0);
        return ok(data);
    }

    @RequestMapping("/app-api/mock/goods/related")
    public Map<String, Object> goodsRelated(@RequestParam(value = "id", defaultValue = "1") Long id) {
        List<Map<String, Object>> goodsList = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            long relatedId = (id + i) % 68 + 1;
            double price = 19.9 + (relatedId * 5.7) % 200;
            goodsList.add(Map.of(
                    "id", relatedId,
                    "name", "推荐好物 #" + relatedId,
                    "listPicUrl", "https://picsum.photos/seed/rel" + i + "/200/200",
                    "retailPrice", String.format("%.2f", price)
            ));
        }
        return ok(Map.of("goodsList", goodsList));
    }

    // =========== 购物车 ===========

    @RequestMapping("/app-api/mock/cart/goodscount")
    public Map<String, Object> cartGoodsCount() {
        return ok(Map.of("cartTotal", Map.of("goodsCount", 2)));
    }

    @RequestMapping("/app-api/mock/cart/index")
    public Map<String, Object> cartIndex() {
        List<Map<String, Object>> cartList = new ArrayList<>();
        cartList.add(makeCartItem(1, 101, "东阿阿胶糕 250g", "250g", 99.90, 1));
        cartList.add(makeCartItem(2, 102, "同仁堂枸杞 500g", "500g", 49.90, 2));

        Map<String, Object> cartTotal = Map.of(
                "goodsCount", 3,
                "goodsAmount", 199.70,
                "checkedGoodsCount", 3,
                "checkedGoodsAmount", 199.70
        );
        return ok(Map.of("cartList", cartList, "cartTotal", cartTotal));
    }

    @RequestMapping("/app-api/mock/cart/add")
    public Map<String, Object> cartAdd(@RequestParam(value = "goodsId", defaultValue = "1") Long goodsId,
                                        @RequestParam(value = "number", defaultValue = "1") int number,
                                        @RequestParam(value = "productId", defaultValue = "101") Long productId) {
        return ok(Map.of("cartTotal", Map.of("goodsCount", number + 2)));
    }

    @RequestMapping("/app-api/mock/buy/add")
    public Map<String, Object> buyAdd(@RequestParam(value = "goodsId", defaultValue = "1") Long goodsId,
                                       @RequestParam(value = "number", defaultValue = "1") int number,
                                       @RequestParam(value = "productId", defaultValue = "101") Long productId) {
        return ok(Map.of());
    }

    @RequestMapping("/app-api/mock/cart/update")
    public Map<String, Object> cartUpdate(@RequestParam(value = "id", defaultValue = "1") Long id,
                                           @RequestParam(value = "number", defaultValue = "1") int number,
                                           @RequestParam(value = "goodsId", defaultValue = "1") Long goodsId,
                                           @RequestParam(value = "productId", defaultValue = "101") Long productId) {
        return ok(Map.of());
    }

    @RequestMapping("/app-api/mock/cart/delete")
    public Map<String, Object> cartDelete() {
        List<Map<String, Object>> cartList = new ArrayList<>();
        cartList.add(makeCartItem(1, 101, "东阿阿胶糕 250g", "250g", 99.90, 1));
        Map<String, Object> cartTotal = Map.of(
                "goodsCount", 1,
                "goodsAmount", 99.90,
                "checkedGoodsCount", 0,
                "checkedGoodsAmount", 0.00
        );
        return ok(Map.of("cartList", cartList, "cartTotal", cartTotal));
    }

    @RequestMapping("/app-api/mock/cart/checked")
    public Map<String, Object> cartChecked(@RequestParam(value = "productIds", defaultValue = "") String productIds,
                                            @RequestParam(value = "isChecked", defaultValue = "1") int isChecked) {
        List<Map<String, Object>> cartList = new ArrayList<>();
        cartList.add(makeCartItem(1, 101, "东阿阿胶糕 250g", "250g", 99.90, 1));
        cartList.add(makeCartItem(2, 102, "同仁堂枸杞 500g", "500g", 49.90, 2));

        double checkedAmount = isChecked == 1 ? 199.70 : 0.00;
        int checkedCount = isChecked == 1 ? 3 : 0;
        Map<String, Object> cartTotal = Map.of(
                "goodsCount", 3,
                "goodsAmount", 199.70,
                "checkedGoodsCount", checkedCount,
                "checkedGoodsAmount", checkedAmount
        );
        return ok(Map.of("cartList", cartList, "cartTotal", cartTotal));
    }

    @RequestMapping("/app-api/mock/cart/checkout")
    public Map<String, Object> cartCheckout(@RequestParam(value = "addressId", defaultValue = "0") Long addressId,
                                             @RequestParam(value = "couponId", defaultValue = "0") Long couponId,
                                             @RequestParam(value = "type", defaultValue = "cart") String type) {
        List<Map<String, Object>> checkedGoodsList = List.of(
                Map.of("id", 1, "goodsId", 1, "goodsName", "东阿阿胶糕", "listPicUrl", "https://picsum.photos/seed/goods0/200/200",
                        "retailPrice", 99.90, "number", 1, "goodsSpecifitionNameValue", "250g"),
                Map.of("id", 2, "goodsId", 2, "goodsName", "同仁堂枸杞", "listPicUrl", "https://picsum.photos/seed/goods1/200/200",
                        "retailPrice", 49.90, "number", 2, "goodsSpecifitionNameValue", "500g")
        );

        Map<String, Object> checkedAddress = new LinkedHashMap<>();
        checkedAddress.put("id", 1);
        checkedAddress.put("userName", "张三");
        checkedAddress.put("telNumber", "13800138000");
        checkedAddress.put("fullRegion", "北京市朝阳区");
        checkedAddress.put("detailInfo", "望京SOHO T3 1201");
        checkedAddress.put("isDefault", 1);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkedGoodsList", checkedGoodsList);
        data.put("checkedAddress", checkedAddress);
        data.put("actualPrice", 189.70);
        data.put("checkedCoupon", null);
        data.put("couponList", List.of());
        data.put("couponPrice", 0.00);
        data.put("freightPrice", 0.00);
        data.put("goodsTotalPrice", 199.70);
        data.put("orderTotalPrice", 199.70);
        return ok(data);
    }

    // =========== 收藏 ===========

    @RequestMapping("/app-api/mock/collect/addordelete")
    public Map<String, Object> collectAddOrDelete(@RequestParam(value = "typeId", defaultValue = "0") int typeId,
                                                   @RequestParam(value = "valueId", defaultValue = "1") Long valueId) {
        return ok(Map.of("type", "add"));
    }

    @RequestMapping("/app-api/mock/collect/list")
    public Map<String, Object> collectList() {
        return ok(Map.of("collectList", List.of(), "totalPages", 0));
    }

    // =========== 订单 ===========

    @RequestMapping("/app-api/mock/order/submit")
    public Map<String, Object> orderSubmit() {
        Map<String, Object> orderInfo = Map.of("id", 10001, "orderSn", "202607030001");
        return ok(Map.of("orderInfo", orderInfo));
    }

    @RequestMapping("/app-api/mock/order/list")
    public Map<String, Object> orderList(@RequestParam(value = "page", defaultValue = "1") int page,
                                          @RequestParam(value = "size", defaultValue = "10") int size) {
        List<Map<String, Object>> list = List.of(
                Map.of("id", 10001, "orderSn", "202607030001", "orderStatusText", "已完成",
                        "actualPrice", "199.70",
                        "handleOption", Map.of("pay", false, "cancel", false),
                        "goodsList", List.of(
                                Map.of("id", 1, "goodsName", "东阿阿胶糕", "number", 1,
                                        "listPicUrl", "https://picsum.photos/seed/goods0/200/200")
                        ))
        );
        return ok(Map.of("list", list, "page", page, "total", 1));
    }

    @RequestMapping("/app-api/mock/order/detail")
    public Map<String, Object> orderDetail(@RequestParam(value = "id", defaultValue = "10001") Long id) {
        Map<String, Object> orderInfo = new LinkedHashMap<>();
        orderInfo.put("id", id);
        orderInfo.put("orderSn", "202607030001");
        orderInfo.put("orderStatusText", "已完成");
        orderInfo.put("actualPrice", "199.70");
        orderInfo.put("goodsList", List.of(
                Map.of("id", 1, "goodsName", "东阿阿胶糕", "number", 1, "retailPrice", "99.90",
                        "listPicUrl", "https://picsum.photos/seed/goods0/200/200", "goodsSpecifitionNameValue", "250g")
        ));
        return ok(Map.of("orderInfo", orderInfo));
    }

    @RequestMapping("/app-api/mock/pay/prepay")
    public Map<String, Object> payPrepay(@RequestParam(value = "orderId", defaultValue = "10001") Long orderId) {
        return ok(Map.of(
                "timeStamp", String.valueOf(System.currentTimeMillis() / 1000),
                "nonceStr", "mock_nonce",
                "package", "prepay_id=mock_prepay",
                "signType", "MD5",
                "paySign", "mock_sign"
        ));
    }

    @RequestMapping("/app-api/mock/pay/query")
    public Map<String, Object> payQuery(@RequestParam(value = "orderId", defaultValue = "10001") Long orderId) {
        return ok(Map.of("orderStatus", "paid"));
    }

    // =========== 搜索 ===========

    @RequestMapping("/app-api/search/index")
    public Map<String, Object> searchIndex() {
        List<String> historyKeywordList = List.of("阿胶", "枸杞", "人参");
        List<Map<String, Object>> hotKeywordList = List.of(
                Map.of("keyword", "阿胶糕", "isHot", 1),
                Map.of("keyword", "枸杞", "isHot", 1),
                Map.of("keyword", "黑芝麻丸", "isHot", 0),
                Map.of("keyword", "人参", "isHot", 0),
                Map.of("keyword", "蜂蜜", "isHot", 0),
                Map.of("keyword", "花茶", "isHot", 0)
        );
        Map<String, Object> defaultKeyword = Map.of("keyword", "阿胶糕");
        return ok(Map.of("historyKeywordList", historyKeywordList,
                "hotKeywordList", hotKeywordList, "defaultKeyword", defaultKeyword));
    }

    @RequestMapping("/app-api/search/helper")
    public Map<String, Object> searchHelper(@RequestParam(value = "keyword", defaultValue = "") String keyword) {
        List<String> data = List.of(keyword + "糕", keyword + "茶", keyword + "丸", keyword + "片");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }

    @RequestMapping("/app-api/search/clearhistory")
    public Map<String, Object> searchClearHistory() {
        return ok(Map.of());
    }

    // =========== 优惠券 ===========

    @RequestMapping("/app-api/coupon/list")
    public Map<String, Object> couponList() {
        List<Map<String, Object>> data = List.of(
                Map.of("id", 1, "name", "新人专享券", "typeMoney", 10, "minGoodsAmount", 99,
                        "useEndDate", "2026-12-31", "couponStatus", 1),
                Map.of("id", 2, "name", "满减优惠券", "typeMoney", 20, "minGoodsAmount", 199,
                        "useEndDate", "2026-08-31", "couponStatus", 1),
                Map.of("id", 3, "name", "会员折扣券", "typeMoney", 50, "minGoodsAmount", 399,
                        "useEndDate", "2026-06-01", "couponStatus", 3)
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }

    // =========== 品牌 ===========

    @RequestMapping("/app-api/brand/list")
    public Map<String, Object> brandList() {
        List<Map<String, Object>> brandList = List.of(
                Map.of("id", 1, "name", "东阿阿胶", "picUrl", "https://picsum.photos/seed/brand1/200/200", "floorPrice", "99.00"),
                Map.of("id", 2, "name", "同仁堂", "picUrl", "https://picsum.photos/seed/brand2/200/200", "floorPrice", "59.00"),
                Map.of("id", 3, "name", "江中", "picUrl", "https://picsum.photos/seed/brand3/200/200", "floorPrice", "39.00")
        );
        return ok(Map.of("brandList", brandList, "totalPages", 1));
    }

    @RequestMapping("/app-api/brand/detail")
    public Map<String, Object> brandDetail(@RequestParam(value = "id", defaultValue = "1") Long id) {
        String name = switch (id.intValue()) {
            case 1 -> "东阿阿胶";
            case 2 -> "同仁堂";
            case 3 -> "江中";
            default -> "品牌" + id;
        };
        Map<String, Object> brand = Map.of(
                "id", id, "name", name,
                "picUrl", "https://picsum.photos/seed/brand" + id + "/600/300",
                "simpleDesc", name + "，品质保证，值得信赖"
        );
        return ok(Map.of("brand", brand));
    }

    // =========== 专题页 ===========

    @RequestMapping("/app-api/topic/list")
    public Map<String, Object> topicList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "title", "药食同源养生指南", "subtitle", "传统中医智慧，现代健康生活",
                        "scenePicUrl", IMG, "priceInfo", "49.9", "content", "药食同源是中国传统养生理念..."),
                Map.of("id", 2, "title", "四季养生茶饮推荐", "subtitle", "顺应时节，调养身心",
                        "scenePicUrl", IMG, "priceInfo", "29.9", "content", "春饮花茶，夏饮绿茶..."),
                Map.of("id", 3, "title", "滋补食材选购攻略", "subtitle", "选对食材，事半功倍",
                        "scenePicUrl", IMG, "priceInfo", "39.9", "content", "如何挑选优质滋补食材...")
        );
        return ok(Map.of("records", records, "total", 3, "pages", 1));
    }

    @RequestMapping("/app-api/topic/detail")
    public Map<String, Object> topicDetail(@RequestParam(value = "id", defaultValue = "1") Long id) {
        return ok(Map.of(
                "id", id,
                "title", "药食同源养生指南",
                "subtitle", "传统中医智慧，现代健康生活",
                "scenePicUrl", IMG,
                "priceInfo", "49.9",
                "content", "<p>药食同源是中国传统养生理念，许多食材同时也是药材。</p><p>合理搭配食材，可以达到调养身心的效果。</p>"
        ));
    }

    @RequestMapping("/app-api/topic/related")
    public Map<String, Object> topicRelated(@RequestParam(value = "id", defaultValue = "1") Long id) {
        List<Map<String, Object>> list = List.of(
                Map.of("id", 2, "title", "四季养生茶饮", "subtitle", "顺应时节",
                        "scenePicUrl", "https://picsum.photos/seed/tr1/400/400", "priceInfo", "29.9"),
                Map.of("id", 3, "title", "滋补食材选购", "subtitle", "选对食材",
                        "scenePicUrl", "https://picsum.photos/seed/tr2/400/400", "priceInfo", "39.9")
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", list);
        return result;
    }

    // =========== 评论 ===========

    @RequestMapping("/app-api/mock/comment/list")
    public Map<String, Object> commentList(
            @RequestParam(value = "valueId", defaultValue = "1") Long valueId,
            @RequestParam(value = "typeId", defaultValue = "0") int typeId,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "nickname", "养生达人", "avatar", "https://picsum.photos/seed/u1/100/100",
                        "addTime", "2025-12-01", "content", "品质很好，推荐！", "picList", List.of()),
                Map.of("id", 2, "nickname", "健康生活", "avatar", "https://picsum.photos/seed/u2/100/100",
                        "addTime", "2025-11-20", "content", "已经回购三次了", "picList", List.of())
        );
        return ok(Map.of("records", records, "total", 2));
    }

    // =========== 地址 ===========

    @RequestMapping("/app-api/mock/address/list")
    public Map<String, Object> addressList() {
        List<Map<String, Object>> list = List.of(
                Map.of("id", 1, "userName", "张三", "telNumber", "13800138000",
                        "fullRegion", "北京市朝阳区", "detailInfo", "望京SOHO T3 1201", "isDefault", 1)
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", list);
        return result;
    }

    @RequestMapping("/app-api/mock/address/save")
    public Map<String, Object> addressSave() {
        return ok(Map.of());
    }

    @RequestMapping("/app-api/mock/address/delete")
    public Map<String, Object> addressDelete() {
        return ok(Map.of());
    }

    // =========== 足迹 ===========

    @RequestMapping("/app-api/mock/footprint/list")
    public Map<String, Object> footprintList() {
        return ok(Map.of("list", List.of(), "totalPages", 0));
    }

    // =========== 用户 ===========

    @RequestMapping("/app-api/user/info")
    public Map<String, Object> userInfo() {
        return ok(Map.of(
                "userInfo", Map.of(
                        "nickName", "测试用户",
                        "avatarUrl", IMG,
                        "mobile", "138****8888"
                )
        ));
    }

    // =========== 帮助中心 ===========

    @RequestMapping("/app-api/helpissue/typeList")
    public Map<String, Object> helpTypeList() {
        return ok(Map.of("list", List.of(
                Map.of("id", 1, "name", "商品相关"),
                Map.of("id", 2, "name", "订单相关"),
                Map.of("id", 3, "name", "配送相关")
        )));
    }

    @RequestMapping("/app-api/helpissue/issueList")
    public Map<String, Object> helpIssueList() {
        return ok(Map.of("list", List.of(
                Map.of("id", 1, "question", "如何退货？", "answer", "在订单详情页点击申请退货即可"),
                Map.of("id", 2, "question", "发货时间？", "answer", "一般下单后48小时内发货")
        )));
    }

    // --- helper ---

    private Map<String, Object> makeCartItem(long id, long productId, String name, String spec, double price, int number) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("goodsId", id);
        item.put("productId", productId);
        item.put("goodsName", name);
        item.put("goodsSpecifitionNameValue", spec);
        item.put("listPicUrl", getGoodsFromList(id).get("listPicUrl"));
        item.put("retailPrice", price);
        item.put("number", number);
        item.put("checked", true);
        return item;
    }

    private String getCategoryName(Long id) {
        return switch (id.intValue()) {
            case 1 -> "滋补养生";
            case 2 -> "茶饮花茶";
            case 3 -> "零食坚果";
            case 4 -> "保健食品";
            case 5 -> "药膳食材";
            default -> "分类" + id;
        };
    }

    private Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }
}
