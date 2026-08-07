package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeOrderQueryService;
import com.shop.module.trade.service.TradeOrderService;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppOrderController {

    private final TradeOrderService tradeOrderService;
    private final TradeOrderQueryService tradeOrderQueryService;

    @RequestMapping("/app-api/order/submit")
    public CommonResult<Map<String, Object>> submit(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeOrderService.submitOrder(userId,
                TradeRequestUtils.getLong(request, "addressId", 0L),
                TradeRequestUtils.getString(request, "requestId", "")));
    }

    @RequestMapping("/app-api/order/list")
    public CommonResult<Map<String, Object>> list(@RequestBody(required = false) String rawBody,
                                                   @RequestParam Map<String, Object> params,
                                                   @RequestParam(value = "showType", required = false) Integer showType,
                                                   @RequestParam(value = "page", required = false) Integer page,
                                                   @RequestParam(value = "size", required = false) Integer size) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeOrderQueryService.getOrderList(
                userId,
                showType != null ? showType : TradeRequestUtils.getInt(request, "showType", 0),
                page != null ? page : TradeRequestUtils.getInt(request, "page", 1),
                size != null ? size : TradeRequestUtils.getInt(request, "size", 10)
        ));
    }

    @RequestMapping("/app-api/order/detail")
    public CommonResult<Map<String, Object>> detail(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params,
                                                     @RequestParam(value = "orderId", required = false) Long orderId,
                                                     @RequestParam(value = "id", required = false) Long id) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long finalOrderId = orderId != null ? orderId : (id != null ? id : TradeRequestUtils.getLong(request, "orderId", 0L));
        return CommonResult.success(tradeOrderService.getOrderDetail(userId, finalOrderId));
    }

    @RequestMapping("/app-api/order/cancelOrder")
    public CommonResult<String> cancel(@RequestBody(required = false) String rawBody,
                                       @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeOrderService.cancelOrder(userId,
                TradeRequestUtils.getLong(request, "orderId", 0L)));
    }

    @RequestMapping("/app-api/order/confirmOrder")
    public CommonResult<String> confirm(@RequestBody(required = false) String rawBody,
                                        @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeOrderService.confirmOrder(userId,
                TradeRequestUtils.getLong(request, "orderId", 0L)));
    }
}
