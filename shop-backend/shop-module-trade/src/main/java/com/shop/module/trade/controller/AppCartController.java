package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeCartService;
import com.shop.module.trade.service.TradeCheckoutService;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppCartController {

    private final TradeCartService tradeCartService;
    private final TradeCheckoutService tradeCheckoutService;

    @RequestMapping("/app-api/cart/goodscount")
    public CommonResult<Map<String, Object>> goodscount() {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        return CommonResult.success(Map.of("cartTotal", tradeCartService.getCartTotal(userId)));
    }

    @RequestMapping("/app-api/cart/index")
    public CommonResult<Map<String, Object>> index() {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        return CommonResult.success(tradeCartService.getCartIndex(userId));
    }

    @RequestMapping("/app-api/cart/add")
    public CommonResult<Map<String, Object>> add(@RequestBody(required = false) String rawBody,
                                                  @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long goodsId = TradeRequestUtils.getLong(request, "goodsId", 0L);
        Long productId = TradeRequestUtils.getLong(request, "productId", 0L);
        int number = TradeRequestUtils.getInt(request, "number", 1);
        return CommonResult.success(tradeCartService.addCart(userId, goodsId, productId, number));
    }

    @RequestMapping("/app-api/buy/add")
    public CommonResult<Map<String, Object>> buyAdd(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long goodsId = TradeRequestUtils.getLong(request, "goodsId", 0L);
        Long productId = TradeRequestUtils.getLong(request, "productId", 0L);
        int number = TradeRequestUtils.getInt(request, "number", 1);
        return CommonResult.success(tradeCartService.addBuyNow(userId, goodsId, productId, number));
    }

    @RequestMapping("/app-api/cart/update")
    public CommonResult<Boolean> update(@RequestBody(required = false) String rawBody,
                                        @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        tradeCartService.updateCart(
                userId,
                TradeRequestUtils.getLong(request, "id", 0L),
                TradeRequestUtils.getLong(request, "goodsId", 0L),
                TradeRequestUtils.getLong(request, "productId", 0L),
                TradeRequestUtils.getInt(request, "number", 1),
                TradeRequestUtils.getInt(request, "expectedCount", 0)
        );
        return CommonResult.success(true);
    }

    @RequestMapping("/app-api/cart/delete")
    public CommonResult<Map<String, Object>> delete(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeCartService.deleteCart(userId,
                TradeRequestUtils.getString(request, "productIds", "")));
    }

    @RequestMapping("/app-api/cart/checked")
    public CommonResult<Map<String, Object>> checked(@RequestBody(required = false) String rawBody,
                                                      @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeCartService.checkedCart(
                userId,
                TradeRequestUtils.getString(request, "productIds", ""),
                TradeRequestUtils.getInt(request, "isChecked", 1)
        ));
    }

    @RequestMapping("/app-api/cart/checkout")
    public CommonResult<Map<String, Object>> checkout(@RequestBody(required = false) String rawBody,
                                                       @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long addressId = TradeRequestUtils.getLong(request, "addressId", 0L);
        return CommonResult.success(tradeCheckoutService.checkout(userId, addressId));
    }
}
