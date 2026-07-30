package com.shop.module.product.controller;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
}
