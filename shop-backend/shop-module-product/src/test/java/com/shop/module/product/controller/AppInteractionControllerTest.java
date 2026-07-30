package com.shop.module.product.controller;

import com.shop.framework.security.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppInteractionControllerTest {
    @Test
    void shouldReturnCommentListInMiniProgramShape() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(new java.util.HashMap<>(Map.of(
                "id", 1L, "content", "很好", "addTime", "2026-07-30", "nickname", "用户", "avatar", ""
        ))));
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(1);

        Map<String, Object> data = (Map<String, Object>) new AppInteractionController(jdbc).commentList(1L, 1, 20).get("data");
        List<Map<String, Object>> records = (List<Map<String, Object>>) data.get("records");

        assertEquals(1, data.get("total"));
        assertEquals("用户", ((Map<?, ?>) records.get(0).get("userInfo")).get("nickname"));
        assertEquals(List.of(), records.get(0).get("picList"));
    }

    @Test
    void shouldRestoreSoftDeletedCollectInsteadOfInsertingDuplicate() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1, 0);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        login(7L);

        try {
            Map<String, Object> result = new AppInteractionController(jdbc).toggleCollect(0, 9L);

            assertEquals("add", ((Map<?, ?>) result.get("data")).get("type"));
            verify(jdbc).update(org.mockito.ArgumentMatchers.startsWith("UPDATE member_collect SET deleted=0"), eq(7L), eq(9L));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldRestoreSoftDeletedFootprintInsteadOfInsertingDuplicate() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0, 1);
        login(7L);

        try {
            Map<String, Object> result = new AppInteractionController(jdbc).recordFootprint(9L);

            assertEquals(0, result.get("code"));
            verify(jdbc).update(org.mockito.ArgumentMatchers.startsWith("UPDATE member_footprint SET deleted=0"), eq(7L), eq(9L), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void login(Long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
}
