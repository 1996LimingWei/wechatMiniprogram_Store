package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.framework.security.SecurityUtils;
import com.shop.module.product.service.MaterialAssetService;
import com.shop.module.product.vo.MaterialAssetRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminMaterialController {

    private final MaterialAssetService materialAssetService;

    @PostMapping("/admin-api/material/upload")
    public CommonResult<MaterialAssetRespVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String bizType) {
        return CommonResult.success(materialAssetService.upload(file, bizType, SecurityUtils.getRequiredAdminId()));
    }

    @GetMapping("/admin-api/material/page")
    public CommonResult<PageResult<MaterialAssetRespVO>> page(
            PageParam pageParam,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return CommonResult.success(materialAssetService.page(pageParam, bizType, keyword, createdBy, startTime, endTime));
    }

    @GetMapping("/admin-api/material/references")
    public CommonResult<List<String>> references(@RequestParam Long id) {
        return CommonResult.success(materialAssetService.references(id));
    }

    @DeleteMapping("/admin-api/material/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        materialAssetService.delete(id);
        return CommonResult.success(true);
    }
}
