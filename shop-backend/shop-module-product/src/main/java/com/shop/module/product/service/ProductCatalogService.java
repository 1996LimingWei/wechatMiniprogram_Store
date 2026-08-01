package com.shop.module.product.service;

import com.shop.common.exception.ServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 根据配置选择商品目录数据来源。 */
@Service
public class ProductCatalogService {
    private final Map<String, ProductCatalogProvider> providers;
    private final String providerType;

    public ProductCatalogService(List<ProductCatalogProvider> providers,
                                 @Value("${product.provider:mock}") String providerType) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(ProductCatalogProvider::type, Function.identity()));
        this.providerType = providerType;
    }

    public ProductCatalogProvider current() {
        ProductCatalogProvider provider = providers.get(providerType);
        if (provider == null) throw new ServerException(500, "不支持的商品数据提供方: " + providerType);
        return provider;
    }
}
