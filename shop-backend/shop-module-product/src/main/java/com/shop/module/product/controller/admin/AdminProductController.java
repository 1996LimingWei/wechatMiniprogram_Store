package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.service.ProductImportExportService;
import com.shop.module.product.service.ProductSpuService;
import com.shop.module.product.service.ProductAdminService;
import com.shop.module.product.vo.ProductImportPreviewRespVO;
import com.shop.module.product.vo.ProductSaveReqVO;
import com.shop.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin-api/product/spu")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductSpuService productSpuService;
    private final ProductAdminService productAdminService;
    private final ProductImportExportService productImportExportService;

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

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        writeCsv(response, "商品导入模板.csv", productImportExportService.templateCsv());
    }

    @PostMapping("/import-preview")
    public CommonResult<ProductImportPreviewRespVO> importPreview(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(productImportExportService.preview(file));
    }

    @PostMapping("/import-confirm")
    public CommonResult<ProductImportPreviewRespVO> importConfirm(@RequestParam("file") MultipartFile file) {
        return CommonResult.success(productImportExportService.importProducts(
                file, SecurityUtils.getRequiredAdminId()));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false)
                       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                       @RequestParam(required = false)
                       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) throws IOException {
        writeCsv(response, "商品导出.csv",
                productImportExportService.exportCsv(name, categoryId, status, startTime, endTime));
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

    private void writeCsv(HttpServletResponse response, String filename, byte[] content) throws IOException {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(content);
    }
}
