package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.config.TradeMockActionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/** 对会改变支付、物流和退款状态的 Mock 接口进行统一环境守卫。 */
@Service
@RequiredArgsConstructor
public class TradeMockActionGuard {

    private final TradeMockActionProperties properties;
    private final Environment environment;

    public void checkEnabled() {
        if (environment.acceptsProfiles(Profiles.of("prod")) || !properties.isMockActionsEnabled()) {
            throw new ServerException(403, "当前环境不允许执行模拟交易操作");
        }
    }
}
