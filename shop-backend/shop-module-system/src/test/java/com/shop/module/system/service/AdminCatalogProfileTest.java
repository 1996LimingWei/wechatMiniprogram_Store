package com.shop.module.system.service;

import com.shop.common.config.DeliveryMode;
import com.shop.framework.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminCatalogProfileTest {
    @Test void catalogProfileHidesTradingMenusAndOpensProducts() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(9L))).thenReturn(List.of(Map.of(
                "id", 9L, "username", "operator", "nickname", "商品运营", "avatar", "")));
        when(jdbc.queryForList(anyString(), eq(String.class), eq(9L)))
                .thenReturn(List.of("SUPER_ADMIN"), List.of("product:manage", "content:manage", "trade:manage", "dashboard:view"));
        var result = new AdminAuthService(jdbc, mock(TokenService.class), new DeliveryMode("catalog")).profile(9L);
        assertEquals("/product/spu", result.get("homePath"));
        assertEquals(List.of("product:manage", "content:manage"), result.get("permissions"));
    }
}
