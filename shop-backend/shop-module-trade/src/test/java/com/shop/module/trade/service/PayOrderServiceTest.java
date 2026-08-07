package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.PayNotifyLogDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayNotifyLogMapper;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOrderServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(PayOrderDO.class, PayNotifyLogDO.class);
    }

    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private PayNotifyLogMapper payNotifyLogMapper;
    @Mock
    private TradeOrderService tradeOrderService;
    @Mock
    private WechatPayService wechatPayService;
    @Mock
    private TradeMockActionGuard tradeMockActionGuard;
    @InjectMocks
    private PayOrderService payOrderService;

    @Test
    void shouldCreatePendingPayOrderWhenPrepay() {
        TradeOrderDO order = createOrder(0, TradeOrderPayStatus.UNPAID, 2990);
        when(tradeOrderService.getUserOrder(1L, 10L)).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            PayOrderDO payOrder = invocation.getArgument(0);
            payOrder.setId(20L);
            return 1;
        }).when(payOrderMapper).insert(any(PayOrderDO.class));

        Map<String, Object> result = payOrderService.prepay(1L, 10L);

        ArgumentCaptor<PayOrderDO> captor = ArgumentCaptor.forClass(PayOrderDO.class);
        verify(payOrderMapper).insert(captor.capture());
        assertEquals(PayOrderStatus.PENDING, captor.getValue().getStatus());
        assertEquals(2990, captor.getValue().getAmount());
        assertEquals(20L, result.get("payOrderId"));
    }

    @Test
    void shouldConfirmPendingPayOrderOnlyOnce() {
        TradeOrderDO order = createOrder(0, TradeOrderPayStatus.UNPAID, 2990);
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.PENDING, 2990);
        when(tradeOrderService.getUserOrder(1L, 10L)).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(payOrderMapper.update(isNull(), any())).thenReturn(1);

        payOrderService.mockSuccess(1L, 10L);

        verify(tradeOrderService).markPaid(1L, 10L);
        verify(payOrderMapper).update(isNull(), any());
    }

    @Test
    void shouldIgnoreRepeatedSuccessfulPaymentCallback() {
        TradeOrderDO order = createOrder(1, TradeOrderPayStatus.PAID, 2990);
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.PAID, 2990);
        when(tradeOrderService.getUserOrder(1L, 10L)).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);

        payOrderService.mockSuccess(1L, 10L);

        verify(tradeOrderService, never()).markPaid(any(), any());
        verify(payOrderMapper, never()).update(isNull(), any());
    }

    @Test
    void shouldRejectCallbackForClosedPayOrder() {
        TradeOrderDO order = createOrder(4, TradeOrderPayStatus.UNPAID, 2990);
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.CLOSED, 2990);
        when(tradeOrderService.getUserOrder(1L, 10L)).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);

        assertThrows(ServerException.class, () -> payOrderService.mockSuccess(1L, 10L));

        verify(tradeOrderService, never()).markPaid(any(), any());
    }

    @Test
    void shouldRejectPaymentWhenAmountDoesNotMatchOrder() {
        TradeOrderDO order = createOrder(0, TradeOrderPayStatus.UNPAID, 2990);
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.PENDING, 2991);
        when(tradeOrderService.getUserOrder(1L, 10L)).thenReturn(order);
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);

        assertThrows(ServerException.class, () -> payOrderService.mockSuccess(1L, 10L));

        verify(tradeOrderService, never()).markPaid(any(), any());
    }

    @Test
    void shouldHandleWechatPaymentNotification() {
        LocalDateTime successTime = LocalDateTime.of(2026, 8, 7, 12, 30);
        WechatPayService.PaymentNotification notification = new WechatPayService.PaymentNotification(
                "notify-1", "TRANSACTION.SUCCESS", "P202608070001", "WX202608070001",
                "SUCCESS", 2990, successTime);
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.PENDING, 2990);
        payOrder.setPaySn("P202608070001");
        payOrder.setChannel("wx_lite");
        TradeOrderDO order = createOrder(0, TradeOrderPayStatus.UNPAID, 2990);
        doAnswer(invocation -> {
            PayNotifyLogDO notifyLog = invocation.getArgument(0);
            notifyLog.setId(40L);
            return 1;
        }).when(payNotifyLogMapper).insert(any(PayNotifyLogDO.class));
        when(payOrderMapper.selectOne(any())).thenReturn(payOrder);
        when(tradeOrderService.getUserOrder(1L, 10L)).thenReturn(order);
        when(payOrderMapper.update(isNull(), any())).thenReturn(1);
        when(payNotifyLogMapper.update(isNull(), any())).thenReturn(1);

        payOrderService.handleWechatNotification(notification, "{\"id\":\"notify-1\"}");

        verify(tradeOrderService).markPaidBySystem(1L, 10L);
        verify(payOrderMapper).update(isNull(), any());
        verify(payNotifyLogMapper).update(isNull(), any());
        assertEquals(PayOrderStatus.PAID, payOrder.getStatus());
        assertEquals("WX202608070001", payOrder.getChannelTradeNo());
        assertEquals(successTime, payOrder.getPayTime());
    }

    private TradeOrderDO createOrder(int status, int payStatus, int actualPrice) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(status);
        order.setPayStatus(payStatus);
        order.setActualPrice(actualPrice);
        return order;
    }

    private PayOrderDO createPayOrder(int status, int amount) {
        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setStatus(status);
        payOrder.setAmount(amount);
        return payOrder;
    }
}
