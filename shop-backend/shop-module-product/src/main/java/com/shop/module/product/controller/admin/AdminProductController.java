package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.service.ProductBatchOperationService;
import com.shop.module.product.service.ProductImportExportService;
import com.shop.module.product.service.ProductSpuService;
import com.shop.module.product.service.ProductAdminService;
import com.shop.module.product.service.AdminOperationSnapshotService;
import com.shop.module.product.vo.AdminOperationSnapshotRespVO;
import com.shop.module.product.vo.ProductBatchOperationReqVO;
import com.shop.module.product.vo.ProductBatchOperationRespVO;
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
import java.util.List;

@RestController
@RequestMapping("/admin-api/product/spu")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductSpuService productSpuService;
    private final ProductAdminService productAdminService;
    private final ProductImportExportService productImportExportService;
    private final ProductBatchOperationService productBatchOperationService;
    private final AdminOperationSnapshotService operationSnapshotService;

    @PostMapping("/save")
    public CommonResult<Long> save(@RequestBody ProductSaveReqVO request) {
        Long adminId = SecurityUtils.getRequiredAdminId();
        Long spuId = request == null || request.getSpu() == null ? null : request.getSpu().getId();
        var before = operationSnapshotService.captureProducts(spuId == null ? List.of() : List.of(spuId));
        Long savedId = productAdminService.saveProduct(
                request.getSpu(), request.getSkus(), adminId, request.getStockAdjustReason(), true);
        var after = operationSnapshotService.captureProducts(List.of(savedId));
        operationSnapshotService.recordProductOperation(spuId == null ? "新建商品" : "保存商品", adminId, before, after);
        return CommonResult.success(savedId);
    }

    @GetMapping("/page")
    public CommonResult<PageResult<ProductSpuDO>> page(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(productSpuService.getAdminSpuPage(pageParam, name, categoryId, status));
    }

    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response,
                               @RequestParam(defaultValue = "xlsx") String format) throws IOException {
        if ("csv".equalsIgnoreCase(format)) {
            writeAttachment(response, "商品导入模板.csv", "text/csv;charset=UTF-8",
                    productImportExportService.templateCsv());
            return;
        }
        writeAttachment(response, "商品导入模板.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                productImportExportService.templateWorkbook());
    }

    @PostMapping("/import-preview")
    public CommonResult<ProductImportPreviewRespVO> importPreview(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(defaultValue = "CREATE") String mode) {
        return CommonResult.success(productImportExportService.preview(file, mode));
    }

    @PostMapping("/import-confirm")
    public CommonResult<ProductImportPreviewRespVO> importConfirm(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(defaultValue = "CREATE") String mode) {
        Long adminId = SecurityUtils.getRequiredAdminId();
        ProductImportPreviewRespVO preview = productImportExportService.preview(file, mode);
        var before = operationSnapshotService.captureProducts(preview.getAffectedSpuIds());
        ProductImportPreviewRespVO result = productImportExportService.importProducts(file, mode, adminId);
        var after = operationSnapshotService.captureProducts(result.getAffectedSpuIds());
        operationSnapshotService.recordProductOperation("导入商品", adminId, before, after);
        return CommonResult.success(result);
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
        writeAttachment(response, "商品导出.csv", "text/csv;charset=UTF-8",
                productImportExportService.exportCsv(name, categoryId, status, startTime, endTime));
    }

    @PostMapping("/batch/status")
    public CommonResult<ProductBatchOperationRespVO> batchStatus(@RequestBody ProductBatchOperationReqVO request) {
        var before = operationSnapshotService.captureProducts(request == null ? List.of() : request.getIds());
        return CommonResult.success(recordBatchOperation("批量上下架", before,
                productBatchOperationService.updateStatus(request)));
    }

    @PostMapping("/batch/category")
    public CommonResult<ProductBatchOperationRespVO> batchCategory(@RequestBody ProductBatchOperationReqVO request) {
        var before = operationSnapshotService.captureProducts(request == null ? List.of() : request.getIds());
        return CommonResult.success(recordBatchOperation("批量调整分类", before,
                productBatchOperationService.updateCategory(request)));
    }

    @PostMapping("/batch/sort")
    public CommonResult<ProductBatchOperationRespVO> batchSort(@RequestBody ProductBatchOperationReqVO request) {
        var before = operationSnapshotService.captureProducts(request == null ? List.of() : request.getIds());
        return CommonResult.success(recordBatchOperation("批量调整排序", before,
                productBatchOperationService.updateSort(request)));
    }

    @PostMapping("/batch/price-preview")
    public CommonResult<ProductBatchOperationRespVO> batchPricePreview(@RequestBody ProductBatchOperationReqVO request) {
        return CommonResult.success(productBatchOperationService.previewPrice(request));
    }

    @PostMapping("/batch/price")
    public CommonResult<ProductBatchOperationRespVO> batchPrice(@RequestBody ProductBatchOperationReqVO request) {
        var before = operationSnapshotService.captureProducts(request == null ? List.of() : request.getIds());
        return CommonResult.success(recordBatchOperation("批量调价", before,
                productBatchOperationService.updatePrice(request, SecurityUtils.getRequiredAdminId())));
    }

    @PostMapping("/batch/stock")
    public CommonResult<ProductBatchOperationRespVO> batchStock(@RequestBody ProductBatchOperationReqVO request) {
        var before = operationSnapshotService.captureProducts(request == null ? List.of() : request.getIds());
        return CommonResult.success(recordBatchOperation("批量调库存", before,
                productBatchOperationService.updateStock(request, SecurityUtils.getRequiredAdminId())));
    }

    @GetMapping("/detail")
    public CommonResult<ProductSpuDO> detail(@RequestParam Long id) {
        return CommonResult.success(productSpuService.getSpuDetail(id));
    }

    @PostMapping("/create")
    public CommonResult<Boolean> create(@RequestBody ProductSpuDO spu) {
        Long adminId = SecurityUtils.getRequiredAdminId();
        productSpuService.createSpu(spu, adminId);
        operationSnapshotService.recordProductOperation("新建商品", adminId,
                operationSnapshotService.captureProducts(List.of()),
                operationSnapshotService.captureProducts(List.of(spu.getId())));
        return CommonResult.success(true);
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody ProductSpuDO spu) {
        Long adminId = SecurityUtils.getRequiredAdminId();
        var before = operationSnapshotService.captureProducts(spu == null || spu.getId() == null ? List.of() : List.of(spu.getId()));
        productSpuService.updateSpu(spu);
        operationSnapshotService.recordProductOperation("更新商品基础信息", adminId, before,
                operationSnapshotService.captureProducts(spu == null || spu.getId() == null ? List.of() : List.of(spu.getId())));
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        Long adminId = SecurityUtils.getRequiredAdminId();
        var before = operationSnapshotService.captureProducts(List.of(id));
        productSpuService.deleteSpu(id);
        operationSnapshotService.recordProductOperation("删除商品", adminId, before,
                operationSnapshotService.captureProducts(List.of(id)));
        return CommonResult.success(true);
    }

    @GetMapping("/rollback/latest")
    public CommonResult<List<AdminOperationSnapshotRespVO>> latestRollbacks(
            @RequestParam(defaultValue = "5") Integer limit) {
        return CommonResult.success(operationSnapshotService.listRecentProductSnapshots(limit == null ? 5 : limit));
    }

    @PostMapping("/rollback")
    public CommonResult<Boolean> rollback(@RequestParam Long snapshotId) {
        operationSnapshotService.rollbackProductSnapshot(snapshotId, SecurityUtils.getRequiredAdminId());
        return CommonResult.success(true);
    }

    private ProductBatchOperationRespVO recordBatchOperation(
            String operationLabel,
            AdminOperationSnapshotService.OperationCapture before,
            ProductBatchOperationRespVO response
    ) {
        List<Long> successIds = response.getRows().stream()
                .filter(row -> row.isSuccess())
                .map(row -> row.getId())
                .toList();
        if (!successIds.isEmpty()) {
            operationSnapshotService.recordProductOperation(operationLabel, SecurityUtils.getRequiredAdminId(),
                    operationSnapshotService.filterProducts(before, successIds),
                    operationSnapshotService.captureProducts(successIds));
        }
        return response;
    }

    private void writeAttachment(HttpServletResponse response, String filename, String contentType,
                                 byte[] content) throws IOException {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(content);
    }
}
