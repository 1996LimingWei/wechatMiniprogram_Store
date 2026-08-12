package com.shop.module.trade.service.provider;

import com.shop.module.trade.service.WechatPayService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatTradeRefundProviderTest {

    private final WechatPayService wechatPayService = mock(WechatPayService.class);
    private final WechatTradeRefundProvider provider = new WechatTradeRefundProvider(wechatPayService);

    @Test
    void shouldMapWechatProcessingRefund() {
        when(wechatPayService.postJson(eq("/v3/refund/domestic/refunds"), any())).thenReturn(
                refundResponse("PROCESSING"));

        TradeRefundProvider.RefundResult result = provider.refund(
                new TradeRefundProvider.RefundRequest(
                        "R202608080001", "O202608080001", "P202608080001", 2990, 2990, "用户退款"));

        assertEquals(TradeRefundProvider.RefundStatus.PROCESSING, result.status());
        assertEquals("WX-R202608080001", result.providerRefundNo());
    }

    @Test
    void shouldMapWechatAbnormalRefundToFailed() {
        when(wechatPayService.getJson("/v3/refund/domestic/refunds/R202608080001")).thenReturn(
                refundResponse("ABNORMAL"));

        TradeRefundProvider.RefundResult result = provider.query(
                new TradeRefundProvider.RefundQuery(
                        "R202608080001", "WX-R202608080001", "P202608080001", 2990));

        assertEquals(TradeRefundProvider.RefundStatus.FAILED, result.status());
    }

    private Map<String, Object> refundResponse(String status) {
        return Map.of(
                "refund_id", "WX-R202608080001",
                "out_refund_no", "R202608080001",
                "out_trade_no", "P202608080001",
                "status", status,
                "amount", Map.of("refund", 2990, "total", 2990, "currency", "CNY"));
    }
}
