package com.shop.framework.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_PREFIX = "shop:token:";
    private static final String USER_TOKEN_PREFIX = "shop:user-tokens:";
    private static final long TOKEN_EXPIRE_HOURS = 24 * 7; // 7 days

    private final StringRedisTemplate redisTemplate;

    public String createToken(LoginUser loginUser) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        String value = loginUser.getUserId() + ":" + loginUser.getUserType();
        redisTemplate.opsForValue().set(key, value, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        String userTokenKey = getUserTokenKey(loginUser.getUserId(), loginUser.getUserType());
        redisTemplate.opsForSet().add(userTokenKey, token);
        redisTemplate.expire(userTokenKey, TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        return token;
    }

    public LoginUser getLoginUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String key = TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(":");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(Long.parseLong(parts[0]));
        loginUser.setUserType(Integer.parseInt(parts[1]));
        return loginUser;
    }

    public void deleteToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        LoginUser loginUser = getLoginUser(token);
        redisTemplate.delete(TOKEN_PREFIX + token);
        if (loginUser != null) {
            redisTemplate.opsForSet().remove(
                    getUserTokenKey(loginUser.getUserId(), loginUser.getUserType()), token);
        }
    }

    public void deleteAllTokens(Long userId, Integer userType) {
        String userTokenKey = getUserTokenKey(userId, userType);
        Set<String> tokens = redisTemplate.opsForSet().members(userTokenKey);
        if (tokens != null && !tokens.isEmpty()) {
            redisTemplate.delete(tokens.stream().map(token -> TOKEN_PREFIX + token).toList());
        }
        redisTemplate.delete(userTokenKey);
    }

    private String getUserTokenKey(Long userId, Integer userType) {
        return USER_TOKEN_PREFIX + userType + ":" + userId;
    }
}
