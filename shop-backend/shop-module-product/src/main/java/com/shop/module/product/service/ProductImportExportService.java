package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import com.shop.module.product.vo.ProductImportPreviewRespVO;
import com.shop.module.product.vo.ProductImportRowRespVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImportExportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DataFormatter EXCEL_FORMATTER = new DataFormatter();
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final int TEMPLATE_ROW_LIMIT = 1000;
    private static final String CREATE_STOCK_REASON = "商品导入初始化库存";
    private static final String UPDATE_STOCK_REASON = "商品导入批量更新";
    private static final List<String> TYPE_OPTIONS = List.of("实物");
    private static final List<String> STATUS_OPTIONS = List.of("下架", "上架");
    private static final List<String> SPEC_NAME_OPTIONS = List.of("规格", "净含量", "包装", "口味", "套餐");
    private static final List<String> HEADERS = List.of(
            "商品组编码", "商品名称", "商品类型", "分类ID", "分类名称", "关键词", "简介", "主图URL",
            "轮播图URL(多个用|分隔)", "详情说明", "详情图URL(多个用|分隔)", "SKU编码", "规格名称", "规格值",
            "售价(元)", "市场价(元)", "库存", "上架状态(上架/下架)", "排序", "SKU图片URL", "创建时间");

    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final ProductAdminService productAdminService;
    private final MaterialAssetService materialAssetService;

    public byte[] templateCsv() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        rows.add(templateExampleRow());
        return csvBytes(rows);
    }

    public byte[] templateWorkbook() {
        List<CategoryDO> activeCategories = listActiveCategories();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet dataSheet = workbook.createSheet("商品导入");
            Sheet dictSheet = workbook.createSheet("字典");
            createTemplateHeader(workbook, dataSheet);
            createTemplateExample(dataSheet);
            createInstructionSheet(workbook);
            populateDictionarySheet(dictSheet, activeCategories);
            applyDataValidation(dataSheet, activeCategories);
            for (int i = 0; i < HEADERS.size(); i++) {
                dataSheet.setColumnWidth(i, Math.min(256 * 36, Math.max(256 * 14, HEADERS.get(i).length() * 512)));
            }
            workbook.setSheetOrder("填写说明", 0);
            workbook.setActiveSheet(0);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new ServerException(500, "生成商品导入模板失败");
        }
    }

    public ProductImportPreviewRespVO preview(MultipartFile file, String importMode) {
        return parseAndValidate(file, ImportMode.of(importMode), true, 0L);
    }

    public ProductImportPreviewRespVO importProducts(MultipartFile file, String importMode, Long adminId) {
        ProductImportPreviewRespVO result = parseAndValidate(file, ImportMode.of(importMode), false, adminId);
        if (result.getErrorRows() > 0) {
            throw new ServerException(400, "导入文件仍存在错误，请先预校验通过");
        }
        return result;
    }

    public byte[] exportCsv(String name, Long categoryId, Integer status,
                            LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ProductSpuDO> wrapper = new LambdaQueryWrapper<ProductSpuDO>()
                .like(hasText(name), ProductSpuDO::getName, name)
                .eq(categoryId != null, ProductSpuDO::getCategoryId, categoryId)
                .eq(status != null, ProductSpuDO::getStatus, status)
                .ge(startTime != null, ProductSpuDO::getCreateTime, startTime)
                .le(endTime != null, ProductSpuDO::getCreateTime, endTime)
                .orderByDesc(ProductSpuDO::getCreateTime);
        List<ProductSpuDO> spus = productSpuMapper.selectList(wrapper);
        Map<Long, CategoryDO> categories = categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>())
                .stream().collect(Collectors.toMap(CategoryDO::getId, c -> c));
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        for (ProductSpuDO spu : spus) {
            List<ProductSkuDO> skus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                    .eq(ProductSkuDO::getSpuId, spu.getId())
                    .orderByAsc(ProductSkuDO::getId));
            if (skus.isEmpty()) {
                rows.add(exportRow(spu, categories.get(spu.getCategoryId()), null));
                continue;
            }
            for (ProductSkuDO sku : skus) {
                rows.add(exportRow(spu, categories.get(spu.getCategoryId()), sku));
            }
        }
        return csvBytes(rows);
    }

    private ProductImportPreviewRespVO parseAndValidate(MultipartFile file, ImportMode importMode,
                                                        boolean dryRun, Long adminId) {
        List<ImportRow> rows = parseImportFile(file);
        Map<Long, CategoryDO> activeCategoryById = listActiveCategories().stream()
                .collect(Collectors.toMap(CategoryDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<String, Long> activeCategoryNameIndex = activeCategoryById.values().stream()
                .collect(Collectors.toMap(CategoryDO::getName, CategoryDO::getId, (a, b) -> a, LinkedHashMap::new));

        Set<String> fileSkuCodes = rows.stream()
                .map(row -> row.skuCode)
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, ProductSkuDO> existingSkuIndex = loadExistingSkuIndex(fileSkuCodes);
        Map<Long, ProductSpuDO> cachedSpuIndex = new HashMap<>();
        Map<Long, List<ProductSkuDO>> cachedSpuSkuIndex = new HashMap<>();

        for (ImportRow row : rows) {
            row.existingSku = existingSkuIndex.get(row.skuCode);
            validateRow(row, importMode, activeCategoryById, activeCategoryNameIndex);
        }
        validateRowGroups(rows, importMode, cachedSpuIndex, cachedSpuSkuIndex);

        ProductImportPreviewRespVO result = buildPreview(rows, importMode, dryRun);
        if (dryRun || result.getErrorRows() > 0) {
            return result;
        }

        Map<String, List<ImportRow>> groups = groupRows(rows, importMode);
        for (List<ImportRow> group : groups.values()) {
            ImportRow first = group.getFirst();
            if (first.targetSpuId == null) {
                ProductSpuDO newSpu = buildSpu(first);
                List<ProductSkuDO> skus = group.stream()
                        .map(row -> buildSku(row, null))
                        .toList();
                productAdminService.saveProduct(newSpu, skus, adminId, CREATE_STOCK_REASON);
                result.setCreatedProductCount(result.getCreatedProductCount() + 1);
                result.setCreatedSkuCount(result.getCreatedSkuCount() + skus.size());
                result.getAffectedSpuIds().add(newSpu.getId());
                continue;
            }

            ProductSpuDO currentSpu = cachedSpuIndex.computeIfAbsent(first.targetSpuId, productSpuMapper::selectById);
            if (currentSpu == null) {
                throw new ServerException(1101, "导入目标商品不存在，请刷新后重试");
            }
            ProductSpuDO updatedSpu = buildSpu(first);
            updatedSpu.setId(currentSpu.getId());
            updatedSpu.setVideoUrl(currentSpu.getVideoUrl());
            List<ProductSkuDO> existingSkus = cachedSpuSkuIndex.computeIfAbsent(first.targetSpuId, this::listSpuSkus);
            Map<String, ProductSkuDO> existingBySkuCode = existingSkus.stream()
                    .filter(sku -> hasText(sku.getSkuCode()))
                    .collect(Collectors.toMap(ProductSkuDO::getSkuCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
            List<ProductSkuDO> requestedSkus = group.stream()
                    .map(row -> buildSku(row, existingBySkuCode.get(row.skuCode)))
                    .toList();
            productAdminService.saveProduct(updatedSpu, requestedSkus, adminId, UPDATE_STOCK_REASON);
            result.setUpdatedProductCount(result.getUpdatedProductCount() + 1);
            result.setUpdatedSkuCount(result.getUpdatedSkuCount() + (int) group.stream().filter(row -> row.existingSku != null).count());
            result.setCreatedSkuCount(result.getCreatedSkuCount() + (int) group.stream().filter(row -> row.existingSku == null).count());
            result.getAffectedSpuIds().add(currentSpu.getId());
        }
        result.setAffectedSpuIds(result.getAffectedSpuIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return result;
    }

    private ProductImportPreviewRespVO buildPreview(List<ImportRow> rows, ImportMode importMode, boolean dryRun) {
        ProductImportPreviewRespVO result = new ProductImportPreviewRespVO();
        result.setImportMode(importMode.name());
        result.setDryRun(dryRun);
        result.setTotalRows(rows.size());
        for (ImportRow row : rows) {
            ProductImportRowRespVO rowResp = row.toResp();
            result.getRows().add(rowResp);
            if (rowResp.isValid()) {
                result.setValidRows(result.getValidRows() + 1);
            } else {
                result.setErrorRows(result.getErrorRows() + 1);
            }
        }
        result.setAffectedSpuIds(new ArrayList<>(rows.stream()
                .map(row -> row.targetSpuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList()));
        return result;
    }

    private void validateRow(ImportRow row, ImportMode importMode,
                             Map<Long, CategoryDO> activeCategoryById,
                             Map<String, Long> activeCategoryNameIndex) {
        if (!hasText(row.groupCode)) {
            if (importMode == ImportMode.CREATE) {
                row.error("商品组编码", "新增模式必须填写商品组编码，同一商品的多条规格请保持一致");
            }
        } else if (!row.groupCode.matches("[A-Za-z0-9_-]{1,64}")) {
            row.error("商品组编码", "商品组编码仅支持 1 至 64 位字母、数字、下划线或连字符");
        }

        if (!hasText(row.productName)) {
            row.error("商品名称", "商品名称必填");
        } else if (row.productName.length() > 128) {
            row.error("商品名称", "商品名称不能超过 128 个字符");
        }
        row.typeValue = parseType(row.type, row);
        row.categoryId = resolveCategoryId(row, activeCategoryById, activeCategoryNameIndex);
        validateImage(row.mainPicUrl, "主图URL", true, row);
        splitUrls(row.sliderPicUrls).forEach(url -> validateImage(url, "轮播图URL", true, row));
        splitUrls(row.detailImageUrls).forEach(url -> validateImage(url, "详情图URL", true, row));
        validateImage(row.skuPicUrl, "SKU图片URL", false, row);

        if (!hasText(row.skuCode)) {
            row.error("SKU编码", "SKU编码必填");
        } else if (!row.skuCode.matches("[A-Za-z0-9_-]{1,64}")) {
            row.error("SKU编码", "SKU编码仅支持 1 至 64 位字母、数字、下划线或连字符");
        }
        row.priceCents = parseMoney(row.price, "售价", true, row);
        row.marketPriceCents = parseMoney(row.marketPrice, "市场价", false, row);
        if (row.marketPriceCents != null && row.marketPriceCents > 0
                && row.priceCents != null && row.marketPriceCents < row.priceCents) {
            row.error("市场价(元)", "市场价不能低于售价");
        }
        row.stockValue = parseInteger(row.stock, "库存", true, 0, 1_000_000, row);
        row.statusValue = parseStatus(row.status, row);
        row.sortValue = parseInteger(row.sort, "排序", false, 0, 9999, row);

        if (hasText(row.specName) != hasText(row.specValue)) {
            row.error("规格名称/规格值", "规格名称和规格值必须同时填写");
        }

        if (importMode == ImportMode.CREATE && row.existingSku != null) {
            row.error("SKU编码", "新增模式下 SKU编码 不能已存在");
        }
        if (importMode == ImportMode.UPDATE && row.existingSku == null) {
            row.error("SKU编码", "更新模式下 SKU编码 必须已存在，请先导出商品后再修改");
        }
        if (row.existingSku == null && !hasText(row.groupCode)) {
            row.error("商品组编码", "新增商品或新增规格时必须填写商品组编码");
        }
    }

    private void validateRowGroups(List<ImportRow> rows, ImportMode importMode,
                                   Map<Long, ProductSpuDO> cachedSpuIndex,
                                   Map<Long, List<ProductSkuDO>> cachedSpuSkuIndex) {
        Map<String, List<ImportRow>> groups = groupRows(rows, importMode, false);
        for (List<ImportRow> group : groups.values()) {
            if (group.stream().anyMatch(row -> !row.errors.isEmpty())) {
                continue;
            }
            validateSameField(group, ImportRow::groupKey, "商品组编码", "同一商品组内的商品组编码必须一致");
            validateSameField(group, row -> row.productName, "商品名称", "同一商品组内的商品名称必须一致");
            validateSameField(group, row -> row.typeValue, "商品类型", "同一商品组内的商品类型必须一致");
            validateSameField(group, row -> row.categoryId, "分类", "同一商品组内的分类必须一致");
            validateSameField(group, row -> normalizeText(row.keyword), "关键词", "同一商品组内的关键词必须一致");
            validateSameField(group, row -> normalizeText(row.introduction), "简介", "同一商品组内的简介必须一致");
            validateSameField(group, row -> normalizeText(row.mainPicUrl), "主图URL", "同一商品组内的主图必须一致");
            validateSameField(group, row -> normalizeText(row.sliderPicUrls), "轮播图URL", "同一商品组内的轮播图必须一致");
            validateSameField(group, row -> normalizeText(row.detailText), "详情说明", "同一商品组内的详情说明必须一致");
            validateSameField(group, row -> normalizeText(row.detailImageUrls), "详情图URL", "同一商品组内的详情图必须一致");
            validateSameField(group, row -> row.statusValue, "上架状态", "同一商品组内的上架状态必须一致");
            validateSameField(group, row -> row.sortValue, "排序", "同一商品组内的排序必须一致");

            if (group.stream().anyMatch(row -> !row.errors.isEmpty())) {
                continue;
            }
            validateModeGroup(group, importMode, cachedSpuIndex, cachedSpuSkuIndex);
        }
    }

    private void validateModeGroup(List<ImportRow> group, ImportMode importMode,
                                   Map<Long, ProductSpuDO> cachedSpuIndex,
                                   Map<Long, List<ProductSkuDO>> cachedSpuSkuIndex) {
        List<ImportRow> existingRows = group.stream().filter(row -> row.existingSku != null).toList();
        if (existingRows.isEmpty()) {
            if (importMode == ImportMode.UPDATE) {
                addGroupError(group, "SKU编码", "更新模式要求商品组内所有 SKU编码 都已存在");
            }
            return;
        }

        Long targetSpuId = existingRows.getFirst().existingSku.getSpuId();
        boolean mixedSpu = existingRows.stream().anyMatch(row -> !Objects.equals(row.existingSku.getSpuId(), targetSpuId));
        if (mixedSpu) {
            addGroupError(group, "SKU编码", "同一商品组内的已有 SKU 不能归属多个商品");
            return;
        }
        group.forEach(row -> row.targetSpuId = targetSpuId);

        List<ProductSkuDO> existingSpuSkus = cachedSpuSkuIndex.computeIfAbsent(targetSpuId, this::listSpuSkus);
        cachedSpuIndex.computeIfAbsent(targetSpuId, productSpuMapper::selectById);
        Set<String> currentCodes = existingSpuSkus.stream()
                .map(ProductSkuDO::getSkuCode)
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> providedExistingCodes = existingRows.stream()
                .map(row -> row.skuCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (importMode == ImportMode.UPDATE) {
            if (group.size() != existingSpuSkus.size() || !providedExistingCodes.equals(currentCodes)) {
                addGroupError(group, "SKU编码", "更新模式必须包含该商品全部 SKU，请先导出商品后整组修改再导入");
            }
            if (group.stream().anyMatch(row -> row.existingSku == null)) {
                addGroupError(group, "SKU编码", "更新模式不允许新增 SKU，请使用新增模式或上新后手工补充");
            }
            return;
        }

        if (!providedExistingCodes.containsAll(currentCodes)) {
            addGroupError(group, "SKU编码", "覆盖已有商品时必须包含当前全部 SKU，避免误删历史规格");
        }
    }

    private Map<String, List<ImportRow>> groupRows(List<ImportRow> rows, ImportMode importMode) {
        return groupRows(rows, importMode, true);
    }

    private Map<String, List<ImportRow>> groupRows(List<ImportRow> rows, ImportMode importMode, boolean onlyValid) {
        return rows.stream()
                .filter(row -> !onlyValid || row.errors.isEmpty())
                .filter(row -> hasText(resolveGroupKey(row, importMode)))
                .collect(Collectors.groupingBy(row -> resolveGroupKey(row, importMode),
                        LinkedHashMap::new, Collectors.toList()));
    }

    private String resolveGroupKey(ImportRow row, ImportMode importMode) {
        if (hasText(row.groupCode)) {
            return row.groupCode;
        }
        if (row.existingSku != null) {
            return "SPU-" + row.existingSku.getSpuId();
        }
        if (importMode == ImportMode.CREATE) {
            return "";
        }
        return row.productName;
    }

    private void validateSameField(List<ImportRow> group, Function<ImportRow, Object> getter,
                                   String column, String message) {
        Object firstValue = getter.apply(group.getFirst());
        for (ImportRow row : group) {
            if (!Objects.equals(firstValue, getter.apply(row))) {
                addGroupError(group, column, message);
                return;
            }
        }
    }

    private void addGroupError(List<ImportRow> group, String column, String message) {
        group.forEach(row -> row.error(column, message));
    }

    private Map<String, ProductSkuDO> loadExistingSkuIndex(Set<String> fileSkuCodes) {
        if (fileSkuCodes.isEmpty()) {
            return Map.of();
        }
        return productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                        .in(ProductSkuDO::getSkuCode, fileSkuCodes))
                .stream()
                .collect(Collectors.toMap(ProductSkuDO::getSkuCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private List<CategoryDO> listActiveCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                        .eq(CategoryDO::getStatus, 1)
                        .orderByAsc(CategoryDO::getSort)
                        .orderByAsc(CategoryDO::getId))
                .stream()
                .sorted(Comparator.comparing(CategoryDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(CategoryDO::getId))
                .toList();
    }

    private List<ProductSkuDO> listSpuSkus(Long spuId) {
        return productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                .eq(ProductSkuDO::getSpuId, spuId)
                .orderByAsc(ProductSkuDO::getId));
    }

    private Long resolveCategoryId(ImportRow row, Map<Long, CategoryDO> activeCategoryById,
                                   Map<String, Long> activeCategoryNameIndex) {
        if (hasText(row.categoryIdText)) {
            try {
                Long id = Long.parseLong(row.categoryIdText);
                if (!activeCategoryById.containsKey(id)) {
                    row.error("分类ID", "分类ID不存在或已停用");
                    return null;
                }
                return id;
            } catch (NumberFormatException exception) {
                row.error("分类ID", "分类ID必须为数字");
                return null;
            }
        }
        if (!hasText(row.categoryName)) {
            row.error("分类", "分类ID或分类名称必填其一");
            return null;
        }
        Long id = activeCategoryNameIndex.get(row.categoryName);
        if (id == null) {
            row.error("分类名称", "分类名称不存在或已停用");
        }
        return id;
    }

    private void validateImage(String url, String fieldName, boolean required, ImportRow row) {
        try {
            materialAssetService.validateBusinessImageUrl(url, fieldName, required);
        } catch (ServerException exception) {
            row.error(fieldName, exception.getMessage());
        }
    }

    private Integer parseMoney(String value, String fieldName, boolean required, ImportRow row) {
        if (!hasText(value)) {
            if (required) {
                row.error(fieldName + "(元)", fieldName + "必填");
            }
            return null;
        }
        try {
            BigDecimal cents = new BigDecimal(value.trim()).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
            int result = cents.intValueExact();
            if (result <= 0 || result > 100_000_000) {
                row.error(fieldName + "(元)", fieldName + "应大于 0 且不超过 1000000 元");
            }
            return result;
        } catch (Exception exception) {
            row.error(fieldName + "(元)", fieldName + "格式不正确");
            return null;
        }
    }

    private Integer parseInteger(String value, String fieldName, boolean required,
                                 int min, int max, ImportRow row) {
        if (!hasText(value)) {
            if (required) {
                row.error(fieldName, fieldName + "必填");
                return null;
            }
            return min;
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result < min || result > max) {
                row.error(fieldName, fieldName + "应为 " + min + " 至 " + max);
            }
            return result;
        } catch (NumberFormatException exception) {
            row.error(fieldName, fieldName + "必须为整数");
            return null;
        }
    }

    private Integer parseStatus(String value, ImportRow row) {
        if (!hasText(value)) {
            row.error("上架状态(上架/下架)", "上架状态必填");
            return null;
        }
        if ("下架".equals(value) || "0".equals(value)) {
            return 0;
        }
        if ("上架".equals(value) || "1".equals(value)) {
            return 1;
        }
        row.error("上架状态(上架/下架)", "上架状态只能填写上架、下架、1 或 0");
        return null;
    }

    private Integer parseType(String value, ImportRow row) {
        if (!hasText(value) || "实物".equals(value) || "1".equals(value)) {
            return 1;
        }
        row.error("商品类型", "当前版本仅支持实物商品，请填写实物或 1；历史非实物数据请联系维护人员处理");
        return null;
    }

    private List<ImportRow> parseImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServerException(400, "请选择 Excel 或 CSV 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServerException(400, "导入文件不能超过 2MB");
        }
        try {
            List<List<String>> records;
            if (filename.endsWith(".csv")) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
                records = parseCsv(content);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                records = parseWorkbook(file.getBytes());
            } else {
                throw new ServerException(400, "仅支持 .xlsx、.xls 或 .csv 文件");
            }
            if (records.isEmpty()) {
                throw new ServerException(400, "导入文件为空");
            }
            List<String> header = records.getFirst().stream().map(String::trim).toList();
            if (header.size() < 2 || !"商品组编码".equals(header.getFirst().trim()) || !"商品名称".equals(header.get(1).trim())) {
                throw new ServerException(400, "导入文件表头不正确，请先下载最新模板");
            }
            List<ImportRow> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                if (records.get(i).stream().allMatch(value -> !hasText(value))) {
                    continue;
                }
                rows.add(new ImportRow(i + 1, records.get(i)));
            }
            if (rows.isEmpty()) {
                throw new ServerException(400, "导入文件没有商品数据");
            }
            if (rows.size() > MAX_IMPORT_ROWS) {
                throw new ServerException(400, "单次最多导入 1000 行商品 SKU");
            }
            return rows;
        } catch (IOException exception) {
            throw new ServerException(400, "读取导入文件失败");
        }
    }

    private List<List<String>> parseWorkbook(byte[] bytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }
            List<List<String>> records = new ArrayList<>();
            int lastRowNum = sheet.getLastRowNum();
            int lastColumn = 0;
            for (int i = 0; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row != null && row.getLastCellNum() > lastColumn) {
                    lastColumn = row.getLastCellNum();
                }
            }
            for (int i = 0; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                List<String> values = new ArrayList<>();
                for (int j = 0; j < Math.max(lastColumn, HEADERS.size()); j++) {
                    Cell cell = row == null ? null : row.getCell(j);
                    values.add(cell == null ? "" : EXCEL_FORMATTER.formatCellValue(cell).trim());
                }
                records.add(values);
            }
            return records;
        } catch (Exception exception) {
            throw new ServerException(400, "Excel 文件解析失败，请确认文件未损坏");
        }
    }

    private List<List<String>> parseCsv(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                current.add(cell.toString().trim());
                cell.setLength(0);
            } else if (ch == '\n') {
                current.add(cell.toString().trim());
                records.add(current);
                current = new ArrayList<>();
                cell.setLength(0);
            } else if (ch != '\r') {
                cell.append(ch);
            }
        }
        current.add(cell.toString().trim());
        records.add(current);
        return records;
    }

    private ProductSpuDO buildSpu(ImportRow row) {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setName(row.productName);
        spu.setType(row.typeValue == null ? 1 : row.typeValue);
        spu.setCategoryId(row.categoryId);
        spu.setKeyword(row.keyword);
        spu.setIntroduction(row.introduction);
        spu.setDescription(buildDescription(row.detailText, row.detailImageUrls));
        spu.setPicUrl(row.mainPicUrl);
        List<String> sliderUrls = splitUrls(row.sliderPicUrls);
        spu.setSliderPicUrls(toJson(sliderUrls.isEmpty() ? List.of(row.mainPicUrl) : sliderUrls));
        spu.setStatus(row.statusValue == null ? 0 : row.statusValue);
        spu.setSort(row.sortValue == null ? 0 : row.sortValue);
        return spu;
    }

    private ProductSkuDO buildSku(ImportRow row, ProductSkuDO existing) {
        ProductSkuDO sku = new ProductSkuDO();
        if (existing != null) {
            sku.setId(existing.getId());
            sku.setWarningStock(existing.getWarningStock());
            sku.setWeight(existing.getWeight());
            sku.setVolume(existing.getVolume());
        }
        sku.setSkuCode(row.skuCode);
        sku.setProperties(buildProperties(row.specName, row.specValue));
        sku.setPrice(row.priceCents);
        sku.setMarketPrice(row.marketPriceCents);
        sku.setStock(row.stockValue);
        sku.setPicUrl(row.skuPicUrl);
        return sku;
    }

    private List<String> splitUrls(String raw) {
        if (!hasText(raw)) {
            return List.of();
        }
        return List.of(raw.split("\\|")).stream().map(String::trim).filter(this::hasText).toList();
    }

    private String buildDescription(String detailText, String detailImageUrls) {
        List<String> parts = new ArrayList<>();
        if (hasText(detailText)) {
            parts.add(detailText.trim());
        }
        for (String url : splitUrls(detailImageUrls)) {
            parts.add("<p><img src=\"" + escapeHtml(url) + "\" /></p>");
        }
        return String.join("\n", parts);
    }

    private String buildProperties(String specName, String specValue) {
        if (!hasText(specName) && !hasText(specValue)) {
            return "[]";
        }
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("id", 1);
        property.put("valueId", 1);
        property.put("name", specName);
        property.put("valueName", specValue);
        return toJson(List.of(property));
    }

    private List<String> exportRow(ProductSpuDO spu, CategoryDO category, ProductSkuDO sku) {
        SpecValue spec = parseSpec(sku == null ? "" : sku.getProperties());
        return List.of(
                "SPU-" + spu.getId(),
                value(spu.getName()),
                Integer.valueOf(2).equals(spu.getType()) ? "虚拟" : "实物",
                String.valueOf(spu.getCategoryId()),
                category == null ? "" : category.getName(),
                value(spu.getKeyword()),
                value(spu.getIntroduction()),
                value(spu.getPicUrl()),
                String.join("|", splitUrlsFromJson(spu.getSliderPicUrls())),
                value(stripImageTags(spu.getDescription())),
                String.join("|", extractDescriptionImages(spu.getDescription())),
                sku == null || sku.getSkuCode() == null ? "" : sku.getSkuCode(),
                spec.name(),
                spec.value(),
                money(sku == null ? spu.getPrice() : sku.getPrice()),
                money(sku == null ? spu.getMarketPrice() : sku.getMarketPrice()),
                String.valueOf(sku == null ? spu.getStock() : sku.getStock()),
                Integer.valueOf(1).equals(spu.getStatus()) ? "上架" : "下架",
                String.valueOf(spu.getSort()),
                sku == null ? "" : value(sku.getPicUrl()),
                formatTime(spu.getCreateTime()));
    }

    private List<String> templateExampleRow() {
        return List.of(
                "GQJH-001", "枸杞菊花茶礼盒", "实物", "", "茶饮花茶", "枸杞 菊花 礼盒", "清润回甘，适合日常茶饮",
                "https://picsum.photos/seed/demo-main/600/600",
                "https://picsum.photos/seed/demo-main/600/600|https://picsum.photos/seed/demo-slider/600/600",
                "独立小袋装，冲泡方便。", "https://picsum.photos/seed/demo-detail/750/900",
                "GQJH-LH-001", "规格", "120g", "39.80", "59.80", "100", "下架", "100",
                "https://picsum.photos/seed/demo-sku/600/600", "");
    }

    private void createTemplateHeader(Workbook workbook, Sheet dataSheet) {
        Row row = dataSheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        for (int i = 0; i < HEADERS.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS.get(i));
            cell.setCellStyle(style);
        }
        dataSheet.createFreezePane(0, 1);
    }

    private void createTemplateExample(Sheet dataSheet) {
        Row example = dataSheet.createRow(1);
        List<String> values = templateExampleRow();
        for (int i = 0; i < values.size(); i++) {
            example.createCell(i).setCellValue(values.get(i));
        }
    }

    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("填写说明");
        List<String> lines = List.of(
                "1. 建议客户始终使用本模板录入商品，再导入后台。",
                "2. 同一商品有多条规格时，请保持“商品组编码”一致；不同商品必须使用不同组编码。",
                "3. 首次上新请选择“新增商品”；后续改价、改库存、上下架，建议先导出再回填后使用“更新已有商品”。",
                "4. 更新已有商品时，必须带上该商品全部 SKU，避免误删规格。",
                "5. 商品类型、分类名称、上架状态支持下拉选择；图片列必须填写可访问的站内素材地址或 HTTP(S) 地址。",
                "6. 单次最多导入 1000 行，文件大小不超过 2MB。"
        );
        for (int i = 0; i < lines.size(); i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(lines.get(i));
        }
        sheet.setColumnWidth(0, 256 * 96);
    }

    private void populateDictionarySheet(Sheet dictSheet, List<CategoryDO> activeCategories) {
        dictSheet.createRow(0).createCell(0).setCellValue("分类名称");
        for (int i = 0; i < activeCategories.size(); i++) {
            dictSheet.createRow(i + 1).createCell(0).setCellValue(activeCategories.get(i).getName());
        }
        dictSheet.getRow(0).createCell(2).setCellValue("商品类型");
        for (int i = 0; i < TYPE_OPTIONS.size(); i++) {
            Row row = dictSheet.getRow(i + 1) == null ? dictSheet.createRow(i + 1) : dictSheet.getRow(i + 1);
            row.createCell(2).setCellValue(TYPE_OPTIONS.get(i));
        }
        dictSheet.getRow(0).createCell(4).setCellValue("上架状态");
        for (int i = 0; i < STATUS_OPTIONS.size(); i++) {
            Row row = dictSheet.getRow(i + 1) == null ? dictSheet.createRow(i + 1) : dictSheet.getRow(i + 1);
            row.createCell(4).setCellValue(STATUS_OPTIONS.get(i));
        }
        dictSheet.getRow(0).createCell(6).setCellValue("规格名称");
        for (int i = 0; i < SPEC_NAME_OPTIONS.size(); i++) {
            Row row = dictSheet.getRow(i + 1) == null ? dictSheet.createRow(i + 1) : dictSheet.getRow(i + 1);
            row.createCell(6).setCellValue(SPEC_NAME_OPTIONS.get(i));
        }
    }

    private void applyDataValidation(Sheet dataSheet, List<CategoryDO> activeCategories) {
        org.apache.poi.ss.usermodel.DataValidationHelper helper = dataSheet.getDataValidationHelper();
        addExplicitValidation(helper, dataSheet, TYPE_OPTIONS, 2, TEMPLATE_ROW_LIMIT);
        addExplicitValidation(helper, dataSheet, STATUS_OPTIONS, 17, TEMPLATE_ROW_LIMIT);
        addExplicitValidation(helper, dataSheet, SPEC_NAME_OPTIONS, 12, TEMPLATE_ROW_LIMIT);
        if (!activeCategories.isEmpty()) {
            List<String> categories = activeCategories.stream().map(CategoryDO::getName).toList();
            addExplicitValidation(helper, dataSheet, categories, 4, TEMPLATE_ROW_LIMIT);
        }
    }

    private void addExplicitValidation(org.apache.poi.ss.usermodel.DataValidationHelper helper, Sheet sheet,
                                       List<String> values, int columnIndex, int rowLimit) {
        var constraint = helper.createExplicitListConstraint(values.toArray(String[]::new));
        var regions = new CellRangeAddressList(1, rowLimit, columnIndex, columnIndex);
        var validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private List<String> splitUrlsFromJson(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(value, OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception exception) {
            return List.of(value);
        }
    }

    private SpecValue parseSpec(String properties) {
        if (!hasText(properties) || "[]".equals(properties.trim())) {
            return new SpecValue("", "");
        }
        try {
            List<?> values = OBJECT_MAPPER.readValue(properties, List.class);
            if (values.isEmpty() || !(values.getFirst() instanceof Map<?, ?> map)) {
                return new SpecValue("", "");
            }
            return new SpecValue(value(map.get("name")), value(map.get("valueName")));
        } catch (Exception exception) {
            return new SpecValue("", "");
        }
    }

    private List<String> extractDescriptionImages(String description) {
        if (!hasText(description)) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)<img\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1")
                .matcher(description);
        while (matcher.find()) {
            if (hasText(matcher.group(2))) {
                urls.add(matcher.group(2).trim());
            }
        }
        return urls;
    }

    private String stripImageTags(String description) {
        if (!hasText(description)) {
            return "";
        }
        return description
                .replaceAll("(?i)<img\\b[^>]*>", "")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .trim();
    }

    private byte[] csvBytes(List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        for (List<String> row : rows) {
            out.writeBytes(row.stream().map(this::csvCell).collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8));
            out.writeBytes("\n".getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private String csvCell(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private String money(Integer cents) {
        if (cents == null) {
            return "";
        }
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new ServerException(500, "商品导入数据序列化失败");
        }
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record SpecValue(String name, String value) {
    }

    private enum ImportMode {
        CREATE,
        UPDATE,
        UPSERT;

        static ImportMode of(String raw) {
            if (raw == null || raw.isBlank()) {
                return CREATE;
            }
            try {
                return ImportMode.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new ServerException(400, "导入模式不正确，仅支持 CREATE、UPDATE、UPSERT");
            }
        }
    }

    private class ImportRow {
        private final int rowNo;
        private final List<String> cells;
        private final List<String> errors = new ArrayList<>();
        private final Set<String> errorColumns = new LinkedHashSet<>();
        private ProductSkuDO existingSku;
        private Long targetSpuId;
        private Long categoryId;
        private Integer typeValue;
        private Integer priceCents;
        private Integer marketPriceCents;
        private Integer stockValue;
        private Integer statusValue;
        private Integer sortValue;
        private final String groupCode;
        private final String productName;
        private final String type;
        private final String categoryIdText;
        private final String categoryName;
        private final String keyword;
        private final String introduction;
        private final String mainPicUrl;
        private final String sliderPicUrls;
        private final String detailText;
        private final String detailImageUrls;
        private final String skuCode;
        private final String specName;
        private final String specValue;
        private final String price;
        private final String marketPrice;
        private final String stock;
        private final String status;
        private final String sort;
        private final String skuPicUrl;

        private ImportRow(int rowNo, List<String> cells) {
            this.rowNo = rowNo;
            this.cells = cells;
            groupCode = cell(0);
            productName = cell(1);
            type = cell(2);
            categoryIdText = cell(3);
            categoryName = cell(4);
            keyword = cell(5);
            introduction = cell(6);
            mainPicUrl = cell(7);
            sliderPicUrls = cell(8);
            detailText = cell(9);
            detailImageUrls = cell(10);
            skuCode = cell(11);
            specName = cell(12);
            specValue = cell(13);
            price = cell(14);
            marketPrice = cell(15);
            stock = cell(16);
            status = cell(17);
            sort = cell(18);
            skuPicUrl = cell(19);
        }

        private String cell(int index) {
            return index < cells.size() ? cells.get(index).trim() : "";
        }

        private String groupKey() {
            return groupCode;
        }

        private void error(String column, String message) {
            if (errors.contains(message)) {
                return;
            }
            errorColumns.add(column);
            errors.add(message);
        }

        private ProductImportRowRespVO toResp() {
            ProductImportRowRespVO vo = new ProductImportRowRespVO();
            vo.setRowNo(rowNo);
            vo.setValid(errors.isEmpty());
            vo.setGroupCode(groupCode);
            vo.setProductName(productName);
            vo.setCategoryName(hasText(categoryName) ? categoryName : categoryIdText);
            vo.setSkuCode(skuCode);
            vo.setSpecName(specName);
            vo.setSpecValue(specValue);
            vo.setPrice(price);
            vo.setStock(stockValue);
            vo.setErrorColumns(new ArrayList<>(errorColumns));
            vo.setErrors(errors);
            return vo;
        }
    }
}
