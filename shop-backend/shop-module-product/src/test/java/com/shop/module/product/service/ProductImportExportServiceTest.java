package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import com.shop.module.product.vo.ProductImportPreviewRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductImportExportServiceTest {

    private ProductSpuMapper productSpuMapper;
    private ProductSkuMapper productSkuMapper;
    private CategoryMapper categoryMapper;
    private ProductAdminService productAdminService;
    private MaterialAssetService materialAssetService;
    private ProductImportExportService service;

    @BeforeEach
    void setUp() {
        productSpuMapper = mock(ProductSpuMapper.class);
        productSkuMapper = mock(ProductSkuMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        productAdminService = mock(ProductAdminService.class);
        materialAssetService = mock(MaterialAssetService.class);
        service = new ProductImportExportService(
                productSpuMapper, productSkuMapper, categoryMapper, productAdminService, materialAssetService);
        CategoryDO category = new CategoryDO();
        category.setId(1L);
        category.setName("茶饮花茶");
        category.setStatus(1);
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(category));
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(productSkuMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
    }

    @Test
    void shouldGenerateCsvTemplateForCustomerImport() {
        String csv = new String(service.templateCsv(), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("商品名称"));
        assertTrue(csv.contains("SKU编码"));
        assertTrue(csv.contains("创建时间"));
    }

    @Test
    void shouldPreviewErrorsWithoutPersisting() {
        MockMultipartFile file = csvFile(header() + ",bad-category,,,,,,,,,BAD SKU,,规格,0,1,-1,未知,,\n");

        ProductImportPreviewRespVO result = service.preview(file);

        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getValidRows());
        assertEquals(1, result.getErrorRows());
        assertTrue(result.getRows().getFirst().getErrorColumns().contains("商品名称"));
        assertTrue(result.getRows().getFirst().getErrorColumns().contains("分类ID"));
        assertTrue(result.getRows().getFirst().getErrorColumns().contains("SKU编码"));
        assertTrue(result.getRows().getFirst().getErrors().stream().anyMatch(error -> error.contains("售价")));
        verify(productAdminService, never()).saveProduct(any(ProductSpuDO.class), anyList(), any(), any());
    }

    @Test
    void shouldDetectDuplicateSkuCodeInFile() {
        String content = header()
                + row("导入商品A", "DUP-001")
                + row("导入商品B", "DUP-001");

        ProductImportPreviewRespVO result = service.preview(csvFile(content));

        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getValidRows());
        assertEquals(1, result.getErrorRows());
        assertTrue(result.getRows().get(1).getErrors().getFirst().contains("第 2 行重复"));
        verify(productAdminService, never()).saveProduct(any(ProductSpuDO.class), anyList(), any(), any());
    }

    @Test
    void shouldImportTwentyProductsAfterValidation() {
        StringBuilder content = new StringBuilder(header());
        for (int i = 1; i <= 20; i++) {
            content.append(row("批量导入商品" + i, "IMPORT-" + i));
        }

        ProductImportPreviewRespVO result = service.importProducts(csvFile(content.toString()), 88L);

        assertEquals(20, result.getTotalRows());
        assertEquals(20, result.getValidRows());
        assertEquals(0, result.getErrorRows());
        assertEquals(20, result.getCreatedProductCount());
        assertEquals(20, result.getCreatedSkuCount());
        verify(productAdminService, times(20)).saveProduct(
                any(ProductSpuDO.class), anyList(), eq(88L), eq("商品导入初始化库存"));
    }

    @Test
    void shouldExportProductsWithSkuAndCreateTime() {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(10L);
        spu.setName("导出商品");
        spu.setCategoryId(1L);
        spu.setPicUrl("https://cdn.example.com/a.png");
        spu.setSliderPicUrls("[\"https://cdn.example.com/a.png\"]");
        spu.setPrice(1990);
        spu.setMarketPrice(2990);
        spu.setStock(8);
        spu.setStatus(1);
        spu.setSort(10);
        spu.setCreateTime(LocalDateTime.of(2026, 8, 16, 12, 0, 0));
        ProductSkuDO sku = new ProductSkuDO();
        sku.setSkuCode("EXPORT-001");
        sku.setProperties("[{\"name\":\"规格\",\"valueName\":\"120g\"}]");
        sku.setPrice(1990);
        sku.setMarketPrice(2990);
        sku.setStock(8);
        sku.setPicUrl("https://cdn.example.com/sku.png");
        when(productSpuMapper.selectList(any(Wrapper.class))).thenReturn(List.of(spu));
        when(productSkuMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sku));

        String csv = new String(service.exportCsv(null, null, null, null, null), StandardCharsets.UTF_8);

        assertTrue(csv.contains("创建时间"));
        assertTrue(csv.contains("EXPORT-001"));
        assertTrue(csv.contains("2026-08-16 12:00:00"));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "products.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private String header() {
        return "商品名称,分类ID,分类名称,关键词,简介,主图URL,轮播图URL(多个用|分隔),详情内容,详情图URL(多个用|分隔),"
                + "SKU编码,规格名称,规格值,售价(元),市场价(元),库存,上架状态(上架/下架),排序,SKU图片URL,创建时间\n";
    }

    private String row(String productName, String skuCode) {
        return productName + ",1,,关键词,简介,https://cdn.example.com/main.png,https://cdn.example.com/main.png,"
                + "<p>详情</p>,https://cdn.example.com/detail.png," + skuCode
                + ",规格,120g,19.90,29.90,10,下架,10,https://cdn.example.com/sku.png,\n";
    }
}
