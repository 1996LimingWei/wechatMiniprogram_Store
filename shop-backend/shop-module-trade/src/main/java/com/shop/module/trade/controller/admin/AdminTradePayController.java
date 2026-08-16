package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.service.PayExceptionWorkbenchService;
import com.shop.module.trade.util.TradeSecurityUtils;
import com.shop.module.trade.vo.PayExceptionHandleReqVO;
import com.shop.module.trade.vo.PaySyncReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminTradePayController {

    private final PayExceptionWorkbenchService payExceptionWorkbenchService;

    @GetMapping("/admin-api/trade/pay/order/list")
    public CommonResult<PageResult<?>> payOrderList(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "paySn", required = false) String paySn,
            @RequestParam(value = "orderSn", required = false) String orderSn,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "createTimeStart", required = false) String createTimeStart,
            @RequestParam(value = "createTimeEnd", required = false) String createTimeEnd) {
        return CommonResult.success(payExceptionWorkbenchService.getPayOrderPage(
                page, size, paySn, orderSn, status, createTimeStart, createTimeEnd));
    }

    @GetMapping("/admin-api/trade/pay/order/detail")
    public CommonResult<?> payOrderDetail(
            @RequestParam("payOrderId") Long payOrderId) {
        return CommonResult.success(payExceptionWorkbenchService.getPayOrderDetail(payOrderId));
    }

    @PostMapping("/admin-api/trade/pay/order/sync")
    public CommonResult<?> syncPayOrder(@RequestBody PaySyncReqVO request) {
        return CommonResult.success(payExceptionWorkbenchService.manualSync(
                TradeSecurityUtils.getRequiredUserId(), request.getPayOrderId()));
    }

    @GetMapping("/admin-api/trade/pay/exception/list")
    public CommonResult<PageResult<?>> exceptionList(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "paySn", required = false) String paySn,
            @RequestParam(value = "orderSn", required = false) String orderSn,
            @RequestParam(value = "reasonCode", required = false) String reasonCode,
            @RequestParam(value = "handled", required = false) Integer handled,
            @RequestParam(value = "createTimeStart", required = false) String createTimeStart,
            @RequestParam(value = "createTimeEnd", required = false) String createTimeEnd) {
        return CommonResult.success(payExceptionWorkbenchService.getExceptionPage(
                page, size, paySn, orderSn, reasonCode, handled, createTimeStart, createTimeEnd));
    }

    @PostMapping("/admin-api/trade/pay/exception/handle")
    public CommonResult<Boolean> handleException(@RequestBody PayExceptionHandleReqVO request) {
        return CommonResult.success(payExceptionWorkbenchService.handleException(
                TradeSecurityUtils.getRequiredUserId(), request.getExceptionId(), request.getRemark()));
    }
}
