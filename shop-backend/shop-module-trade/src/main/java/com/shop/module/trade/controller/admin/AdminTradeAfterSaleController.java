package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeAfterSaleService;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/trade/after-sale")
@RequiredArgsConstructor
public class AdminTradeAfterSaleController {

    private final TradeAfterSaleService tradeAfterSaleService;

    @RequestMapping("/list")
    public CommonResult<Map<String, Object>> list(@RequestBody(required = false) String rawBody,
                                                  @RequestParam Map<String, Object> params,
                                                  @RequestParam(value = "page", required = false) Integer page,
                                                  @RequestParam(value = "size", required = false) Integer size,
                                                  @RequestParam(value = "status", required = false) Integer status) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        int finalPage = page != null ? page : TradeRequestUtils.getInt(request, "page", 1);
        int finalSize = size != null ? size : TradeRequestUtils.getInt(request, "size", 10);
        Integer finalStatus = status != null ? status : getInteger(request, "status");
        return CommonResult.success(tradeAfterSaleService.adminList(
                finalPage,
                finalSize,
                finalStatus,
                TradeRequestUtils.getLong(request, "userId", 0L),
                TradeRequestUtils.getLong(request, "orderId", 0L)
        ));
    }

    @RequestMapping("/approve")
    public CommonResult<Map<String, Object>> approve(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeAfterSaleService.adminApprove(TradeRequestUtils.getLong(request, "orderId", 0L)));
    }

    @RequestMapping("/reject")
    public CommonResult<Map<String, Object>> reject(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeAfterSaleService.adminReject(
                TradeRequestUtils.getLong(request, "orderId", 0L),
                TradeRequestUtils.getString(request, "rejectReason", "商家拒绝售后申请")
        ));
    }

    private Integer getInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
