package com.shop.module.trade.service.provider;

import com.shop.common.exception.ServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 根据环境配置选择物流轨迹来源。 */
@Service
public class TradeLogisticsProviderService {

    private final Map<String, TradeLogisticsProvider> providers;
    private final String providerType;

    public TradeLogisticsProviderService(
            List<TradeLogisticsProvider> providers,
            @Value("${trade.logistics.provider:disabled}") String providerType) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                TradeLogisticsProvider::type, Function.identity()));
        this.providerType = providerType;
    }

    public List<TradeLogisticsProvider.LogisticsTrace> query(
            TradeLogisticsProvider.LogisticsQuery query) {
        TradeLogisticsProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new ServerException(500, "不支持的物流提供方: " + providerType);
        }
        return provider.query(query);
    }
}
