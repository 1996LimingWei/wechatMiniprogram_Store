package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin-api/product/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    public CommonResult<List<CategoryDO>> list() {
        return CommonResult.success(categoryService.getAllList());
    }

    @PostMapping("/create")
    public CommonResult<Boolean> create(@RequestBody CategoryDO category) {
        categoryService.create(category);
        return CommonResult.success(true);
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody CategoryDO category) {
        categoryService.update(category);
        return CommonResult.success(true);
    }

    @PutMapping("/update-status")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        categoryService.delete(id);
        return CommonResult.success(true);
    }
}
