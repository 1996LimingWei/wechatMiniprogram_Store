package com.shop.module.trade.service.provider;

import com.shop.common.exception.ServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 根据环境配置选择退款渠道，业务服务不感知 Mock 或真实实现。 */
@Service
public class TradeRefundProviderService {

    private final Map<String, TradeRefundProvider> providers;
    private final String providerType;

    public TradeRefundProviderService(
            List<TradeRefundProvider> providers,
            @Value("${trade.refund.provider:disabled}") String providerType) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                TradeRefundProvider::type, Function.identity()));
        this.providerType = providerType;
    }

    public String currentType() {
        return current().type();
    }

    public TradeRefundProvider.RefundResult refund(TradeRefundProvider.RefundRequest request) {
        return current().refund(request);
    }

    private TradeRefundProvider current() {
        TradeRefundProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new ServerException(500, "不支持的退款提供方: " + providerType);
        }
        return provider;
    }
}
