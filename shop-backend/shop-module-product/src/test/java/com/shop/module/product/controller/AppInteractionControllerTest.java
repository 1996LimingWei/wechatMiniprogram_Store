package com.shop.module.product.controller;

import com.shop.framework.security.LoginUser;
import com.shop.common.exception.ServerException;
import com.shop.module.product.config.MaterialStorageProperties;
import com.shop.module.product.service.MaterialFileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppInteractionControllerTest {
    @Test
    void shouldAllowReviewForCompletedOrderEvenIfProductIsOffSale() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenReturn(List.of(88L));
        login(7L);
        try {
            assertEquals(0, new AppInteractionController(jdbc, mock(MaterialFileStorageService.class), new MaterialStorageProperties())
                    .postComment(9L, "收货评价", "", 88L).get("code"));
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void shouldRejectReviewWithoutOwnedCompletedOrder() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenReturn(List.of());
        login(7L);
        try {
            assertThrows(ServerException.class, () -> new AppInteractionController(jdbc,
                    mock(MaterialFileStorageService.class), new MaterialStorageProperties()).postComment(9L, "评价", "", 88L));
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void shouldRejectSpoofedImageMimeBeforeStorage() {
        MaterialFileStorageService storage = mock(MaterialFileStorageService.class);
        login(7L);
        try {
            ServerException error = assertThrows(ServerException.class, () -> new AppInteractionController(
                    mock(JdbcTemplate.class), storage, new MaterialStorageProperties()).uploadImage(
                    new MockMultipartFile("file", "fake.png", "image/png", "<html>not an image</html>".getBytes())));
            assertEquals(400, error.getCode());
            org.mockito.Mockito.verifyNoInteractions(storage);
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void shouldRejectExternalOrExcessiveReviewPictures() {
        login(7L);
        try {
            var controller = new AppInteractionController(mock(JdbcTemplate.class),
                    mock(MaterialFileStorageService.class), new MaterialStorageProperties());
            assertThrows(ServerException.class, () -> controller.postComment(9L, "评价", "https://untrusted.example/a.png", 88L));
            String picture = "http://127.0.0.1:8085/uploads/material/comment/a.png";
            assertThrows(ServerException.class, () -> controller.postComment(9L, "评价", String.join(",", java.util.Collections.nCopies(5, picture)), 88L));
        } finally { SecurityContextHolder.clearContext(); }
    }
    @Test
    void shouldReturnCommentListInMiniProgramShape() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(new java.util.HashMap<>(Map.of(
                "id", 1L, "content", "很好", "addTime", "2026-07-30", "nickname", "用户", "avatar", ""
        ))));
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(1);

        Map<String, Object> data = (Map<String, Object>) new AppInteractionController(jdbc, mock(MaterialFileStorageService.class), new MaterialStorageProperties()).commentList(1L, 1, 20, 0).get("data");
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
            Map<String, Object> result = new AppInteractionController(jdbc, mock(MaterialFileStorageService.class), new MaterialStorageProperties()).toggleCollect(0, 9L);

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
            Map<String, Object> result = new AppInteractionController(jdbc, mock(MaterialFileStorageService.class), new MaterialStorageProperties()).recordFootprint(9L);

            assertEquals(0, result.get("code"));
            verify(jdbc).update(org.mockito.ArgumentMatchers.startsWith("UPDATE member_footprint SET deleted=0"), eq(7L), eq(9L), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void commentUsesSelectedOrderAndCurrentUser() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenReturn(List.of(88L));
        login(7L);
        try {
            new AppInteractionController(jdbc, mock(MaterialFileStorageService.class), new MaterialStorageProperties())
                    .postComment(9L, "很好", "", 88L);
            verify(jdbc).queryForList(anyString(), eq(Long.class), eq(7L), eq(9L), eq(88L), eq(88L), eq(9L), eq(7L));
            verify(jdbc).update(anyString(), eq(7L), eq(88L), eq(9L), eq("很好"), org.mockito.ArgumentMatchers.isNull());
        } finally { SecurityContextHolder.clearContext(); }
    }

    private void login(Long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }
}
