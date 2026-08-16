package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.vo.BatchShipReqVO;
import com.shop.module.trade.vo.BatchShipResultVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderOperationServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeOrderDO.class);
    }

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    @Mock
    private TradeLogisticsService tradeLogisticsService;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldBatchShipValidRowsAndReportInvalidStatus() {
        TradeOrderOperationService service = new TradeOrderOperationService(
                tradeOrderMapper, tradeOrderItemMapper, tradeOrderLogisticsMapper,
                tradeLogisticsService, tradeOrderLogService, jdbcTemplate);
        TradeOrderDO shippable = order("A001", 1, TradeOrderPayStatus.PAID);
        TradeOrderDO cancelled = order("A002", 4, TradeOrderPayStatus.UNPAID);
        when(tradeOrderMapper.selectOne(any()))
                .thenReturn(shippable)
                .thenReturn(cancelled);
        BatchShipReqVO request = new BatchShipReqVO();
        request.setContent("""
                订单号,物流公司,物流编码,物流单号,内部备注
                A001,顺丰速运,shunfeng,SF1234567890,
                A002,顺丰速运,shunfeng,SF1234567891,
                """);

        BatchShipResultVO result = service.batchShip(9L, request);

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(Boolean.TRUE, result.getRows().get(0).getSuccess());
        assertEquals(Boolean.FALSE, result.getRows().get(1).getSuccess());
        assertTrue(result.getRows().get(1).getMessage().contains("待发货"));
        verify(tradeLogisticsService).adminShip(eq(9L), eq(1L), any(Map.class));
    }

    @Test
    void shouldValidateOnlyWhenDryRun() {
        TradeOrderOperationService service = new TradeOrderOperationService(
                tradeOrderMapper, tradeOrderItemMapper, tradeOrderLogisticsMapper,
                tradeLogisticsService, tradeOrderLogService, jdbcTemplate);
        when(tradeOrderMapper.selectOne(any())).thenReturn(order("A001", 1, TradeOrderPayStatus.PAID));
        BatchShipReqVO request = new BatchShipReqVO();
        request.setDryRun(true);
        request.setContent("""
                订单号,物流公司,物流编码,物流单号,内部备注
                A001,顺丰速运,shunfeng,SF1234567890,
                """);

        BatchShipResultVO result = service.batchShip(9L, request);

        assertEquals(1, result.getSuccessCount());
        assertTrue(result.getRows().getFirst().getMessage().contains("校验通过"));
        verify(tradeLogisticsService, never()).adminShip(any(), any(), any());
    }

    private TradeOrderDO order(String orderSn, int status, int payStatus) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId("A001".equals(orderSn) ? 1L : 2L);
        order.setOrderSn(orderSn);
        order.setStatus(status);
        order.setPayStatus(payStatus);
        return order;
    }
}
