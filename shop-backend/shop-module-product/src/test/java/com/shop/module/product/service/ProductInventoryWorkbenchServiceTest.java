package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import com.shop.module.product.vo.InventoryStockAdjustReqVO;
import com.shop.module.product.vo.InventoryWarningStockReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProductInventoryWorkbenchServiceTest {

    @Test
    void shouldRejectInvalidWarningStock() {
        ProductInventoryWorkbenchService service = service();
        InventoryWarningStockReqVO request = new InventoryWarningStockReqVO();
        request.setSkuId(1L);
        request.setWarningStock(-1);

        ServerException exception = assertThrows(ServerException.class,
                () -> service.updateWarningStock(request));

        assertEquals(400, exception.getCode());
    }

    @Test
    void shouldRejectManualAdjustWithoutReason() {
        ProductInventoryWorkbenchService service = service();
        InventoryStockAdjustReqVO request = new InventoryStockAdjustReqVO();
        request.setSkuId(1L);
        request.setChangeQuantity(10);
        request.setReason("短");

        ServerException exception = assertThrows(ServerException.class,
                () -> service.adjustStock(request, 99L));

        assertEquals(400, exception.getCode());
    }

    @Test
    void shouldRejectZeroManualAdjust() {
        ProductInventoryWorkbenchService service = service();
        InventoryStockAdjustReqVO request = new InventoryStockAdjustReqVO();
        request.setSkuId(1L);
        request.setChangeQuantity(0);
        request.setReason("仓库盘点修正");

        ServerException exception = assertThrows(ServerException.class,
                () -> service.adjustStock(request, 99L));

        assertEquals(400, exception.getCode());
    }

    private ProductInventoryWorkbenchService service() {
        return new ProductInventoryWorkbenchService(
                mock(JdbcTemplate.class), mock(ProductInventoryService.class));
    }
}
