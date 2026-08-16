package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.PayNotifyLogDO;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayNotifyLogMapper;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayExceptionWorkbenchServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(PayOrderDO.class, PayNotifyLogDO.class, TradeOrderDO.class);
    }

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private PayNotifyLogMapper payNotifyLogMapper;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private PayOrderService payOrderService;
    @Mock
    private WechatPayService wechatPayService;
    @InjectMocks
    private PayExceptionWorkbenchService payExceptionWorkbenchService;

    @Test
    void shouldManualSyncWechatPaidLocalUnpaidOrder() {
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.PENDING, 2990);
        TradeOrderDO order = createOrder(0, TradeOrderPayStatus.UNPAID, 2990);
        when(payOrderMapper.selectById(20L)).thenReturn(payOrder);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order);
        when(wechatPayService.isEnabled()).thenReturn(true);
        when(wechatPayService.queryPayment("P202608160001")).thenReturn(
                new WechatPayService.PaymentQueryResult(
                        "P202608160001", "WX202608160001", "SUCCESS", 2990,
                        LocalDateTime.of(2026, 8, 16, 10, 0), "{\"trade_state\":\"SUCCESS\"}"));

        Map<String, Object> result = payExceptionWorkbenchService.manualSync(99L, 20L);

        assertEquals(true, result.get("success"));
        verify(payOrderService).handleWechatNotification(any(WechatPayService.PaymentNotification.class), anyString());
        verify(jdbcTemplate).update(anyString(), eq(20L), eq("P202608160001"), eq(10L), eq("O202608160001"),
                eq(1L), eq(PayExceptionWorkbenchService.REASON_WECHAT_PAID_LOCAL_UNPAID), anyString(),
                eq("SUCCESS"), eq(2990), eq("WX202608160001"), eq(PayOrderStatus.PENDING),
                eq(TradeOrderPayStatus.UNPAID), eq(1), eq("MANUAL_SYNC_FIXED"), eq(99L), any());
    }

    @Test
    void shouldNotMarkPaidWhenWechatAmountMismatch() {
        PayOrderDO payOrder = createPayOrder(PayOrderStatus.PENDING, 2990);
        TradeOrderDO order = createOrder(0, TradeOrderPayStatus.UNPAID, 2990);
        when(payOrderMapper.selectById(20L)).thenReturn(payOrder);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order);
        when(wechatPayService.isEnabled()).thenReturn(true);
        when(wechatPayService.queryPayment("P202608160001")).thenReturn(
                new WechatPayService.PaymentQueryResult(
                        "P202608160001", "WX202608160001", "SUCCESS", 1990,
                        LocalDateTime.of(2026, 8, 16, 10, 0), "{\"trade_state\":\"SUCCESS\"}"));

        Map<String, Object> result = payExceptionWorkbenchService.manualSync(99L, 20L);

        assertEquals(false, result.get("success"));
        verify(payOrderService, never()).handleWechatNotification(any(), anyString());
        verify(jdbcTemplate).update(anyString(), eq(20L), eq("P202608160001"), eq(10L), eq("O202608160001"),
                eq(1L), eq(PayExceptionWorkbenchService.REASON_AMOUNT_MISMATCH), anyString(),
                eq("SUCCESS"), eq(1990), eq("WX202608160001"), eq(PayOrderStatus.PENDING),
                eq(TradeOrderPayStatus.UNPAID), eq(0), eq(""), isNull(), isNull());
    }

    private PayOrderDO createPayOrder(int status, int amount) {
        PayOrderDO payOrder = new PayOrderDO();
        payOrder.setId(20L);
        payOrder.setPaySn("P202608160001");
        payOrder.setOrderId(10L);
        payOrder.setUserId(1L);
        payOrder.setAmount(amount);
        payOrder.setChannel("wx_lite");
        payOrder.setStatus(status);
        payOrder.setCreateTime(LocalDateTime.of(2026, 8, 16, 9, 30));
        return payOrder;
    }

    private TradeOrderDO createOrder(int status, int payStatus, int actualPrice) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setOrderSn("O202608160001");
        order.setUserId(1L);
        order.setStatus(status);
        order.setPayStatus(payStatus);
        order.setActualPrice(actualPrice);
        return order;
    }
}
