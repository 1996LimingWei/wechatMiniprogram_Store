package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.config.TradeMockActionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeMockActionGuardTest {

    @Test
    void shouldAllowEnabledMockActionOutsideProduction() {
        TradeMockActionProperties properties = new TradeMockActionProperties();
        properties.setMockActionsEnabled(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertDoesNotThrow(() -> new TradeMockActionGuard(properties, environment).checkEnabled());
    }

    @Test
    void shouldRejectDisabledOrProductionMockAction() {
        TradeMockActionProperties disabled = new TradeMockActionProperties();
        disabled.setMockActionsEnabled(false);
        assertThrows(ServerException.class,
                () -> new TradeMockActionGuard(disabled, new MockEnvironment()).checkEnabled());

        TradeMockActionProperties enabled = new TradeMockActionProperties();
        enabled.setMockActionsEnabled(true);
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");
        assertThrows(ServerException.class,
                () -> new TradeMockActionGuard(enabled, production).checkEnabled());
    }
}
