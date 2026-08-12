package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleItemDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeAfterSaleServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeAfterSaleDO.class, TradeOrderDO.class, PayOrderDO.class);
    }

    @Mock
    private TradeAfterSaleMapper tradeAfterSaleMapper;
    @Mock
    private TradeAfterSaleItemMapper tradeAfterSaleItemMapper;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private TradeProductService tradeProductService;
    @Mock
    private TradeRefundProviderService tradeRefundProviderService;
    @InjectMocks
    private TradeAfterSaleService tradeAfterSaleService;

    @Test
    void shouldRefundPayOrderOnlyOnceForRepeatedApproval() {
        stubSuccessfulRefund();
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(5);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        order.setActualPrice(2990);

        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(30L);
        afterSale.setOrderId(10L);
        afterSale.setUserId(1L);
        afterSale.setStatus(0);
        afterSale.setRefundAmount(2990);

        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setStatus(PayOrderStatus.PAID);
        payOrder.setAmount(2990);

        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(tradeAfterSaleMapper.selectOne(any())).thenReturn(afterSale);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(payOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);

        tradeAfterSaleService.mockApprove(1L, 10L);
        tradeAfterSaleService.mockApprove(1L, 10L);

        verify(payOrderMapper, times(1)).update(isNull(), any());
        verify(tradeOrderMapper, times(1)).update(isNull(), any());
        verify(tradeOrderLogService, times(1)).recordPayChanged(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRecoverSkuStockForRefundBeforeShipment() {
        stubSuccessfulRefund();
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(5);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        order.setActualPrice(2990);
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(30L);
        afterSale.setOrderId(10L);
        afterSale.setUserId(1L);
        afterSale.setType(1);
        afterSale.setStatus(0);
        afterSale.setRefundAmount(2990);
        afterSale.setBeforeOrderStatus(1);
        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setStatus(PayOrderStatus.PAID);
        payOrder.setAmount(2990);
        TradeAfterSaleItemDO afterSaleItem = createAfterSaleItem();

        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(tradeAfterSaleMapper.selectOne(any())).thenReturn(afterSale);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(payOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeAfterSaleItemMapper.selectList(any())).thenReturn(java.util.List.of(afterSaleItem));

        tradeAfterSaleService.mockApprove(1L, 10L);

        verify(tradeProductService).recoverStock(
                eq(200L), eq(2), eq("AFTER_SALE"), isNull(),
                eq(TradeOrderLogService.OPERATOR_USER), eq(1L));
        verify(tradeProductService).adjustSales(100L, -2);
    }

    @Test
    void shouldKeepPaymentPaidWhileRefundProviderIsProcessing() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setOrderSn("202608060001");
        order.setUserId(1L);
        order.setStatus(5);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(30L);
        afterSale.setOrderId(10L);
        afterSale.setAfterSaleSn("R202608060001");
        afterSale.setStatus(0);
        afterSale.setRefundAmount(2990);
        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setPaySn("P202608060001");
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setStatus(PayOrderStatus.PAID);
        payOrder.setAmount(2990);
        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(tradeAfterSaleMapper.selectOne(any())).thenReturn(afterSale);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeRefundProviderService.currentType()).thenReturn("wechat");
        when(tradeRefundProviderService.refund(any())).thenReturn(
                new TradeRefundProvider.RefundResult(
                        "WX-R202608060001",
                        TradeRefundProvider.RefundStatus.PROCESSING,
                        "退款已受理"));

        Map<String, Object> result = tradeAfterSaleService.mockApprove(1L, 10L);

        assertEquals(4, result.get("status"));
        assertEquals("WX-R202608060001", result.get("providerRefundNo"));
        InOrder inOrder = inOrder(tradeAfterSaleMapper, tradeRefundProviderService);
        inOrder.verify(tradeAfterSaleMapper).update(isNull(), any());
        inOrder.verify(tradeRefundProviderService).refund(any());
        verify(payOrderMapper, never()).update(isNull(), any());
        verify(tradeOrderMapper, never()).update(isNull(), any());
    }

    @Test
    void shouldCompleteProcessingRefundAfterProviderQuerySucceeds() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setOrderSn("202608070001");
        order.setUserId(1L);
        order.setStatus(5);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(30L);
        afterSale.setOrderId(10L);
        afterSale.setAfterSaleSn("R202608070001");
        afterSale.setStatus(4);
        afterSale.setRefundAmount(2990);
        afterSale.setRefundProvider("wechat");
        afterSale.setProviderRefundNo("WX-R202608070001");
        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setPaySn("P202608070001");
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setStatus(PayOrderStatus.PAID);
        payOrder.setAmount(2990);
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(afterSale);
        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeRefundProviderService.currentType()).thenReturn("wechat");
        when(tradeRefundProviderService.query(any())).thenReturn(
                new TradeRefundProvider.RefundResult(
                        "WX-R202608070001",
                        TradeRefundProvider.RefundStatus.SUCCESS,
                        "退款成功"));
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(payOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);

        Map<String, Object> result = tradeAfterSaleService.syncProcessing(9L, 30L);

        assertEquals(1, result.get("status"));
        assertEquals(TradeOrderPayStatus.REFUNDED, order.getPayStatus());
        assertEquals(PayOrderStatus.REFUNDED, payOrder.getStatus());
        verify(tradeRefundProviderService).query(any());
        verify(tradeOrderLogService).recordPayChanged(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRestoreOrderWhenWechatRefundFails() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setOrderSn("202608080001");
        order.setUserId(1L);
        order.setStatus(5);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(30L);
        afterSale.setOrderId(10L);
        afterSale.setAfterSaleSn("R202608080001");
        afterSale.setStatus(0);
        afterSale.setBeforeOrderStatus(2);
        afterSale.setRefundAmount(2990);
        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setPaySn("P202608080001");
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setStatus(PayOrderStatus.PAID);
        payOrder.setAmount(2990);
        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(tradeAfterSaleMapper.selectOne(any())).thenReturn(afterSale);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeRefundProviderService.currentType()).thenReturn("wechat");
        when(tradeRefundProviderService.refund(any())).thenReturn(
                new TradeRefundProvider.RefundResult(
                        "WX-R202608080001", TradeRefundProvider.RefundStatus.FAILED,
                        "微信退款异常"));
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);

        Map<String, Object> result = tradeAfterSaleService.mockApprove(1L, 10L);

        assertEquals(5, result.get("status"));
        assertEquals(2, order.getStatus());
        assertEquals(TradeOrderPayStatus.PAID, order.getPayStatus());
        verify(payOrderMapper, never()).update(isNull(), any());
    }

    @Test
    void shouldAllowNewApplicationAfterRejectedAfterSale() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(2);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        order.setActualPrice(2990);
        TradeOrderItemDO orderItem = new TradeOrderItemDO();
        orderItem.setId(40L);
        orderItem.setOrderId(10L);
        orderItem.setSpuId(100L);
        orderItem.setSkuId(200L);
        orderItem.setGoodsName("测试商品");
        orderItem.setPrice(2990);
        orderItem.setCount(1);
        TradeAfterSaleDO rejected = new TradeAfterSaleDO();
        rejected.setId(30L);
        rejected.setOrderId(10L);
        rejected.setStatus(2);
        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(tradeAfterSaleMapper.selectOne(any())).thenReturn(rejected);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(java.util.List.of(orderItem));
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);

        Map<String, Object> result = tradeAfterSaleService.apply(
                1L, 10L, Map.of("reason", "再次申请退款"));

        assertEquals(0, result.get("status"));
        verify(tradeAfterSaleMapper).insert(any(TradeAfterSaleDO.class));
    }

    private void stubSuccessfulRefund() {
        when(tradeRefundProviderService.currentType()).thenReturn("mock");
        when(tradeRefundProviderService.refund(any())).thenReturn(
                new TradeRefundProvider.RefundResult(
                        "MOCK-R202608060001",
                        TradeRefundProvider.RefundStatus.SUCCESS,
                        "Mock 退款成功"));
    }

    private TradeAfterSaleItemDO createAfterSaleItem() {
        TradeAfterSaleItemDO item = new TradeAfterSaleItemDO();
        item.setAfterSaleId(30L);
        item.setOrderItemId(40L);
        item.setSpuId(100L);
        item.setSkuId(200L);
        item.setGoodsName("测试商品");
        item.setSpecName("标准规格");
        item.setPrice(1495);
        item.setApplyCount(2);
        item.setRefundAmount(2990);
        return item;
    }
}
