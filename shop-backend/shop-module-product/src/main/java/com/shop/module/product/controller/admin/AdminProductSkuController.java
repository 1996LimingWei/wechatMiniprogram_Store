package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.service.ProductAdminService;
import com.shop.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin-api/product/sku")
@RequiredArgsConstructor
public class AdminProductSkuController {

    private final ProductAdminService productAdminService;

    @GetMapping("/list")
    public CommonResult<List<ProductSkuDO>> list(@RequestParam Long spuId) {
        return CommonResult.success(productAdminService.listSkus(spuId));
    }

    @PostMapping("/save-batch")
    public CommonResult<Boolean> saveBatch(@RequestParam Long spuId,
                                           @RequestParam String stockAdjustReason,
                                           @RequestBody List<ProductSkuDO> skus) {
        productAdminService.saveSkus(
                spuId, skus, SecurityUtils.getRequiredAdminId(), stockAdjustReason);
        return CommonResult.success(true);
    }
}
