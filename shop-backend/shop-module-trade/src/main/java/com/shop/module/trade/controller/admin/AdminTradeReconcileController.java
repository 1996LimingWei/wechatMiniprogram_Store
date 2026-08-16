package com.shop.module.trade.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.service.ReconciliationWorkbenchService;
import com.shop.module.trade.util.TradeSecurityUtils;
import com.shop.module.trade.vo.ReconcileDifferenceHandleReqVO;
import com.shop.module.trade.vo.ReconcileRunReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class AdminTradeReconcileController {

    private final ReconciliationWorkbenchService reconciliationWorkbenchService;

    @GetMapping("/admin-api/trade/reconcile/batch/list")
    public CommonResult<PageResult<?>> batchList(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "dateStart", required = false) String dateStart,
            @RequestParam(value = "dateEnd", required = false) String dateEnd,
            @RequestParam(value = "status", required = false) Integer status) {
        return CommonResult.success(reconciliationWorkbenchService.getBatchPage(
                page, size, dateStart, dateEnd, status));
    }

    @GetMapping("/admin-api/trade/reconcile/batch/detail")
    public CommonResult<?> batchDetail(@RequestParam("batchId") Long batchId) {
        return CommonResult.success(reconciliationWorkbenchService.getBatchDetail(batchId));
    }

    @GetMapping("/admin-api/trade/reconcile/difference/list")
    public CommonResult<PageResult<?>> differenceList(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam("batchId") Long batchId,
            @RequestParam(value = "diffType", required = false) String diffType,
            @RequestParam(value = "handled", required = false) Integer handled) {
        return CommonResult.success(reconciliationWorkbenchService.getDifferencePage(
                page, size, batchId, diffType, handled));
    }

    @PostMapping("/admin-api/trade/reconcile/run")
    public CommonResult<?> run(@RequestBody ReconcileRunReqVO request) {
        return CommonResult.success(reconciliationWorkbenchService.run(
                TradeSecurityUtils.getRequiredUserId(), request.getReconcileDate(), "MANUAL"));
    }

    @PostMapping("/admin-api/trade/reconcile/difference/handle")
    public CommonResult<Boolean> handleDifference(@RequestBody ReconcileDifferenceHandleReqVO request) {
        return CommonResult.success(reconciliationWorkbenchService.handleDifference(
                TradeSecurityUtils.getRequiredUserId(), request.getDifferenceId(), request.getRemark()));
    }

    @GetMapping("/admin-api/trade/reconcile/export")
    public ResponseEntity<byte[]> export(@RequestParam("batchId") Long batchId) {
        byte[] content = reconciliationWorkbenchService.export(batchId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("日终对账结果.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(content);
    }
}
