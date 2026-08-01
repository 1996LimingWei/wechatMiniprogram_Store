package com.shop.module.product.service;

import com.shop.framework.security.LoginUser;
import com.shop.module.product.dal.dataobject.ProductSearchHistoryDO;
import com.shop.module.product.dal.mysql.ProductSearchHistoryMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSearchServiceTest {
    private final ProductSearchHistoryMapper historyMapper = mock(ProductSearchHistoryMapper.class);
    private final ProductSpuMapper productSpuMapper = mock(ProductSpuMapper.class);
    private final ProductSearchService service = new ProductSearchService(historyMapper, productSpuMapper);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserHistoryAndDatabaseHotKeywords() {
        login(11L);
        ProductSearchHistoryDO history = new ProductSearchHistoryDO();
        history.setUserId(11L);
        history.setKeyword("阿胶");
        when(historyMapper.selectList(any())).thenReturn(List.of(history));
        when(productSpuMapper.selectHotKeywords(6)).thenReturn(List.of("东阿阿胶糕", "枸杞菊花茶"));

        Map<String, Object> result = service.index();

        assertEquals(List.of("阿胶"), result.get("historyKeywordList"));
        assertEquals(Map.of("keyword", "东阿阿胶糕"), result.get("defaultKeyword"));
        assertEquals(2, ((List<?>) result.get("hotKeywordList")).size());
    }

    @Test
    void shouldNotReadOrWriteHistoryForAnonymousUser() {
        when(productSpuMapper.selectHotKeywords(6)).thenReturn(List.of());

        Map<String, Object> result = service.index();
        service.record("阿胶");
        Map<String, Object> cleared = service.clearHistory();

        assertEquals(List.of(), result.get("historyKeywordList"));
        assertEquals(0, cleared.get("cleared"));
        verify(historyMapper, never()).selectList(any());
        verify(historyMapper, never()).upsert(any(), any());
    }

    @Test
    void shouldNormalizeAndUpsertKeywordForCurrentUser() {
        login(22L);

        service.record("  阿胶   糕  ");

        verify(historyMapper).upsert(22L, "阿胶 糕");
    }

    @Test
    void shouldOnlyClearCurrentUserHistory() {
        login(33L);
        when(historyMapper.clearByUserId(33L)).thenReturn(2);

        assertEquals(2, service.clearHistory().get("cleared"));
        verify(historyMapper).clearByUserId(33L);
    }

    @Test
    void shouldReturnDatabaseSuggestionsAndIgnoreBlankKeyword() {
        when(productSpuMapper.selectSearchSuggestions("阿胶", 10)).thenReturn(List.of("东阿阿胶糕"));

        assertEquals(List.of("东阿阿胶糕"), service.suggestions(" 阿胶 "));
        assertEquals(List.of(), service.suggestions("  "));
        verify(productSpuMapper, never()).selectSearchSuggestions("", 10);
    }

    private void login(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUserType(1);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, "token"));
    }
}
