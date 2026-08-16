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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImportExportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> HEADERS = List.of(
            "商品名称", "分类ID", "分类名称", "关键词", "简介", "主图URL", "轮播图URL(多个用|分隔)",
            "详情内容", "详情图URL(多个用|分隔)", "SKU编码", "规格名称", "规格值", "售价(元)",
            "市场价(元)", "库存", "上架状态(上架/下架)", "排序", "SKU图片URL", "创建时间");

    private final ProductSpuMapper productSpuMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    private final ProductAdminService productAdminService;
    private final MaterialAssetService materialAssetService;

    public byte[] templateCsv() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        rows.add(List.of("枸杞菊花茶礼盒", "", "茶饮花茶", "枸杞 菊花 礼盒", "清润回甘，适合日常茶饮",
                "https://picsum.photos/seed/demo-main/600/600",
                "https://picsum.photos/seed/demo-main/600/600|https://picsum.photos/seed/demo-slider/600/600",
                "<p>独立小袋装，冲泡方便。</p>", "https://picsum.photos/seed/demo-detail/750/900",
                "GQJH-LH-001", "规格", "120g", "39.80", "59.80", "100", "下架", "100",
                "https://picsum.photos/seed/demo-sku/600/600", ""));
        return csvBytes(rows);
    }

    public ProductImportPreviewRespVO preview(MultipartFile file) {
        return parseAndValidate(file, true, 0L);
    }

    public ProductImportPreviewRespVO importProducts(MultipartFile file, Long adminId) {
        ProductImportPreviewRespVO result = parseAndValidate(file, false, adminId);
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

    private ProductImportPreviewRespVO parseAndValidate(MultipartFile file, boolean dryRun, Long adminId) {
        List<ImportRow> rows = parseCsvFile(file);
        Map<String, Long> categoryNameIndex = categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>())
                .stream().filter(c -> Integer.valueOf(1).equals(c.getStatus()))
                .collect(Collectors.toMap(CategoryDO::getName, CategoryDO::getId, (a, b) -> a));
        Set<String> existingSkuCodes = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuDO>()
                        .isNotNull(ProductSkuDO::getSkuCode))
                .stream().map(ProductSkuDO::getSkuCode).collect(Collectors.toSet());
        Map<String, Integer> seenSkuCodes = new HashMap<>();
        ProductImportPreviewRespVO result = new ProductImportPreviewRespVO();
        result.setDryRun(dryRun);
        result.setTotalRows(rows.size());

        for (ImportRow row : rows) {
            validateRow(row, categoryNameIndex, existingSkuCodes, seenSkuCodes);
            ProductImportRowRespVO rowResp = row.toResp();
            result.getRows().add(rowResp);
            if (rowResp.isValid()) result.setValidRows(result.getValidRows() + 1);
            else result.setErrorRows(result.getErrorRows() + 1);
        }
        if (dryRun || result.getErrorRows() > 0) {
            return result;
        }

        Map<String, List<ImportRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> row.productName, LinkedHashMap::new, Collectors.toList()));
        for (List<ImportRow> group : grouped.values()) {
            ImportRow first = group.getFirst();
            ProductSpuDO spu = buildSpu(first);
            List<ProductSkuDO> skus = group.stream().map(this::buildSku).toList();
            productAdminService.saveProduct(spu, skus, adminId, "商品导入初始化库存");
            result.setCreatedProductCount(result.getCreatedProductCount() + 1);
            result.setCreatedSkuCount(result.getCreatedSkuCount() + skus.size());
        }
        return result;
    }

    private void validateRow(ImportRow row, Map<String, Long> categoryNameIndex,
                             Set<String> existingSkuCodes, Map<String, Integer> seenSkuCodes) {
        if (!hasText(row.productName)) row.error("商品名称", "商品名称必填");
        if (row.productName.length() > 128) row.error("商品名称", "商品名称不能超过 128 个字符");
        row.categoryId = resolveCategoryId(row, categoryNameIndex);
        validateImage(row.mainPicUrl, "主图URL", true, row);
        splitUrls(row.sliderPicUrls).forEach(url -> validateImage(url, "轮播图URL", true, row));
        splitUrls(row.detailImageUrls).forEach(url -> validateImage(url, "详情图URL", true, row));
        validateImage(row.skuPicUrl, "SKU图片URL", false, row);
        if (!hasText(row.skuCode)) {
            row.error("SKU编码", "SKU编码必填");
        } else if (!row.skuCode.matches("[A-Za-z0-9_-]{1,64}")) {
            row.error("SKU编码", "SKU编码仅支持 1 至 64 位字母、数字、下划线或连字符");
        } else if (existingSkuCodes.contains(row.skuCode)) {
            row.error("SKU编码", "SKU编码已存在");
        } else {
            Integer firstRow = seenSkuCodes.putIfAbsent(row.skuCode, row.rowNo);
            if (firstRow != null) row.error("SKU编码", "SKU编码与第 " + firstRow + " 行重复");
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
        if (hasText(row.specName) != hasText(row.specValue)) row.error("规格名称/规格值", "规格名称和规格值必须同时填写");
    }

    private Long resolveCategoryId(ImportRow row, Map<String, Long> categoryNameIndex) {
        if (hasText(row.categoryIdText)) {
            try {
                Long id = Long.parseLong(row.categoryIdText);
                CategoryDO category = categoryMapper.selectById(id);
                if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
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
        Long id = categoryNameIndex.get(row.categoryName);
        if (id == null) row.error("分类名称", "分类名称不存在或已停用");
        return id;
    }

    private void validateImage(String url, String fieldName, boolean required, ImportRow row) {
        try {
            materialAssetService.validateBusinessImageUrl(url, fieldName, required);
        } catch (ServerException exception) {
            row.error(fieldName, exception.getMessage());
        }
    }

    private ProductSpuDO buildSpu(ImportRow row) {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setName(row.productName);
        spu.setCategoryId(row.categoryId);
        spu.setKeyword(row.keyword);
        spu.setIntroduction(row.introduction);
        spu.setDescription(buildDescription(row.description, row.detailImageUrls));
        spu.setPicUrl(row.mainPicUrl);
        List<String> sliderUrls = splitUrls(row.sliderPicUrls);
        spu.setSliderPicUrls(toJson(sliderUrls.isEmpty() ? List.of(row.mainPicUrl) : sliderUrls));
        spu.setStatus(row.statusValue);
        spu.setSort(row.sortValue == null ? 0 : row.sortValue);
        return spu;
    }

    private ProductSkuDO buildSku(ImportRow row) {
        ProductSkuDO sku = new ProductSkuDO();
        sku.setSkuCode(row.skuCode);
        sku.setProperties(buildProperties(row.specName, row.specValue));
        sku.setPrice(row.priceCents);
        sku.setMarketPrice(row.marketPriceCents);
        sku.setStock(row.stockValue);
        sku.setPicUrl(row.skuPicUrl);
        return sku;
    }

    private List<String> splitUrls(String raw) {
        if (!hasText(raw)) return List.of();
        return List.of(raw.split("\\|")).stream().map(String::trim).filter(this::hasText).toList();
    }

    private String buildDescription(String description, String detailImageUrls) {
        List<String> parts = new ArrayList<>();
        if (hasText(description)) parts.add(description);
        for (String url : splitUrls(detailImageUrls)) {
            parts.add("<p><img src=\"" + escapeHtml(url) + "\" /></p>");
        }
        return String.join("\n", parts);
    }

    private String buildProperties(String specName, String specValue) {
        if (!hasText(specName) && !hasText(specValue)) return "[]";
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("id", 1);
        property.put("valueId", 1);
        property.put("name", specName);
        property.put("valueName", specValue);
        return toJson(List.of(property));
    }

    private Integer parseMoney(String value, String fieldName, boolean required, ImportRow row) {
        if (!hasText(value)) {
            if (required) row.error(fieldName + "(元)", fieldName + "必填");
            return null;
        }
        try {
            BigDecimal cents = new BigDecimal(value.trim()).movePointRight(2).setScale(0, RoundingMode.HALF_UP);
            int result = cents.intValueExact();
            if (result <= 0 || result > 100_000_000) row.error(fieldName + "(元)", fieldName + "应大于 0 且不超过 1000000 元");
            return result;
        } catch (Exception exception) {
            row.error(fieldName + "(元)", fieldName + "格式不正确");
            return null;
        }
    }

    private Integer parseInteger(String value, String fieldName, boolean required,
                                 int min, int max, ImportRow row) {
        if (!hasText(value)) {
            if (required) row.error(fieldName, fieldName + "必填");
            return required ? null : min;
        }
        try {
            int result = Integer.parseInt(value.trim());
            if (result < min || result > max) row.error(fieldName, fieldName + "应为 " + min + " 至 " + max);
            return result;
        } catch (NumberFormatException exception) {
            row.error(fieldName, fieldName + "必须为整数");
            return null;
        }
    }

    private Integer parseStatus(String value, ImportRow row) {
        if (!hasText(value) || "下架".equals(value) || "0".equals(value)) return 0;
        if ("上架".equals(value) || "1".equals(value)) return 1;
        row.error("上架状态(上架/下架)", "上架状态只能填写上架、下架、1 或 0");
        return 0;
    }

    private List<ImportRow> parseCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ServerException(400, "请选择 CSV 文件");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".csv")) throw new ServerException(400, "仅支持 CSV 文件");
        if (file.getSize() > 2 * 1024 * 1024) throw new ServerException(400, "导入文件不能超过 2MB");
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
            List<List<String>> records = parseCsv(content);
            if (records.isEmpty()) throw new ServerException(400, "导入文件为空");
            List<String> header = records.getFirst();
            if (header.isEmpty() || !"商品名称".equals(header.getFirst().trim())) {
                throw new ServerException(400, "导入文件表头不正确，请先下载最新模板");
            }
            List<ImportRow> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                if (records.get(i).stream().allMatch(value -> !hasText(value))) continue;
                rows.add(new ImportRow(i + 1, records.get(i)));
            }
            if (rows.isEmpty()) throw new ServerException(400, "导入文件没有商品数据");
            if (rows.size() > 1000) throw new ServerException(400, "单次最多导入 1000 行商品 SKU");
            return rows;
        } catch (IOException exception) {
            throw new ServerException(400, "读取导入文件失败");
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

    private List<String> exportRow(ProductSpuDO spu, CategoryDO category, ProductSkuDO sku) {
        SpecValue spec = parseSpec(sku == null ? "" : sku.getProperties());
        return List.of(
                value(spu.getName()), String.valueOf(spu.getCategoryId()), category == null ? "" : category.getName(),
                value(spu.getKeyword()), value(spu.getIntroduction()), value(spu.getPicUrl()),
                String.join("|", splitUrlsFromJson(spu.getSliderPicUrls())), value(spu.getDescription()), "",
                sku == null || sku.getSkuCode() == null ? "" : sku.getSkuCode(), spec.name(), spec.value(),
                money(sku == null ? spu.getPrice() : sku.getPrice()),
                money(sku == null ? spu.getMarketPrice() : sku.getMarketPrice()),
                String.valueOf(sku == null ? spu.getStock() : sku.getStock()),
                Integer.valueOf(1).equals(spu.getStatus()) ? "上架" : "下架", String.valueOf(spu.getSort()),
                sku == null ? "" : value(sku.getPicUrl()), formatTime(spu.getCreateTime()));
    }

    private List<String> splitUrlsFromJson(String value) {
        if (!hasText(value)) return List.of();
        try {
            return OBJECT_MAPPER.readValue(value, OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception exception) {
            return List.of(value);
        }
    }

    private SpecValue parseSpec(String properties) {
        if (!hasText(properties) || "[]".equals(properties.trim())) return new SpecValue("", "");
        try {
            List<?> values = OBJECT_MAPPER.readValue(properties, List.class);
            if (values.isEmpty() || !(values.getFirst() instanceof Map<?, ?> map)) return new SpecValue("", "");
            return new SpecValue(value(map.get("name")), value(map.get("valueName")));
        } catch (Exception exception) {
            return new SpecValue("", "");
        }
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
        if (cents == null) return "";
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record SpecValue(String name, String value) {
    }

    private class ImportRow {
        private final int rowNo;
        private final List<String> cells;
        private final List<String> errors = new ArrayList<>();
        private final Set<String> errorColumns = new LinkedHashSet<>();
        private Long categoryId;
        private Integer priceCents;
        private Integer marketPriceCents;
        private Integer stockValue;
        private Integer statusValue;
        private Integer sortValue;
        private final String productName;
        private final String categoryIdText;
        private final String categoryName;
        private final String keyword;
        private final String introduction;
        private final String mainPicUrl;
        private final String sliderPicUrls;
        private final String description;
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
            productName = cell(0);
            categoryIdText = cell(1);
            categoryName = cell(2);
            keyword = cell(3);
            introduction = cell(4);
            mainPicUrl = cell(5);
            sliderPicUrls = cell(6);
            description = cell(7);
            detailImageUrls = cell(8);
            skuCode = cell(9);
            specName = cell(10);
            specValue = cell(11);
            price = cell(12);
            marketPrice = cell(13);
            stock = cell(14);
            status = cell(15);
            sort = cell(16);
            skuPicUrl = cell(17);
        }

        private String cell(int index) {
            return index < cells.size() ? cells.get(index).trim() : "";
        }

        private void error(String column, String message) {
            errorColumns.add(column);
            errors.add(message);
        }

        private ProductImportRowRespVO toResp() {
            ProductImportRowRespVO vo = new ProductImportRowRespVO();
            vo.setRowNo(rowNo);
            vo.setValid(errors.isEmpty());
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
