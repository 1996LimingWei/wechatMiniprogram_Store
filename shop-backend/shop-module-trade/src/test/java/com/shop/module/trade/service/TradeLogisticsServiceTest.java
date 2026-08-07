package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeLogisticsProvider;
import com.shop.module.trade.service.provider.TradeLogisticsProviderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeLogisticsServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeOrderDO.class, TradeOrderLogisticsDO.class);
    }

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @Mock
    private TradeLogisticsProviderService tradeLogisticsProviderService;
    @InjectMocks
    private TradeLogisticsService tradeLogisticsService;

    @Test
    void shouldReturnAdminLogisticsDetailWithTraces() {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setStatus(2);
        TradeOrderLogisticsDO logistics = new TradeOrderLogisticsDO();
        logistics.setId(20L);
        logistics.setOrderId(10L);
        logistics.setLogisticsCompany("顺丰速运");
        logistics.setLogisticsNo("SF123456");
        logistics.setDeliveryTime(LocalDateTime.of(2026, 8, 6, 10, 30));
        when(tradeOrderMapper.selectOne(any())).thenReturn(order);
        when(tradeOrderLogisticsMapper.selectOne(any())).thenReturn(logistics);
        when(tradeLogisticsProviderService.query(any())).thenReturn(List.of(
                new TradeLogisticsProvider.LogisticsTrace(
                        LocalDateTime.of(2026, 8, 6, 16, 30), "包裹运输中"),
                new TradeLogisticsProvider.LogisticsTrace(
                        LocalDateTime.of(2026, 8, 6, 10, 30), "商家已发货")
        ));

        Map<String, Object> result = tradeLogisticsService.adminQuery(10L);

        assertTrue((Boolean) result.get("hasLogistics"));
        assertEquals("顺丰速运", result.get("logisticsCompany"));
        assertEquals("SF123456", result.get("logisticsNo"));
        assertEquals(2, ((List<?>) result.get("traces")).size());
    }
}
