package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.service.provider.DisabledTradeLogisticsProvider;
import com.shop.module.trade.service.provider.DisabledTradeRefundProvider;
import com.shop.module.trade.service.provider.MockTradeLogisticsProvider;
import com.shop.module.trade.service.provider.MockTradeRefundProvider;
import com.shop.module.trade.service.provider.TradeLogisticsProvider;
import com.shop.module.trade.service.provider.TradeLogisticsProviderService;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeProviderContractTest {

    @Test
    void shouldSelectMockRefundProviderWithStableRefundNumber() {
        TradeRefundProviderService service = new TradeRefundProviderService(
                List.of(new MockTradeRefundProvider(), new DisabledTradeRefundProvider()), "mock");
        TradeRefundProvider.RefundRequest request = new TradeRefundProvider.RefundRequest(
                "R202608060001", "202608060001", "P202608060001", 2990, "不想要了");

        TradeRefundProvider.RefundResult first = service.refund(request);
        TradeRefundProvider.RefundResult second = service.refund(request);

        assertEquals(TradeRefundProvider.RefundStatus.SUCCESS, first.status());
        assertEquals(first.providerRefundNo(), second.providerRefundNo());
    }

    @Test
    void shouldRejectRefundWhenProductionProviderIsDisabled() {
        TradeRefundProviderService service = new TradeRefundProviderService(
                List.of(new DisabledTradeRefundProvider()), "disabled");
        TradeRefundProvider.RefundRequest request = new TradeRefundProvider.RefundRequest(
                "R202608060001", "202608060001", "P202608060001", 2990, "不想要了");

        assertThrows(ServerException.class, () -> service.refund(request));
    }

    @Test
    void shouldReturnDeterministicMockLogisticsTraces() {
        TradeLogisticsProviderService service = new TradeLogisticsProviderService(
                List.of(new MockTradeLogisticsProvider(), new DisabledTradeLogisticsProvider()), "mock");
        TradeLogisticsProvider.LogisticsQuery query = new TradeLogisticsProvider.LogisticsQuery(
                10L, "顺丰速运", "SF123456", LocalDateTime.of(2026, 8, 6, 10, 30), 3);

        List<TradeLogisticsProvider.LogisticsTrace> traces = service.query(query);

        assertEquals(4, traces.size());
        assertEquals("包裹已签收", traces.getFirst().text());
    }
}
