package com.shop.module.trade.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.config.TradeDevActionGuard;
import com.shop.module.trade.service.PayOrderService;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AppPayController {

    private final PayOrderService payOrderService;
    private final TradeDevActionGuard tradeDevActionGuard;

    @RequestMapping("/app-api/pay/prepay")
    public CommonResult<Map<String, Object>> prepay(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(payOrderService.prepay(userId, orderId));
    }

    @RequestMapping("/app-api/pay/query")
    public CommonResult<Map<String, Object>> query(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params) {
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(payOrderService.query(userId, orderId));
    }

    @RequestMapping("/app-api/pay/mock-success")
    public CommonResult<Boolean> mockSuccess(@RequestBody(required = false) String rawBody,
                                             @RequestParam Map<String, Object> params) {
        tradeDevActionGuard.checkEnabled();
        Long userId = TradeSecurityUtils.getRequiredUserId();
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        payOrderService.mockSuccess(userId, orderId);
        return CommonResult.success(true);
    }
}
