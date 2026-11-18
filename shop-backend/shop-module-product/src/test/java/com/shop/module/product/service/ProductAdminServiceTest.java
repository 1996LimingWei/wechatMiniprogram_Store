package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.shop.common.exception.ServerException;
import com.shop.module.product.dal.dataobject.CategoryDO;
import com.shop.module.product.dal.dataobject.ProductSkuDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.CategoryMapper;
import com.shop.module.product.dal.mysql.ProductSkuMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductAdminServiceTest {

    @Test
    void shouldRejectCreatingVirtualProductBeforeDatabaseWrite() {
        ProductSpuDO product = spu(null);
        product.setType(2);
        ServerException error = assertThrows(ServerException.class,
                () -> service(mock(ProductSpuMapper.class), mock(ProductSkuMapper.class),
                        mock(ProductInventoryService.class)).saveProduct(product, List.of()));
        assertEquals(400, error.getCode());
    }

    @Test
    void shouldRejectVirtualProductOnSaleThroughPartialUpdate() {
        ProductSpuMapper mapper = mock(ProductSpuMapper.class);
        ProductSpuDO current = spu(10L);
        current.setType(2);
        current.setStatus(0);
        when(mapper.selectById(10L)).thenReturn(current);
        ProductSpuDO request = new ProductSpuDO();
        request.setId(10L);
        request.setStatus(1);
        ServerException error = assertThrows(ServerException.class,
                () -> service(mapper, mock(ProductSkuMapper.class), mock(ProductInventoryService.class)).updateSpu(request));
        assertEquals(400, error.getCode());
    }

    @Test
    void shouldRejectChangingPhysicalProductToVirtualByDirectApi() {
        ProductSpuMapper mapper = mock(ProductSpuMapper.class);
        ProductSpuDO current = spu(10L);
        current.setType(1);
        when(mapper.selectById(10L)).thenReturn(current);
        ProductSpuDO request = new ProductSpuDO();
        request.setId(10L);
        request.setType(2);
        ServerException error = assertThrows(ServerException.class,
                () -> service(mapper, mock(ProductSkuMapper.class), mock(ProductInventoryService.class)).updateSpu(request));
        assertEquals(400, error.getCode());
    }

    @BeforeAll
    static void initializeLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        for (Class<?> entityClass : List.of(ProductSpuDO.class, ProductSkuDO.class, CategoryDO.class)) {
            if (TableInfoHelper.getTableInfo(entityClass) == null) {
                TableInfoHelper.initTableInfo(assistant, entityClass);
            }
        }
    }

    @Test
    void shouldPreserveSkuIdentityWhenPropertiesMatch() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductInventoryService inventoryService = mock(ProductInventoryService.class);
        ProductSpuDO spu = spu(10L);
        ProductSkuDO existing = sku(20L, 10L, 5);
        ProductSkuDO requested = sku(null, null, 8);
        when(spuMapper.selectById(10L)).thenReturn(spu);
        when(skuMapper.selectList(any())).thenReturn(List.of(existing));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        when(skuMapper.selectOne(any())).thenReturn(requested);

        service(spuMapper, skuMapper, inventoryService).saveSkus(10L, List.of(requested));

        verify(skuMapper).update(isNull(), any());
        verify(inventoryService).syncSpuStock(10L);
    }

    @Test
    void shouldRejectAdminStockOverwriteAfterConcurrentSale() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSpuDO spu = spu(10L);
        ProductSkuDO existing = sku(20L, 10L, 5);
        ProductSkuDO requested = sku(20L, 10L, 8);
        when(spuMapper.selectById(10L)).thenReturn(spu);
        when(skuMapper.selectList(any())).thenReturn(List.of(existing));
        when(skuMapper.update(isNull(), any())).thenReturn(0);

        ServerException exception = assertThrows(ServerException.class,
                () -> service(spuMapper, skuMapper, mock(ProductInventoryService.class))
                        .saveSkus(10L, List.of(requested)));

        assertEquals(409, exception.getCode());
    }

    @Test
    void shouldRejectDangerousProductDescription() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        ProductSpuDO spu = spu(null);
        spu.setName("安全测试商品");
        spu.setCategoryId(8L);
        spu.setSliderPicUrls("[\"https://example.com/product.png\"]");
        spu.setDescription("<script>alert(1)</script>");
        CategoryDO category = new CategoryDO();
        category.setId(8L);
        category.setStatus(1);
        when(categoryMapper.selectById(8L)).thenReturn(category);

        ProductAdminService service = new ProductAdminService(
                spuMapper, skuMapper, categoryMapper, mock(ProductInventoryService.class),
                mock(JdbcTemplate.class), mock(MaterialAssetService.class));

        ServerException exception = assertThrows(ServerException.class,
                () -> service.saveProduct(spu, List.of(sku(null, null, 8))));
        assertEquals(400, exception.getCode());
    }

    @Test
    void shouldRecordRealAdminAndReasonForStockAdjustment() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductSpuDO spu = spu(10L);
        ProductSkuDO existing = sku(20L, 10L, 5);
        ProductSkuDO requested = sku(20L, 10L, 8);
        when(spuMapper.selectById(10L)).thenReturn(spu);
        when(skuMapper.selectList(any())).thenReturn(List.of(existing));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        when(skuMapper.selectOne(any())).thenReturn(requested);

        ProductAdminService service = new ProductAdminService(
                spuMapper, skuMapper, mock(CategoryMapper.class),
                mock(ProductInventoryService.class), jdbcTemplate, mock(MaterialAssetService.class));
        service.saveSkus(10L, List.of(requested), 99L, "仓库盘点补录库存");

        verify(jdbcTemplate).update(any(String.class),
                eq(20L), eq(10L), any(String.class), eq(3), eq(5), eq(8),
                eq(99L), eq("仓库盘点补录库存"));
    }

    @Test
    void shouldRejectStockAdjustmentWithoutReason() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSpuDO spu = spu(10L);
        ProductSkuDO existing = sku(20L, 10L, 5);
        ProductSkuDO requested = sku(20L, 10L, 8);
        when(spuMapper.selectById(10L)).thenReturn(spu);
        when(skuMapper.selectList(any())).thenReturn(List.of(existing));
        when(skuMapper.update(isNull(), any())).thenReturn(1);

        ServerException exception = assertThrows(ServerException.class,
                () -> service(spuMapper, skuMapper, mock(ProductInventoryService.class))
                        .saveSkus(10L, List.of(requested), 99L, ""));

        assertEquals(400, exception.getCode());
    }

    @Test
    void shouldSaveLegacyProductUsingItsExistingPriceAndStock() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProductSpuDO saved = legacyProduct();
        ProductSpuDO edited = legacyProduct();
        edited.setName("更新后的旧商品");
        when(spuMapper.selectOne(any())).thenReturn(saved);
        when(spuMapper.updateById(edited)).thenReturn(1);
        when(skuMapper.selectList(any())).thenReturn(List.of());
        CategoryDO category = new CategoryDO();
        category.setStatus(1);
        when(categoryMapper.selectById(8L)).thenReturn(category);
        when(skuMapper.insert(any(ProductSkuDO.class))).thenAnswer(invocation -> {
            ProductSkuDO created = invocation.getArgument(0);
            created.setId(21L);
            return 1;
        });
        when(skuMapper.selectOne(any())).thenReturn(sku(21L, 10L, 100));

        ProductAdminService service = new ProductAdminService(
                spuMapper, skuMapper, categoryMapper, mock(ProductInventoryService.class),
                jdbcTemplate, mock(MaterialAssetService.class));
        assertEquals(10L, service.saveProduct(edited, List.of(), 99L, ""));

        ArgumentCaptor<ProductSkuDO> captured = ArgumentCaptor.forClass(ProductSkuDO.class);
        verify(skuMapper).insert(captured.capture());
        assertEquals("[]", captured.getValue().getProperties());
        assertEquals(2900, captured.getValue().getPrice());
        assertEquals(3900, captured.getValue().getMarketPrice());
        assertEquals(100, captured.getValue().getStock());
        verify(jdbcTemplate).update(any(String.class),
                eq(21L), eq(10L), any(String.class), eq(100), eq(0), eq(100),
                eq(99L), eq("旧商品补齐默认规格"));
    }

    @Test
    void shouldRejectEmptySpecSaveWhenProductAlreadyHasSpecs() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSpuDO edited = legacyProduct();
        when(spuMapper.selectOne(any())).thenReturn(edited);
        when(skuMapper.selectList(any())).thenReturn(List.of(sku(20L, 10L, 100)));

        ServerException exception = assertThrows(ServerException.class,
                () -> service(spuMapper, skuMapper, mock(ProductInventoryService.class))
                        .saveProduct(edited, List.of(), 99L, ""));

        assertEquals(409, exception.getCode());
        verify(skuMapper, never()).insert(any(ProductSkuDO.class));
        verify(spuMapper, never()).updateById(any(ProductSpuDO.class));
    }

    private ProductSpuDO legacyProduct() {
        ProductSpuDO product = spu(10L);
        product.setName("旧商品");
        product.setCategoryId(8L);
        product.setPrice(2900);
        product.setMarketPrice(3900);
        product.setStock(100);
        return product;
    }

    @Test
    void rollbackDoesNotWriteHistoricalStockOrPrice() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductSpuDO current = spu(10L);
        ProductSpuDO snapshot = spu(10L);
        snapshot.setStock(100);
        snapshot.setPrice(1990);
        when(spuMapper.selectById(10L)).thenReturn(current);
        when(spuMapper.updateById(any(ProductSpuDO.class))).thenReturn(1);
        when(skuMapper.selectList(any())).thenReturn(List.of(sku(20L, 10L, 4)));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        service(spuMapper, skuMapper, mock(ProductInventoryService.class))
                .restoreProductSnapshot(snapshot, List.of(sku(20L, 10L, 100)), 1L, "回退");
        var captor = org.mockito.ArgumentCaptor.forClass(ProductSpuDO.class);
        verify(spuMapper).updateById(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getStock());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getPrice());
        var update = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(skuMapper).update(isNull(), update.capture());
        org.junit.jupiter.api.Assertions.assertFalse(update.getValue().getSqlSet().contains("stock"));
    }

    private ProductAdminService service(ProductSpuMapper spuMapper, ProductSkuMapper skuMapper,
                                        ProductInventoryService inventoryService) {
        return new ProductAdminService(spuMapper, skuMapper, mock(CategoryMapper.class),
                inventoryService, mock(JdbcTemplate.class), mock(MaterialAssetService.class));
    }

    private ProductSpuDO spu(Long id) {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(id);
        spu.setStatus(1);
        spu.setPicUrl("https://example.com/product.png");
        return spu;
    }

    private ProductSkuDO sku(Long id, Long spuId, int stock) {
        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setProperties("[{\"id\":1,\"name\":\"规格\",\"valueId\":1,\"valueName\":\"标准\"}]");
        sku.setPrice(1990);
        sku.setMarketPrice(2990);
        sku.setStock(stock);
        sku.setPicUrl("https://example.com/sku.png");
        return sku;
    }
}
