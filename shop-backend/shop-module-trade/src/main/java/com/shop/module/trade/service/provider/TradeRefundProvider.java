package com.shop.module.trade.service.provider;

/** 退款渠道统一契约，真实渠道必须以 afterSaleSn 保证幂等。 */
public interface TradeRefundProvider {

    String type();

    RefundResult refund(RefundRequest request);

    enum RefundStatus {
        SUCCESS,
        PROCESSING
    }

    record RefundRequest(
            String afterSaleSn,
            String orderSn,
            String paySn,
            Integer amount,
            String reason) {
    }

    record RefundResult(
            String providerRefundNo,
            RefundStatus status,
            String message) {
    }
}
