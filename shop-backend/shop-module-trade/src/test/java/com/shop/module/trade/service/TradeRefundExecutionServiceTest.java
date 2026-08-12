package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeRefundExecutionServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeAfterSaleDO.class, TradeOrderDO.class, PayOrderDO.class);
    }

    @Mock
    private TradeAfterSaleMapper tradeAfterSaleMapper;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private TradeRefundProviderService tradeRefundProviderService;
    @Mock
    private TradeAfterSaleService tradeAfterSaleService;
    @InjectMocks
    private TradeRefundExecutionService executionService;

    @Test
    void shouldCallRefundForNewReliableTask() {
        TradeAfterSaleDO afterSale = afterSale();
        TradeOrderDO order = order();
        PayOrderDO payOrder = payOrder();
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(afterSale);
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeRefundProviderService.currentType()).thenReturn("wechat");
        TradeRefundProvider.RefundResult result = new TradeRefundProvider.RefundResult(
                "WX-R001", TradeRefundProvider.RefundStatus.PROCESSING, "退款处理中");
        when(tradeRefundProviderService.refund(any())).thenReturn(result);

        assertTrue(executionService.execute(
                30L, TradeOrderLogService.OPERATOR_SYSTEM, 0L, false));

        verify(tradeRefundProviderService).refund(any());
        verify(tradeRefundProviderService, never()).query(any());
        verify(tradeAfterSaleService).applyRefundResult(
                30L, result, TradeOrderLogService.OPERATOR_SYSTEM, 0L, 1);
    }

    @Test
    void shouldQueryExistingProviderRefundInsteadOfCreatingAgain() {
        TradeAfterSaleDO afterSale = afterSale();
        afterSale.setProviderRefundNo("WX-R001");
        TradeRefundProvider.RefundResult result = new TradeRefundProvider.RefundResult(
                "WX-R001", TradeRefundProvider.RefundStatus.SUCCESS, "退款成功");
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(afterSale);
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order());
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder());
        when(tradeRefundProviderService.currentType()).thenReturn("wechat");
        when(tradeRefundProviderService.query(any())).thenReturn(result);

        assertTrue(executionService.execute(
                30L, TradeOrderLogService.OPERATOR_SYSTEM, 0L, false));

        verify(tradeRefundProviderService).query(any());
        verify(tradeRefundProviderService, never()).refund(any());
    }

    @Test
    void shouldPersistRetryStateWhenProviderCallThrows() {
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(afterSale());
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order());
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder());
        when(tradeRefundProviderService.currentType()).thenReturn("wechat");
        when(tradeRefundProviderService.refund(any())).thenThrow(new IllegalStateException("网络超时"));

        assertFalse(executionService.execute(
                30L, TradeOrderLogService.OPERATOR_SYSTEM, 0L, false));

        verify(tradeAfterSaleService).recordRefundExecutionFailure(30L, 1, "网络超时");
        verify(tradeAfterSaleService, never()).applyRefundResult(any(), any(), any(), any(), any(Integer.class));
    }

    @Test
    void shouldPersistRetryStateWhenProviderConfigurationChanged() {
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(afterSale());
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeRefundProviderService.currentType()).thenReturn("mock");

        assertFalse(executionService.execute(
                30L, TradeOrderLogService.OPERATOR_SYSTEM, 0L, false));

        verify(tradeAfterSaleService).recordRefundExecutionFailure(
                30L, 1, "退款渠道配置与售后单不一致");
        verify(tradeRefundProviderService, never()).refund(any());
        verify(tradeRefundProviderService, never()).query(any());
    }

    private TradeAfterSaleDO afterSale() {
        TradeAfterSaleDO value = new TradeAfterSaleDO();
        value.setId(30L);
        value.setOrderId(10L);
        value.setAfterSaleSn("R001");
        value.setStatus(4);
        value.setRefundProvider("wechat");
        value.setRefundAmount(2990);
        value.setRefundAttemptCount(0);
        return value;
    }

    private TradeOrderDO order() {
        TradeOrderDO value = new TradeOrderDO();
        value.setId(10L);
        value.setOrderSn("O001");
        value.setUserId(1L);
        value.setStatus(5);
        value.setPayStatus(TradeOrderPayStatus.PAID);
        return value;
    }

    private PayOrderDO payOrder() {
        PayOrderDO value = new PayOrderDO();
        value.setId(20L);
        value.setOrderId(10L);
        value.setUserId(1L);
        value.setPaySn("P001");
        value.setStatus(PayOrderStatus.PAID);
        value.setAmount(2990);
        return value;
    }
}
