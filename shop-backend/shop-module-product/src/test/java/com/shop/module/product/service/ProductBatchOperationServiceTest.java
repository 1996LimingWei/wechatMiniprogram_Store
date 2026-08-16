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
import com.shop.module.product.vo.ProductBatchOperationReqVO;
import com.shop.module.product.vo.ProductBatchOperationRespVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductBatchOperationServiceTest {

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
    void shouldRejectWhenConfirmCountDoesNotMatchSelection() {
        ProductBatchOperationService service = service(
                mock(ProductSpuMapper.class), mock(ProductSkuMapper.class), mock(ProductAdminService.class));
        ProductBatchOperationReqVO request = new ProductBatchOperationReqVO();
        request.setIds(List.of(1L, 2L));
        request.setStatus(0);
        request.setConfirmCount(1);

        ServerException exception = assertThrows(ServerException.class, () -> service.updateStatus(request));

        assertEquals(400, exception.getCode());
    }

    @Test
    void shouldPreviewPriceWithoutPersisting() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductAdminService adminService = mock(ProductAdminService.class);
        when(spuMapper.selectById(1L)).thenReturn(spu(1L, "预览商品"));
        when(skuMapper.selectList(any())).thenReturn(List.of(sku(11L, 1L, 1000, 5)));
        ProductBatchOperationReqVO request = new ProductBatchOperationReqVO();
        request.setIds(List.of(1L));
        request.setPriceAdjustType("PERCENT");
        request.setPriceAdjustValue(BigDecimal.valueOf(10));

        ProductBatchOperationRespVO result = service(spuMapper, skuMapper, adminService).previewPrice(request);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1000, result.getRows().getFirst().getBeforePrice());
        assertEquals(1100, result.getRows().getFirst().getAfterPrice());
        verify(adminService, never()).saveSkus(any(), any(), any(), any());
    }

    @Test
    void shouldAdjustStockThroughProductAdminService() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductAdminService adminService = mock(ProductAdminService.class);
        when(spuMapper.selectById(1L)).thenReturn(spu(1L, "库存商品"));
        when(skuMapper.selectList(any())).thenReturn(List.of(
                sku(11L, 1L, 1000, 5),
                sku(12L, 1L, 1200, 7)));
        ProductBatchOperationReqVO request = new ProductBatchOperationReqVO();
        request.setIds(List.of(1L));
        request.setConfirmCount(1);
        request.setStockDelta(3);
        request.setReason("仓库盘点补录库存");

        ProductBatchOperationRespVO result = service(spuMapper, skuMapper, adminService)
                .updateStock(request, 99L);

        assertEquals(1, result.getSuccessCount());
        assertEquals(12, result.getRows().getFirst().getBeforeStock());
        assertEquals(18, result.getRows().getFirst().getAfterStock());
        ArgumentCaptor<List<ProductSkuDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(adminService).saveSkus(eq(1L), captor.capture(), eq(99L), eq("仓库盘点补录库存"));
        assertEquals(8, captor.getValue().get(0).getStock());
        assertEquals(10, captor.getValue().get(1).getStock());
    }

    @Test
    void shouldReturnPerItemFailureWhenProductMissing() {
        ProductSpuMapper spuMapper = mock(ProductSpuMapper.class);
        ProductBatchOperationReqVO request = new ProductBatchOperationReqVO();
        request.setIds(List.of(404L));
        request.setPriceAdjustType("FIXED_AMOUNT");
        request.setPriceAdjustValue(BigDecimal.ONE);

        ProductBatchOperationRespVO result = service(spuMapper, mock(ProductSkuMapper.class), mock(ProductAdminService.class))
                .previewPrice(request);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("商品不存在", result.getRows().getFirst().getMessage());
    }

    private ProductBatchOperationService service(ProductSpuMapper spuMapper, ProductSkuMapper skuMapper,
                                                 ProductAdminService adminService) {
        return new ProductBatchOperationService(spuMapper, skuMapper, mock(CategoryMapper.class), adminService);
    }

    private ProductSpuDO spu(Long id, String name) {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(id);
        spu.setName(name);
        spu.setStatus(1);
        spu.setPrice(1000);
        spu.setStock(5);
        return spu;
    }

    private ProductSkuDO sku(Long id, Long spuId, int price, int stock) {
        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setSkuCode("SKU-" + id);
        sku.setProperties("[]");
        sku.setPrice(price);
        sku.setMarketPrice(price + 500);
        sku.setStock(stock);
        return sku;
    }
}
