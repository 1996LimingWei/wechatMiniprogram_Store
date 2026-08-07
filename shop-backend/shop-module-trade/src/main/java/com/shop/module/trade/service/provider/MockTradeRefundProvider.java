package com.shop.module.trade.service.provider;

import com.shop.common.exception.ServerException;
import org.springframework.stereotype.Component;

/** 开发环境退款实现，遵守与真实渠道相同的幂等请求契约。 */
@Component
public class MockTradeRefundProvider implements TradeRefundProvider {

    @Override
    public String type() {
        return "mock";
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        if (request.amount() == null || request.amount() <= 0) {
            throw new ServerException(400, "退款金额必须大于 0");
        }
        return new RefundResult(
                "MOCK-" + request.afterSaleSn(),
                RefundStatus.SUCCESS,
                "Mock 退款成功"
        );
    }

    @Override
    public RefundResult query(RefundQuery query) {
        return new RefundResult(
                query.providerRefundNo(),
                RefundStatus.SUCCESS,
                "Mock 退款查询成功"
        );
    }
}
