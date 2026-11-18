package com.shop.framework.security;

import com.shop.common.config.DeliveryMode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class DeliveryModeFilterTest {
    @Test void catalogRejectsUserAndFinancialRequestsEvenWhenCalledDirectly() throws Exception {
        for (String uri : new String[]{"/app-api/order/submit", "/app-api/pay/wechat/notify",
                "/app-api/goods/list", "/admin-api/trade/order/list"}) {
            var response = new MockHttpServletResponse();
            new DeliveryModeFilter(new DeliveryMode("catalog")).doFilter(
                    new MockHttpServletRequest("POST", uri), response,
                    (request, result) -> fail("录入模式不应进入业务处理"));
            assertEquals(503, response.getStatus());
        }
    }

    @Test void catalogAllowsProductRequestsToContinueToAuthentication() throws Exception {
        var response = new MockHttpServletResponse();
        new DeliveryModeFilter(new DeliveryMode("catalog")).doFilter(
                new MockHttpServletRequest("POST", "/admin-api/product/spu/save"), response,
                (request, result) -> ((MockHttpServletResponse) result).setStatus(401));
        assertEquals(401, response.getStatus());
    }

    @Test void unknownModeFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new DeliveryMode(""));
        assertThrows(IllegalArgumentException.class, () -> new DeliveryMode("demo"));
    }
}
