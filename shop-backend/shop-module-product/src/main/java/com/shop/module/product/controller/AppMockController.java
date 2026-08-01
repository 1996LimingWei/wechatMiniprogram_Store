package com.shop.module.product.controller;

import com.shop.module.product.service.MockProductCatalogProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 分类/专题/购物车等页面 mock 接口 - 开发阶段使用
 */
@RestController
@RequiredArgsConstructor
public class AppMockController {

    private final MockProductCatalogProvider productCatalogProvider;

    private Map<String, Object> getGoodsFromList(long id) {
        return productCatalogProvider.goods(id);
    }

    @RequestMapping("/app-api/mock/catalog/index")
    public Map<String, Object> catalogIndex() {
        return ok(productCatalogProvider.catalogIndex());
    }

    @RequestMapping("/app-api/mock/catalog/current")
    public Map<String, Object> catalogCurrent(@RequestParam(value = "id", defaultValue = "1") Long id) {
        return ok(Map.of("currentCategory", productCatalogProvider.catalog(id)));
    }

    @RequestMapping("/app-api/mock/catalog/{id}")
    public Map<String, Object> catalogDetail(@PathVariable Long id) {
        return ok(Map.of("currentCategory", productCatalogProvider.catalog(id)));
    }

    @RequestMapping("/app-api/mock/goods/count")
    public Map<String, Object> goodsCount() {
        return ok(productCatalogProvider.count());
    }

    @RequestMapping("/app-api/mock/goods/category")
    public Map<String, Object> goodsCategory(@RequestParam(value = "id", defaultValue = "1") Long id) {
        return ok(productCatalogProvider.goodsCategory(id));
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
        
        return ok(productCatalogProvider.list(categoryId, keyword, isHot, isNew, page, size));
    }

    // =========== 商品详情 ===========

    @RequestMapping("/app-api/mock/goods/detail")
    public Map<String, Object> goodsDetail(@RequestParam(value = "id", defaultValue = "1") Long id) {
        return ok(productCatalogProvider.detail(id));
    }

    @RequestMapping("/app-api/mock/goods/related")
    public Map<String, Object> goodsRelated(@RequestParam(value = "id", defaultValue = "1") Long id) {
        return ok(productCatalogProvider.related(id));
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

    private Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }
}
