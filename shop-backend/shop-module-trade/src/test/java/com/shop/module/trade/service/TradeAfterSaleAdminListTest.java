package com.shop.module.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeAfterSaleAdminListTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeAfterSaleDO.class, TradeOrderDO.class);
    }

    @Mock
    private TradeAfterSaleMapper tradeAfterSaleMapper;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private TradeProductService tradeProductService;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @Mock
    private TradeRefundProviderService tradeRefundProviderService;
    @InjectMocks
    private TradeAfterSaleService tradeAfterSaleService;

    @Test
    void shouldReturnUserAndOrderNumberForAdminList() {
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(30L);
        afterSale.setOrderId(10L);
        afterSale.setUserId(20L);
        afterSale.setAfterSaleSn("R202608060001");
        afterSale.setType(1);
        afterSale.setStatus(0);
        afterSale.setRefundAmount(2990);
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setOrderSn("20260806000001");
        Page<TradeAfterSaleDO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(afterSale));
        when(tradeAfterSaleMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(tradeOrderMapper.selectBatchIds(any())).thenReturn(List.of(order));

        Map<String, Object> result = tradeAfterSaleService.adminList(1, 10, 0, 0L, 0L);

        @SuppressWarnings("unchecked")
        Map<String, Object> first = ((List<Map<String, Object>>) result.get("list")).getFirst();
        assertEquals(20L, first.get("userId"));
        assertEquals("20260806000001", first.get("orderSn"));
        assertEquals("29.90", first.get("refundAmount"));
    }
}
