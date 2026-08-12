package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeAfterSaleExpireServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeAfterSaleDO.class, TradeOrderDO.class);
    }

    @Mock
    private TradeAfterSaleMapper tradeAfterSaleMapper;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @InjectMocks
    private TradeAfterSaleExpireService expireService;

    @Test
    void shouldExpireOneReturnInIndependentUnit() {
        TradeAfterSaleDO afterSale = overdueAfterSale();
        TradeOrderDO order = order();
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(afterSale);
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);

        assertTrue(expireService.expireOne(30L));

        verify(tradeOrderLogService).recordStatusChanged(
                order, TradeOrderLogService.OPERATOR_SYSTEM, 0L,
                "RETURN_DEADLINE_EXPIRED", 5, 2, "买家未在期限内寄回商品");
    }

    @Test
    void shouldRollbackOnlyCurrentReturnWhenOrderConflicts() {
        when(tradeAfterSaleMapper.selectById(30L)).thenReturn(overdueAfterSale());
        when(tradeAfterSaleMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(order());
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(0);

        assertThrows(ServerException.class, () -> expireService.expireOne(30L));
    }

    private TradeAfterSaleDO overdueAfterSale() {
        TradeAfterSaleDO value = new TradeAfterSaleDO();
        value.setId(30L);
        value.setOrderId(10L);
        value.setStatus(6);
        value.setBeforeOrderStatus(2);
        value.setReturnDeadline(LocalDateTime.now().minusHours(1));
        return value;
    }

    private TradeOrderDO order() {
        TradeOrderDO value = new TradeOrderDO();
        value.setId(10L);
        value.setStatus(5);
        value.setPayStatus(TradeOrderPayStatus.PAID);
        return value;
    }
}
