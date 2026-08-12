package com.shop.module.system.service;

import com.shop.common.exception.ServerException;
import com.shop.framework.security.LoginUser;
import com.shop.framework.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final JdbcTemplate jdbcTemplate;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${admin.auth.max-failed-attempts:5}")
    private int maxFailedAttempts = 5;

    @Value("${admin.auth.lock-minutes:15}")
    private int lockMinutes = 15;

    @Transactional(noRollbackFor = ServerException.class)
    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty() || normalizedUsername.length() > 64
                || password == null || password.isEmpty() || password.length() > 128) {
            recordLogin(null, normalizedUsername, false, request, "账号或密码错误");
            throw new ServerException(401, "管理员账号或密码错误");
        }
        List<AdminAccount> accounts = jdbcTemplate.query("""
                SELECT id, username, password, nickname, status, failed_login_count, locked_until
                  FROM sys_admin_user
                 WHERE username = ? AND deleted = b'0'
                 LIMIT 1
                """, (rs, index) -> new AdminAccount(
                rs.getLong("id"), rs.getString("username"), rs.getString("password"),
                rs.getString("nickname"), rs.getInt("status"), rs.getInt("failed_login_count"),
                rs.getTimestamp("locked_until")), normalizedUsername);
        if (accounts.isEmpty()) {
            recordLogin(null, normalizedUsername, false, request, "账号不存在");
            throw new ServerException(401, "管理员账号或密码错误");
        }
        AdminAccount account = accounts.get(0);
        if (account.status() != 1) {
            recordLogin(account.id(), account.username(), false, request, "账号已禁用");
            throw new ServerException(403, "管理员账号已禁用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (account.lockedUntil() != null && account.lockedUntil().toLocalDateTime().isAfter(now)) {
            recordLogin(account.id(), account.username(), false, request, "账号处于临时锁定状态");
            throw new ServerException(423, "登录失败次数过多，请稍后再试");
        }
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            jdbcTemplate.update("""
                    UPDATE sys_admin_user
                       SET failed_login_count = failed_login_count + 1,
                           locked_until = CASE
                               WHEN failed_login_count + 1 >= ? THEN TIMESTAMPADD(MINUTE, ?, ?)
                               ELSE locked_until
                           END
                     WHERE id = ? AND deleted = b'0'
                    """, maxFailedAttempts, lockMinutes, now, account.id());
            Integer failures = jdbcTemplate.queryForObject(
                    "SELECT failed_login_count FROM sys_admin_user WHERE id = ?",
                    Integer.class, account.id());
            boolean locked = failures != null && failures >= maxFailedAttempts;
            recordLogin(account.id(), account.username(), false, request,
                    locked ? "连续失败触发临时锁定" : "密码错误");
            throw new ServerException(401, "管理员账号或密码错误");
        }
        String ip = clientIp(request);
        int updated = jdbcTemplate.update("""
                UPDATE sys_admin_user
                   SET failed_login_count = 0, locked_until = NULL,
                       last_login_time = ?, last_login_ip = ?
                 WHERE id = ? AND status = 1 AND deleted = b'0'
                """, now, ip, account.id());
        if (updated != 1) {
            recordLogin(account.id(), account.username(), false, request, "账号状态已变化");
            throw new ServerException(403, "管理员账号已禁用");
        }
        recordLogin(account.id(), account.username(), true, request, "登录成功");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(account.id());
        loginUser.setUserType(2);
        Map<String, Object> result = buildProfile(
                account.id(), account.username(), account.nickname(), "");
        result.put("token", tokenService.createToken(loginUser));
        return result;
    }

    public Map<String, Object> profile(Long adminUserId) {
        List<Map<String, Object>> accounts = jdbcTemplate.queryForList("""
                SELECT id, username, nickname, avatar
                  FROM sys_admin_user
                 WHERE id = ? AND status = 1 AND deleted = b'0'
                """, adminUserId);
        if (accounts.isEmpty()) throw new ServerException(403, "管理员账号已禁用");
        Map<String, Object> account = accounts.get(0);
        return buildProfile(adminUserId, String.valueOf(account.get("username")),
                account.get("nickname") == null ? "" : String.valueOf(account.get("nickname")),
                account.get("avatar") == null ? "" : String.valueOf(account.get("avatar")));
    }

    public void logout(String token) {
        tokenService.deleteToken(token);
    }

    private Map<String, Object> buildProfile(
            Long adminUserId, String username, String nickname,
            String avatar) {
        List<String> roles = jdbcTemplate.queryForList("""
                SELECT DISTINCT r.code
                  FROM sys_admin_user_role ur
                  JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = b'0'
                 WHERE ur.admin_user_id = ?
                """, String.class, adminUserId);
        List<String> permissions = jdbcTemplate.queryForList("""
                SELECT DISTINCT p.code
                  FROM sys_admin_user_role ur
                  JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = b'0'
                  JOIN sys_role_permission rp ON rp.role_id = r.id
                  JOIN sys_permission p ON p.id = rp.permission_id AND p.status = 1 AND p.deleted = b'0'
                 WHERE ur.admin_user_id = ?
                """, String.class, adminUserId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userId", adminUserId);
        result.put("username", username);
        result.put("nickname", nickname == null ? "" : nickname);
        result.put("avatar", avatar == null ? "" : avatar);
        result.put("roles", roles);
        result.put("permissions", permissions);
        return result;
    }

    private void recordLogin(Long userId, String username, boolean success,
                             HttpServletRequest request, String message) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "";
        if (userAgent.length() > 512) userAgent = userAgent.substring(0, 512);
        jdbcTemplate.update("""
                INSERT INTO sys_login_log
                    (admin_user_id, username, success, ip, user_agent, message)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, username, success ? 1 : 0, clientIp(request), userAgent, message);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return ip.length() <= 64 ? ip : ip.substring(0, 64);
    }

    private record AdminAccount(Long id, String username, String passwordHash, String nickname,
                                Integer status, Integer failedLoginCount, Timestamp lockedUntil) {
    }
}
