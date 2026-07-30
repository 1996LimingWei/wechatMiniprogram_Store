package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.service.TradeLogisticsService;
import com.shop.module.trade.service.TradeOrderQueryService;
import com.shop.module.trade.service.TradeOrderService;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/trade/order")
@RequiredArgsConstructor
public class AdminTradeOrderController {

    private final TradeOrderService tradeOrderService;
    private final TradeOrderQueryService tradeOrderQueryService;
    private final TradeLogisticsService tradeLogisticsService;

    @RequestMapping("/list")
    public CommonResult<PageResult<Map<String, Object>>> list(@RequestBody(required = false) String rawBody,
                                                              @RequestParam Map<String, Object> params,
                                                              @RequestParam(value = "page", required = false) Integer page,
                                                              @RequestParam(value = "size", required = false) Integer size) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        int finalPage = page != null ? page : TradeRequestUtils.getInt(request, "page", 1);
        int finalSize = size != null ? size : TradeRequestUtils.getInt(request, "size", 10);
        return CommonResult.success(tradeOrderQueryService.getAdminOrderPage(finalPage, finalSize, request));
    }

    @RequestMapping("/detail")
    public CommonResult<Map<String, Object>> detail(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params,
                                                    @RequestParam(value = "orderId", required = false) Long orderId,
                                                    @RequestParam(value = "id", required = false) Long id) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long finalOrderId = orderId != null ? orderId : (id != null ? id : TradeRequestUtils.getLong(request, "orderId", 0L));
        return CommonResult.success(tradeOrderService.getAdminOrderDetail(finalOrderId));
    }

    @RequestMapping("/ship")
    public CommonResult<Map<String, Object>> ship(@RequestBody(required = false) String rawBody,
                                                  @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long orderId = TradeRequestUtils.getLong(request, "orderId", 0L);
        return CommonResult.success(tradeLogisticsService.adminShip(orderId, request));
    }
}
