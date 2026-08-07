package com.shop.module.trade.service.provider;

import com.shop.common.exception.ServerException;
import org.springframework.stereotype.Component;

/** 生产安全默认实现，未配置真实退款渠道时禁止伪造成功。 */
@Component
public class DisabledTradeRefundProvider implements TradeRefundProvider {

    @Override
    public String type() {
        return "disabled";
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new ServerException(503, "退款渠道尚未配置");
    }

    @Override
    public RefundResult query(RefundQuery query) {
        throw new ServerException(503, "退款渠道尚未配置");
    }
}
