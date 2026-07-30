package com.shop.module.trade.service;

import com.shop.module.trade.config.TradeOrderProperties;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeOrderDO.class);
    }

    @Mock
    private TradeCartService tradeCartService;
    @Mock
    private TradeCheckoutService tradeCheckoutService;
    @Mock
    private MemberAddressService memberAddressService;
    @Mock
    private TradeProductService tradeProductService;
    @Mock
    private TradeLogisticsService tradeLogisticsService;
    @Mock
    private TradeAfterSaleService tradeAfterSaleService;
    @Mock
    private TradeOrderProperties tradeOrderProperties;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @InjectMocks
    private TradeOrderService tradeOrderService;

    @Test
    void shouldClosePendingPayOrderWhenUserCancelsOrder() {
        TradeOrderDO pendingOrder = createOrder(0);
        TradeOrderDO closedOrder = createOrder(4);
        when(tradeOrderMapper.selectOne(any())).thenReturn(pendingOrder);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(closedOrder);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of());

        String result = tradeOrderService.cancelOrder(1L, 10L);

        assertEquals("订单已取消", result);
        verify(payOrderMapper).update(isNull(), any());
        verify(tradeOrderLogService).recordStatusChanged(eq(closedOrder), eq(TradeOrderLogService.OPERATOR_USER),
                eq(1L), eq("USER_CANCEL"), eq(0), eq(4), any());
    }

    @Test
    void shouldClosePendingPayOrderWhenOrderExpires() {
        TradeOrderDO pendingOrder = createOrder(0);
        TradeOrderDO closedOrder = createOrder(4);
        when(tradeOrderProperties.getUnpaidTimeoutMinutes()).thenReturn(30);
        when(tradeOrderProperties.getExpireBatchSize()).thenReturn(100);
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(pendingOrder));
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(closedOrder);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of());

        assertEquals(1, tradeOrderService.closeExpiredUnpaidOrders());

        verify(payOrderMapper).update(isNull(), any());
        verify(tradeOrderLogService).recordStatusChanged(eq(closedOrder), eq(TradeOrderLogService.OPERATOR_SYSTEM),
                eq(0L), eq("SYSTEM_CLOSE"), eq(0), eq(4), any());
    }

    private TradeOrderDO createOrder(int status) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(status);
        order.setPayStatus(TradeOrderPayStatus.UNPAID);
        return order;
    }
}
