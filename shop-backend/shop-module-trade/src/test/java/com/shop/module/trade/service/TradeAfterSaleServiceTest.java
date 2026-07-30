package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @InjectMocks
    private TradeAfterSaleService tradeAfterSaleService;

    @Test
    void shouldRefundPayOrderOnlyOnceForRepeatedApproval() {
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
}
