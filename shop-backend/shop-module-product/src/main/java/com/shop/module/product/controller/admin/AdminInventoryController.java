package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.framework.security.SecurityUtils;
import com.shop.module.product.service.ProductInventoryWorkbenchService;
import com.shop.module.product.vo.InventoryReconcileRespVO;
import com.shop.module.product.vo.InventorySkuRespVO;
import com.shop.module.product.vo.InventoryStockAdjustReqVO;
import com.shop.module.product.vo.InventoryStockLogRespVO;
import com.shop.module.product.vo.InventoryWarningStockReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminInventoryController {

    private final ProductInventoryWorkbenchService inventoryWorkbenchService;

    @GetMapping("/admin-api/product/inventory/page")
    public CommonResult<PageResult<InventorySkuRespVO>> page(PageParam pageParam,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) Boolean lowStockOnly) {
        return CommonResult.success(inventoryWorkbenchService.page(
                pageParam, productName, skuCode, stockStatus, lowStockOnly));
    }

    @PutMapping("/admin-api/product/inventory/warning-stock")
    public CommonResult<Boolean> warningStock(@RequestBody InventoryWarningStockReqVO request) {
        inventoryWorkbenchService.updateWarningStock(request);
        return CommonResult.success(true);
    }

    @PostMapping("/admin-api/product/inventory/adjust")
    public CommonResult<InventorySkuRespVO> adjust(@RequestBody InventoryStockAdjustReqVO request) {
        return CommonResult.success(inventoryWorkbenchService.adjustStock(
                request, SecurityUtils.getRequiredAdminId()));
    }

    @GetMapping("/admin-api/product/inventory/log-page")
    public CommonResult<PageResult<InventoryStockLogRespVO>> logPage(PageParam pageParam,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long spuId,
            @RequestParam(required = false) String bizNo) {
        return CommonResult.success(inventoryWorkbenchService.logPage(pageParam, skuId, spuId, bizNo));
    }

    @GetMapping("/admin-api/product/inventory/reconcile")
    public CommonResult<InventoryReconcileRespVO> reconcile() {
        return CommonResult.success(inventoryWorkbenchService.reconcile());
    }
}
