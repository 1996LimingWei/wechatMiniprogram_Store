package com.shop.module.product.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.pojo.CommonResult;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin-api/product/sku")
@RequiredArgsConstructor
public class AdminProductSkuController {

    private final ProductSkuMapper productSkuMapper;

    @GetMapping("/list")
    public CommonResult<List<ProductSkuDO>> list(@RequestParam Long spuId) {
        return CommonResult.success(
                productSkuMapper.selectList(
                        new LambdaQueryWrapper<ProductSkuDO>()
                                .eq(ProductSkuDO::getSpuId, spuId)));
    }

    @PostMapping("/save-batch")
    @Transactional
    public CommonResult<Boolean> saveBatch(@RequestParam Long spuId,
                                           @RequestBody List<ProductSkuDO> skus) {
        // 删除该 SPU 下的所有旧 SKU
        productSkuMapper.delete(
                new LambdaQueryWrapper<ProductSkuDO>()
                        .eq(ProductSkuDO::getSpuId, spuId));
        // 批量插入新 SKU
        for (ProductSkuDO sku : skus) {
            sku.setSpuId(spuId);
            sku.setId(null); // 确保使用新 ID
            productSkuMapper.insert(sku);
        }
        return CommonResult.success(true);
    }
}
