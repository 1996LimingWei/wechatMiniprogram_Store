package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.service.ProductSpuService;
import com.shop.module.product.service.ProductAdminService;
import com.shop.module.product.vo.ProductSaveReqVO;
import com.shop.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/product/spu")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductSpuService productSpuService;
    private final ProductAdminService productAdminService;

    @PostMapping("/save")
    public CommonResult<Long> save(@RequestBody ProductSaveReqVO request) {
        return CommonResult.success(productAdminService.saveProduct(
                request.getSpu(), request.getSkus(), SecurityUtils.getRequiredAdminId(),
                request.getStockAdjustReason()));
    }

    @GetMapping("/page")
    public CommonResult<PageResult<ProductSpuDO>> page(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(productSpuService.getAdminSpuPage(pageParam, name, categoryId, status));
    }

    @GetMapping("/detail")
    public CommonResult<ProductSpuDO> detail(@RequestParam Long id) {
        return CommonResult.success(productSpuService.getSpuDetail(id));
    }

    @PostMapping("/create")
    public CommonResult<Boolean> create(@RequestBody ProductSpuDO spu) {
        productSpuService.createSpu(spu, SecurityUtils.getRequiredAdminId());
        return CommonResult.success(true);
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody ProductSpuDO spu) {
        productSpuService.updateSpu(spu);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        productSpuService.deleteSpu(id);
        return CommonResult.success(true);
    }
}
