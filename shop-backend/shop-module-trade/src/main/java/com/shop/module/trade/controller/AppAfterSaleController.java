package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeAfterSaleService;
import com.shop.module.trade.service.TradeMockActionGuard;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppAfterSaleController {

    private final TradeAfterSaleService tradeAfterSaleService;
    private final TradeMockActionGuard tradeMockActionGuard;

    @RequestMapping("/app-api/order/refund/apply")
    public CommonResult<Map<String, Object>> apply(@RequestBody(required = false) String rawBody,
                                                   @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeAfterSaleService.apply(userId, orderId, request));
    }

    @RequestMapping("/app-api/order/refund/detail")
    public CommonResult<Map<String, Object>> detail(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params,
                                                    @RequestParam(value = "orderId", required = false) Long orderId) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long finalOrderId = orderId != null ? orderId : TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeAfterSaleService.detail(userId, finalOrderId));
    }

    @RequestMapping("/app-api/order/refund/mock-approve")
    public CommonResult<Map<String, Object>> mockApprove(@RequestBody(required = false) String rawBody,
                                                         @RequestParam Map<String, Object> params) {
        tradeMockActionGuard.checkEnabled();
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeAfterSaleService.mockApprove(userId, orderId));
    }

    @RequestMapping("/app-api/order/refund/cancel")
    public CommonResult<Map<String, Object>> cancel(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeAfterSaleService.cancel(userId, orderId));
    }
}
