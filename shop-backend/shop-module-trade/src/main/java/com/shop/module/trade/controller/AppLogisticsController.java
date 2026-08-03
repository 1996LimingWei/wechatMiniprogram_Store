package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeLogisticsService;
import com.shop.module.trade.service.TradeMockActionGuard;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppLogisticsController {

    private final TradeLogisticsService tradeLogisticsService;
    private final TradeMockActionGuard tradeMockActionGuard;

    @RequestMapping("/app-api/order/logistics")
    public CommonResult<Map<String, Object>> logistics(@RequestBody(required = false) String rawBody,
                                                       @RequestParam Map<String, Object> params,
                                                       @RequestParam(value = "orderId", required = false) Long orderId) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long finalOrderId = orderId != null ? orderId : TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeLogisticsService.query(userId, finalOrderId));
    }

    @RequestMapping("/app-api/order/mock-ship")
    public CommonResult<Map<String, Object>> mockShip(@RequestBody(required = false) String rawBody,
                                                      @RequestParam Map<String, Object> params) {
        tradeMockActionGuard.checkEnabled();
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeLogisticsService.mockShip(userId, orderId, request));
    }
}
