package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.framework.security.LoginUser;
import com.shop.module.product.dal.dataobject.ProductSearchHistoryDO;
import com.shop.module.product.dal.mysql.ProductSearchHistoryMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private static final int HISTORY_LIMIT = 10;
    private static final int HOT_KEYWORD_LIMIT = 6;
    private static final int SUGGESTION_LIMIT = 10;
    private static final int KEYWORD_MAX_LENGTH = 64;

    private final ProductSearchHistoryMapper searchHistoryMapper;
    private final ProductSpuMapper productSpuMapper;

    public Map<String, Object> index() {
        Long userId = currentMemberUserId();
        List<String> historyKeywords = userId == null ? List.of() : searchHistoryMapper.selectList(
                        new LambdaQueryWrapper<ProductSearchHistoryDO>()
                                .eq(ProductSearchHistoryDO::getUserId, userId)
                                .orderByDesc(ProductSearchHistoryDO::getUpdateTime)
                                .last("LIMIT " + HISTORY_LIMIT))
                .stream().map(ProductSearchHistoryDO::getKeyword).toList();
        List<String> hotKeywords = productSpuMapper.selectHotKeywords(HOT_KEYWORD_LIMIT);
        List<Map<String, Object>> hotKeywordList = hotKeywords.stream()
                .map(keyword -> Map.<String, Object>of(
                        "keyword", keyword,
                        "isHot", hotKeywords.indexOf(keyword) < 2 ? 1 : 0))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("historyKeywordList", historyKeywords);
        result.put("hotKeywordList", hotKeywordList);
        result.put("defaultKeyword", Map.of("keyword", hotKeywords.isEmpty() ? "" : hotKeywords.get(0)));
        return result;
    }

    public List<String> suggestions(String keyword) {
        String normalized = normalize(keyword);
        return normalized.isEmpty() ? List.of() : productSpuMapper.selectSearchSuggestions(normalized, SUGGESTION_LIMIT);
    }

    public void record(String keyword) {
        Long userId = currentMemberUserId();
        String normalized = normalize(keyword);
        if (userId != null && !normalized.isEmpty()) {
            searchHistoryMapper.upsert(userId, normalized);
        }
    }

    public Map<String, Object> clearHistory() {
        Long userId = currentMemberUserId();
        int cleared = userId == null ? 0 : searchHistoryMapper.clearByUserId(userId);
        return Map.of("cleared", cleared);
    }

    private String normalize(String keyword) {
        if (keyword == null) {
            return "";
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.length() <= KEYWORD_MAX_LENGTH ? normalized : normalized.substring(0, KEYWORD_MAX_LENGTH);
    }

    private Long currentMemberUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser user
                && Integer.valueOf(1).equals(user.getUserType())) {
            return user.getUserId();
        }
        return null;
    }
}

