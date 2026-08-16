package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeObservabilityService;
import com.shop.module.trade.vo.ObservabilitySummaryRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminTradeObservabilityController {

    private final TradeObservabilityService tradeObservabilityService;

    @GetMapping("/admin-api/trade/observability/summary")
    public CommonResult<ObservabilitySummaryRespVO> summary(
            @RequestParam(value = "orderSn", required = false) String orderSn) {
        return CommonResult.success(tradeObservabilityService.getSummary(orderSn));
    }

    @GetMapping("/admin-api/trade/observability/alerts")
    public CommonResult<List<ObservabilitySummaryRespVO.AlertItem>> alerts() {
        return CommonResult.success(tradeObservabilityService.getActiveAlerts());
    }

    @GetMapping("/admin-api/trade/observability/order-trace")
    public CommonResult<ObservabilitySummaryRespVO.OrderTrace> orderTrace(
            @RequestParam("orderSn") String orderSn) {
        return CommonResult.success(tradeObservabilityService.traceOrder(orderSn));
    }
}
