package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.trade.service.TradeAfterSaleService;
import com.shop.module.trade.service.TradeRefundExecutionService;
import com.shop.module.trade.service.TradeOrderLogService;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/trade/after-sale")
@RequiredArgsConstructor
public class AdminTradeAfterSaleController {

    private final TradeAfterSaleService tradeAfterSaleService;
    private final TradeRefundExecutionService tradeRefundExecutionService;

    @GetMapping("/list")
    public CommonResult<Map<String, Object>> list(@RequestParam Map<String, Object> params,
                                                  @RequestParam(value = "page", required = false) Integer page,
                                                  @RequestParam(value = "size", required = false) Integer size,
                                                  @RequestParam(value = "status", required = false) Integer status) {
        Map<String, Object> request = TradeRequestUtils.parse(null, params);
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

    @PostMapping("/approve")
    public CommonResult<Map<String, Object>> approve(@RequestBody(required = false) String rawBody,
                                                     @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeAfterSaleService.adminApprove(
                TradeSecurityUtils.getRequiredUserId(), TradeRequestUtils.getLong(request, "afterSaleId", 0L)));
    }

    @PostMapping("/reject")
    public CommonResult<Map<String, Object>> reject(@RequestBody(required = false) String rawBody,
                                                    @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeAfterSaleService.adminReject(
                TradeSecurityUtils.getRequiredUserId(),
                TradeRequestUtils.getLong(request, "afterSaleId", 0L),
                TradeRequestUtils.getString(request, "rejectReason", "")
        ));
    }

    @PostMapping("/sync")
    public CommonResult<Map<String, Object>> sync(@RequestBody(required = false) String rawBody,
                                                  @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        Long adminId = TradeSecurityUtils.getRequiredUserId();
        Long afterSaleId = TradeRequestUtils.getLong(request, "afterSaleId", 0L);
        tradeRefundExecutionService.execute(
                afterSaleId, TradeOrderLogService.OPERATOR_ADMIN, adminId, true);
        return CommonResult.success(tradeAfterSaleService.getAdminAfterSale(afterSaleId));
    }

    @PostMapping("/receive")
    public CommonResult<Map<String, Object>> receive(@RequestBody(required = false) String rawBody,
                                                      @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(tradeAfterSaleService.adminReceive(
                TradeSecurityUtils.getRequiredUserId(),
                TradeRequestUtils.getLong(request, "afterSaleId", 0L),
                TradeRequestUtils.getString(request, "receiveRemark", "")));
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
