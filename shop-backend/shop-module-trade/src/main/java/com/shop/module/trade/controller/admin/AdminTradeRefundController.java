package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.service.RefundExceptionWorkbenchService;
import com.shop.module.trade.util.TradeSecurityUtils;
import com.shop.module.trade.vo.RefundActionReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminTradeRefundController {

    private final RefundExceptionWorkbenchService refundExceptionWorkbenchService;

    @GetMapping("/admin-api/trade/refund/list")
    public CommonResult<PageResult<?>> list(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "refundSn", required = false) String refundSn,
            @RequestParam(value = "afterSaleSn", required = false) String afterSaleSn,
            @RequestParam(value = "orderSn", required = false) String orderSn,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "exceptionOnly", required = false, defaultValue = "1") Integer exceptionOnly,
            @RequestParam(value = "createTimeStart", required = false) String createTimeStart,
            @RequestParam(value = "createTimeEnd", required = false) String createTimeEnd) {
        return CommonResult.success(refundExceptionWorkbenchService.getRefundPage(
                page, size, refundSn, afterSaleSn, orderSn, status, exceptionOnly, createTimeStart, createTimeEnd));
    }

    @GetMapping("/admin-api/trade/refund/detail")
    public CommonResult<?> detail(@RequestParam("afterSaleId") Long afterSaleId) {
        return CommonResult.success(refundExceptionWorkbenchService.getRefundDetail(afterSaleId));
    }

    @PostMapping("/admin-api/trade/refund/sync")
    public CommonResult<?> sync(@RequestBody RefundActionReqVO request) {
        return CommonResult.success(refundExceptionWorkbenchService.syncRefund(
                TradeSecurityUtils.getRequiredUserId(), request.getAfterSaleId()));
    }

    @PostMapping("/admin-api/trade/refund/retry")
    public CommonResult<?> retry(@RequestBody RefundActionReqVO request) {
        return CommonResult.success(refundExceptionWorkbenchService.retryRefund(
                TradeSecurityUtils.getRequiredUserId(), request.getAfterSaleId()));
    }

    @PostMapping("/admin-api/trade/refund/handle")
    public CommonResult<Boolean> handle(@RequestBody RefundActionReqVO request) {
        return CommonResult.success(refundExceptionWorkbenchService.handleRefund(
                TradeSecurityUtils.getRequiredUserId(), request.getAfterSaleId(), request.getRemark()));
    }
}
