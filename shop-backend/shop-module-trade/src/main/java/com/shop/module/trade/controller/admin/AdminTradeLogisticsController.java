package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeLogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/trade/logistics")
@RequiredArgsConstructor
public class AdminTradeLogisticsController {

    private final TradeLogisticsService tradeLogisticsService;

    @GetMapping("/detail")
    public CommonResult<Map<String, Object>> detail(@RequestParam("orderId") Long orderId) {
        return CommonResult.success(tradeLogisticsService.adminQuery(orderId));
    }
}
