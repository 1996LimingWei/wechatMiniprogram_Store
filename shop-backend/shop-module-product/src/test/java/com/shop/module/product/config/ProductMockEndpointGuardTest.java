package com.shop.module.product.config;

import com.shop.common.exception.ServerException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ProductMockEndpointGuardTest {
    @Test
    void allowsExplicitlyEnabledDevelopmentEndpoint() {
        ProductMockEndpointGuard guard = new ProductMockEndpointGuard(new MockEnvironment().withProperty("spring.profiles.active", "dev"), true);
        assertTrue(guard.preHandle(mock(jakarta.servlet.http.HttpServletRequest.class), mock(jakarta.servlet.http.HttpServletResponse.class), new Object()));
    }

    @Test
    void rejectsDisabledEndpoint() {
        ProductMockEndpointGuard guard = new ProductMockEndpointGuard(new MockEnvironment(), false);
        assertThrows(ServerException.class, () -> guard.preHandle(mock(jakarta.servlet.http.HttpServletRequest.class), mock(jakarta.servlet.http.HttpServletResponse.class), new Object()));
    }

    @Test
    void productionProfileAlwaysRejectsEndpoint() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductMockEndpointGuard guard = new ProductMockEndpointGuard(environment, true);
        assertThrows(ServerException.class, () -> guard.preHandle(mock(jakarta.servlet.http.HttpServletRequest.class), mock(jakarta.servlet.http.HttpServletResponse.class), new Object()));
    }
}
