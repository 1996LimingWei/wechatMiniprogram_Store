package com.shop.module.system.service;

import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.framework.security.TokenService;
import com.shop.module.system.vo.AdminUserPasswordReqVO;
import com.shop.module.system.vo.AdminUserRespVO;
import com.shop.module.system.vo.AdminUserSaveReqVO;
import com.shop.module.system.vo.LoginLogRespVO;
import com.shop.module.system.vo.OperationLogRespVO;
import com.shop.module.system.vo.PermissionRespVO;
import com.shop.module.system.vo.RoleRespVO;
import com.shop.module.system.vo.RoleSaveReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemAdministrationService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{2,63}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{2,63}$");

    private final JdbcTemplate jdbcTemplate;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void requireSuperAdmin(Long adminUserId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_admin_user u
                JOIN sys_admin_user_role ur ON ur.admin_user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id AND r.deleted = b'0' AND r.status = 1
                WHERE u.id = ? AND u.status = 1 AND u.deleted = b'0' AND r.code = 'SUPER_ADMIN'
                """, Integer.class, adminUserId);
        if (count == null || count < 1) throw new ServerException(403, "仅超级管理员可执行系统管理操作");
    }

    public PageResult<AdminUserRespVO> getUserPage(PageParam pageParam, String username, String nickname, Integer status) {
        if (status != null && status != 0 && status != 1) throw new ServerException(400, "账号状态不正确");
        StringBuilder where = new StringBuilder(" WHERE u.deleted = b'0'");
        List<Object> parameters = new ArrayList<>();
        if (notBlank(username)) { where.append(" AND u.username LIKE ?"); parameters.add("%" + username.trim() + "%"); }
        if (notBlank(nickname)) { where.append(" AND u.nickname LIKE ?"); parameters.add("%" + nickname.trim() + "%"); }
        if (status != null) { where.append(" AND u.status = ?"); parameters.add(status); }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_admin_user u" + where, Long.class, parameters.toArray());
        parameters.add(pageParam.getPageSize());
        parameters.add((pageParam.getPageNo() - 1) * pageParam.getPageSize());
        List<AdminUserRespVO> users = jdbcTemplate.query("""
                SELECT u.id, u.username, u.nickname, u.avatar, u.status, u.failed_login_count, u.locked_until,
                       u.last_login_time, u.last_login_ip, u.create_time
                  FROM sys_admin_user u
                """ + where + " ORDER BY u.id DESC LIMIT ? OFFSET ?", (rs, index) -> {
            AdminUserRespVO item = new AdminUserRespVO();
            item.setId(rs.getLong("id")); item.setUsername(rs.getString("username")); item.setNickname(rs.getString("nickname"));
            item.setAvatar(rs.getString("avatar")); item.setStatus(rs.getInt("status"));
            item.setFailedLoginCount(rs.getInt("failed_login_count")); item.setLockedUntil(toLocalDateTime(rs.getTimestamp("locked_until")));
            item.setLastLoginTime(toLocalDateTime(rs.getTimestamp("last_login_time"))); item.setLastLoginIp(rs.getString("last_login_ip"));
            item.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time"))); return item;
        }, parameters.toArray());
        fillUserRoles(users);
        return new PageResult<>(users, total == null ? 0L : total);
    }

    public AdminUserRespVO getUser(Long id) {
        if (id == null || id <= 0) throw new ServerException(400, "管理员编号不正确");
        List<AdminUserRespVO> users = jdbcTemplate.query("""
                SELECT id, username, nickname, avatar, status, failed_login_count, locked_until, last_login_time, last_login_ip, create_time
                  FROM sys_admin_user WHERE id = ? AND deleted = b'0'
                """, (rs, index) -> {
            AdminUserRespVO item = new AdminUserRespVO();
            item.setId(rs.getLong("id")); item.setUsername(rs.getString("username")); item.setNickname(rs.getString("nickname")); item.setAvatar(rs.getString("avatar"));
            item.setStatus(rs.getInt("status")); item.setFailedLoginCount(rs.getInt("failed_login_count")); item.setLockedUntil(toLocalDateTime(rs.getTimestamp("locked_until")));
            item.setLastLoginTime(toLocalDateTime(rs.getTimestamp("last_login_time"))); item.setLastLoginIp(rs.getString("last_login_ip")); item.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time"))); return item;
        }, id);
        if (users.isEmpty()) throw new ServerException(404, "管理员账号不存在");
        fillUserRoles(users); return users.getFirst();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveUser(AdminUserSaveReqVO request) {
        if (request == null) throw new ServerException(400, "管理员信息不能为空");
        validateUserFields(request, request.getId() == null);
        List<Long> roleIds = requireActiveRoleIds(request.getRoleIds());
        try {
            if (request.getId() == null) {
                jdbcTemplate.update("""
                        INSERT INTO sys_admin_user (username, password, nickname, avatar, status)
                        VALUES (?, ?, ?, ?, 1)
                        """, request.getUsername().trim(), passwordEncoder.encode(request.getPassword()),
                        normalize(request.getNickname()), normalize(request.getAvatar()));
                Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                replaceUserRoles(id, roleIds);
                return id;
            }
            ensureUserExists(request.getId());
            ensureSuperAdminRetained(request.getId(), roleIds, null);
            jdbcTemplate.update("UPDATE sys_admin_user SET nickname = ?, avatar = ? WHERE id = ? AND deleted = b'0'",
                    normalize(request.getNickname()), normalize(request.getAvatar()), request.getId());
            replaceUserRoles(request.getId(), roleIds);
            tokenService.deleteAllTokens(request.getId(), 2);
            return request.getId();
        } catch (DuplicateKeyException exception) {
            throw new ServerException(409, "管理员账号或角色关系已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeUserStatus(Long actorId, Long userId, Integer status) {
        if (userId == null || status == null || (status != 0 && status != 1)) throw new ServerException(400, "账号状态不正确");
        if (actorId.equals(userId) && status == 0) throw new ServerException(400, "不能禁用当前登录账号");
        ensureUserExists(userId);
        if (status == 0) ensureSuperAdminRetained(userId, null, status);
        jdbcTemplate.update("UPDATE sys_admin_user SET status = ? WHERE id = ? AND deleted = b'0'", status, userId);
        tokenService.deleteAllTokens(userId, 2);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Long userId) {
        ensureUserExists(userId);
        jdbcTemplate.update("UPDATE sys_admin_user SET failed_login_count = 0, locked_until = NULL WHERE id = ?", userId);
        tokenService.deleteAllTokens(userId, 2);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AdminUserPasswordReqVO request) {
        if (request == null || request.getId() == null) throw new ServerException(400, "管理员编号不能为空");
        validatePassword(request.getPassword()); ensureUserExists(request.getId());
        jdbcTemplate.update("UPDATE sys_admin_user SET password = ?, failed_login_count = 0, locked_until = NULL WHERE id = ?",
                passwordEncoder.encode(request.getPassword()), request.getId());
        tokenService.deleteAllTokens(request.getId(), 2);
    }

    public void forceLogout(Long userId) { ensureUserExists(userId); tokenService.deleteAllTokens(userId, 2); }

    @Transactional(rollbackFor = Exception.class)
    public void changeOwnPassword(Long adminUserId, String oldPassword, String newPassword) {
        validatePassword(newPassword);
        List<String> passwords = jdbcTemplate.queryForList("SELECT password FROM sys_admin_user WHERE id = ? AND status = 1 AND deleted = b'0'", String.class, adminUserId);
        if (passwords.isEmpty()) throw new ServerException(403, "管理员账号已禁用");
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, passwords.getFirst())) throw new ServerException(400, "原密码不正确");
        jdbcTemplate.update("UPDATE sys_admin_user SET password = ?, failed_login_count = 0, locked_until = NULL WHERE id = ?", passwordEncoder.encode(newPassword), adminUserId);
        tokenService.deleteAllTokens(adminUserId, 2);
    }

    public List<RoleRespVO> getRoles() {
        List<RoleRespVO> roles = jdbcTemplate.query("""
                SELECT id, code, name, status, create_time FROM sys_role WHERE deleted = b'0' ORDER BY id
                """, (rs, index) -> {
            RoleRespVO item = new RoleRespVO(); item.setId(rs.getLong("id")); item.setCode(rs.getString("code")); item.setName(rs.getString("name"));
            item.setStatus(rs.getInt("status")); item.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time"))); return item;
        });
        fillRolePermissions(roles); return roles;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveRole(RoleSaveReqVO request) {
        if (request == null || !notBlank(request.getName())) throw new ServerException(400, "角色名称不能为空");
        if (normalize(request.getName()).length() > 64) throw new ServerException(400, "角色名称不能超过 64 个字符");
        List<Long> permissionIds = requireActivePermissionIds(request.getPermissionIds());
        try {
            if (request.getId() == null) {
                String code = normalize(request.getCode()).toUpperCase();
                if (!CODE_PATTERN.matcher(code).matches() || SUPER_ADMIN.equals(code)) throw new ServerException(400, "角色编码格式不正确");
                jdbcTemplate.update("INSERT INTO sys_role (code, name, status) VALUES (?, ?, 1)", code, normalize(request.getName()));
                Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                replaceRolePermissions(id, permissionIds); return id;
            }
            String code = roleCode(request.getId());
            if (SUPER_ADMIN.equals(code)) throw new ServerException(400, "超级管理员角色不可编辑");
            jdbcTemplate.update("UPDATE sys_role SET name = ? WHERE id = ? AND deleted = b'0'", normalize(request.getName()), request.getId());
            replaceRolePermissions(request.getId(), permissionIds); revokeRoleUsers(request.getId()); return request.getId();
        } catch (DuplicateKeyException exception) { throw new ServerException(409, "角色编码已存在"); }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeRoleStatus(Long roleId, Integer status) {
        if (roleId == null || status == null || (status != 0 && status != 1)) throw new ServerException(400, "角色状态不正确");
        String code = roleCode(roleId);
        if (SUPER_ADMIN.equals(code)) throw new ServerException(400, "超级管理员角色不可停用");
        jdbcTemplate.update("UPDATE sys_role SET status = ? WHERE id = ? AND deleted = b'0'", status, roleId);
        revokeRoleUsers(roleId);
    }

    public List<PermissionRespVO> getPermissions() {
        return jdbcTemplate.query("""
                SELECT id, code, name, path_pattern, http_method, status FROM sys_permission
                 WHERE deleted = b'0' ORDER BY id
                """, (rs, index) -> {
            PermissionRespVO item = new PermissionRespVO(); item.setId(rs.getLong("id")); item.setCode(rs.getString("code"));
            item.setName(rs.getString("name")); item.setPathPattern(rs.getString("path_pattern")); item.setHttpMethod(rs.getString("http_method")); item.setStatus(rs.getInt("status")); return item;
        });
    }

    public PageResult<LoginLogRespVO> getLoginLogs(PageParam pageParam, String username, Integer success) {
        if (success != null && success != 0 && success != 1) throw new ServerException(400, "登录结果不正确");
        StringBuilder where = new StringBuilder(" WHERE 1 = 1"); List<Object> parameters = new ArrayList<>();
        if (notBlank(username)) { where.append(" AND l.username LIKE ?"); parameters.add("%" + username.trim() + "%"); }
        if (success != null) { where.append(" AND l.success = ?"); parameters.add(success); }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_login_log l" + where, Long.class, parameters.toArray());
        parameters.add(pageParam.getPageSize()); parameters.add((pageParam.getPageNo() - 1) * pageParam.getPageSize());
        List<LoginLogRespVO> rows = jdbcTemplate.query("""
                SELECT l.id, l.admin_user_id, l.username, COALESCE(u.nickname, '') nickname, l.success, l.ip, l.user_agent, l.message, l.create_time
                  FROM sys_login_log l LEFT JOIN sys_admin_user u ON u.id = l.admin_user_id
                """ + where + " ORDER BY l.id DESC LIMIT ? OFFSET ?", (rs, index) -> {
            LoginLogRespVO item = new LoginLogRespVO(); item.setId(rs.getLong("id")); item.setAdminUserId(nullableLong(rs, "admin_user_id"));
            item.setUsername(rs.getString("username")); item.setNickname(rs.getString("nickname")); item.setSuccess(rs.getInt("success")); item.setIp(rs.getString("ip"));
            item.setUserAgent(rs.getString("user_agent")); item.setMessage(rs.getString("message")); item.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time"))); return item;
        }, parameters.toArray());
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    public PageResult<OperationLogRespVO> getOperationLogs(PageParam pageParam, String username, String requestUri,
                                                            String businessRef, String operationType,
                                                            Integer highRisk, Integer success) {
        if (success != null && success != 0 && success != 1) throw new ServerException(400, "操作结果不正确");
        if (highRisk != null && highRisk != 0 && highRisk != 1) throw new ServerException(400, "高风险标记不正确");
        StringBuilder where = new StringBuilder(" WHERE 1 = 1"); List<Object> parameters = new ArrayList<>();
        if (notBlank(username)) { where.append(" AND u.username LIKE ?"); parameters.add("%" + username.trim() + "%"); }
        if (notBlank(requestUri)) { where.append(" AND l.request_uri LIKE ?"); parameters.add("%" + requestUri.trim() + "%"); }
        if (notBlank(businessRef)) { where.append(" AND l.business_ref LIKE ?"); parameters.add("%" + businessRef.trim() + "%"); }
        if (notBlank(operationType)) { where.append(" AND l.operation_type LIKE ?"); parameters.add("%" + operationType.trim() + "%"); }
        if (highRisk != null) { where.append(" AND l.high_risk = ?"); parameters.add(highRisk); }
        if (success != null) { where.append(" AND l.success = ?"); parameters.add(success); }
        String from = " FROM sys_operation_log l LEFT JOIN sys_admin_user u ON u.id = l.admin_user_id";
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + from + where, Long.class, parameters.toArray());
        parameters.add(pageParam.getPageSize()); parameters.add((pageParam.getPageNo() - 1) * pageParam.getPageSize());
        List<OperationLogRespVO> rows = jdbcTemplate.query("""
                SELECT l.id, l.admin_user_id, COALESCE(u.username, '') username, COALESCE(u.nickname, '') nickname,
                       l.admin_role_codes, l.method, l.request_uri, l.operation_type, l.high_risk,
                       l.business_ref, l.success, l.ip, l.user_agent, l.duration_ms, l.message,
                       l.before_snapshot, l.after_snapshot, l.create_time
                """ + from + where + " ORDER BY l.id DESC LIMIT ? OFFSET ?", (rs, index) -> {
            OperationLogRespVO item = new OperationLogRespVO(); item.setId(rs.getLong("id")); item.setAdminUserId(rs.getLong("admin_user_id"));
            item.setUsername(rs.getString("username")); item.setNickname(rs.getString("nickname")); item.setAdminRoleCodes(rs.getString("admin_role_codes"));
            item.setMethod(rs.getString("method")); item.setRequestUri(rs.getString("request_uri")); item.setOperationType(rs.getString("operation_type"));
            item.setHighRisk(rs.getInt("high_risk")); item.setBusinessRef(rs.getString("business_ref")); item.setSuccess(rs.getInt("success")); item.setIp(rs.getString("ip"));
            item.setUserAgent(rs.getString("user_agent")); item.setDurationMs(rs.getLong("duration_ms")); item.setMessage(rs.getString("message"));
            item.setBeforeSnapshot(rs.getString("before_snapshot")); item.setAfterSnapshot(rs.getString("after_snapshot"));
            item.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time"))); return item;
        }, parameters.toArray());
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    private void validateUserFields(AdminUserSaveReqVO request, boolean creating) {
        if (creating && !USERNAME_PATTERN.matcher(normalize(request.getUsername())).matches()) throw new ServerException(400, "账号须以字母开头，长度为 3 至 64 位");
        if (creating) validatePassword(request.getPassword());
        if (!notBlank(request.getNickname()) || normalize(request.getNickname()).length() > 64) throw new ServerException(400, "管理员姓名长度应为 1 至 64 个字符");
        String avatar = normalize(request.getAvatar());
        if (avatar.length() > 512 || (!avatar.isEmpty() && !(avatar.startsWith("https://") || avatar.startsWith("/static/")))) throw new ServerException(400, "头像地址必须是 HTTPS 或站内静态资源");
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 10 || password.length() > 128
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) throw new ServerException(400, "密码须为 10 至 128 位，且同时包含字母和数字");
    }

    private List<Long> requireActiveRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) throw new ServerException(400, "至少选择一个启用角色");
        Set<Long> ids = roleIds.stream().filter(id -> id != null && id > 0).collect(Collectors.toSet());
        if (ids.size() != roleIds.size()) throw new ServerException(400, "角色编号不正确");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role WHERE deleted = b'0' AND status = 1 AND id IN (" + placeholders(ids.size()) + ")", Integer.class, ids.toArray());
        if (count == null || count != ids.size()) throw new ServerException(400, "存在不存在或已停用的角色"); return List.copyOf(ids);
    }

    private List<Long> requireActivePermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) throw new ServerException(400, "至少选择一个启用权限");
        Set<Long> ids = permissionIds.stream().filter(id -> id != null && id > 0).collect(Collectors.toSet());
        if (ids.size() != permissionIds.size()) throw new ServerException(400, "权限编号不正确");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission WHERE deleted = b'0' AND status = 1 AND id IN (" + placeholders(ids.size()) + ")", Integer.class, ids.toArray());
        if (count == null || count != ids.size()) throw new ServerException(400, "存在不存在或已停用的权限"); return List.copyOf(ids);
    }

    private void replaceUserRoles(Long userId, Collection<Long> roleIds) {
        jdbcTemplate.update("DELETE FROM sys_admin_user_role WHERE admin_user_id = ?", userId);
        for (Long roleId : roleIds) jdbcTemplate.update("INSERT INTO sys_admin_user_role (admin_user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private void replaceRolePermissions(Long roleId, Collection<Long> permissionIds) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        for (Long permissionId : permissionIds) jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (?, ?)", roleId, permissionId);
    }

    private void ensureSuperAdminRetained(Long userId, List<Long> replacementRoleIds, Integer replacementStatus) {
        Integer currentSuper = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_admin_user_role ur JOIN sys_role r ON r.id = ur.role_id
                 WHERE ur.admin_user_id = ? AND r.code = 'SUPER_ADMIN' AND r.deleted = b'0'
                """, Integer.class, userId);
        if (currentSuper == null || currentSuper == 0) return;
        boolean remainsSuper = replacementRoleIds == null || replacementRoleIds.stream().anyMatch(this::isSuperAdminRole);
        boolean remainsEnabled = replacementStatus == null || replacementStatus == 1;
        if (remainsSuper && remainsEnabled) return;
        Integer others = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_admin_user u JOIN sys_admin_user_role ur ON ur.admin_user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id
                 WHERE u.id <> ? AND u.status = 1 AND u.deleted = b'0' AND r.code = 'SUPER_ADMIN' AND r.status = 1 AND r.deleted = b'0'
                """, Integer.class, userId);
        if (others == null || others == 0) throw new ServerException(409, "系统至少保留一个启用的超级管理员");
    }

    private boolean isSuperAdminRole(Long roleId) { return SUPER_ADMIN.equals(roleCode(roleId)); }
    private String roleCode(Long roleId) {
        String code = jdbcTemplate.query("SELECT code FROM sys_role WHERE id = ? AND deleted = b'0'", rs -> rs.next() ? rs.getString(1) : null, roleId);
        if (code == null) throw new ServerException(404, "角色不存在"); return code;
    }
    private void ensureUserExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_admin_user WHERE id = ? AND deleted = b'0'", Integer.class, userId);
        if (count == null || count != 1) throw new ServerException(404, "管理员账号不存在");
    }
    private void revokeRoleUsers(Long roleId) {
        List<Long> userIds = jdbcTemplate.queryForList("SELECT admin_user_id FROM sys_admin_user_role WHERE role_id = ?", Long.class, roleId);
        userIds.forEach(id -> tokenService.deleteAllTokens(id, 2));
    }
    private void fillUserRoles(List<AdminUserRespVO> users) {
        if (users.isEmpty()) return;
        Map<Long, AdminUserRespVO> result = users.stream().collect(Collectors.toMap(AdminUserRespVO::getId, item -> item));
        users.forEach(item -> { item.setRoleIds(new ArrayList<>()); item.setRoleCodes(new ArrayList<>()); item.setRoleNames(new ArrayList<>()); });
        jdbcTemplate.query("SELECT ur.admin_user_id, r.id, r.code, r.name FROM sys_admin_user_role ur "
                + "JOIN sys_role r ON r.id = ur.role_id AND r.deleted = b'0' "
                + "WHERE ur.admin_user_id IN (" + placeholders(users.size()) + ") ORDER BY r.id", rs -> {
            AdminUserRespVO item = result.get(rs.getLong("admin_user_id")); if (item != null) { item.getRoleIds().add(rs.getLong("id")); item.getRoleCodes().add(rs.getString("code")); item.getRoleNames().add(rs.getString("name")); }
        }, users.stream().map(AdminUserRespVO::getId).toArray());
    }
    private void fillRolePermissions(List<RoleRespVO> roles) {
        if (roles.isEmpty()) return;
        Map<Long, RoleRespVO> result = roles.stream().collect(Collectors.toMap(RoleRespVO::getId, item -> item)); roles.forEach(item -> { item.setPermissionIds(new ArrayList<>()); item.setPermissionCodes(new ArrayList<>()); });
        jdbcTemplate.query("SELECT rp.role_id, p.id, p.code FROM sys_role_permission rp "
                + "JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = b'0' "
                + "WHERE rp.role_id IN (" + placeholders(roles.size()) + ") ORDER BY p.id", rs -> {
            RoleRespVO item = result.get(rs.getLong("role_id"));
            if (item != null) { item.getPermissionIds().add(rs.getLong("id")); item.getPermissionCodes().add(rs.getString("code")); }
        }, roles.stream().map(RoleRespVO::getId).toArray());
    }
    private static String placeholders(int count) { return "?".repeat(count).replace("?", "?,").replaceAll(",$", ""); }
    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
    private static LocalDateTime toLocalDateTime(Timestamp timestamp) { return timestamp == null ? null : timestamp.toLocalDateTime(); }
    private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException { long value = rs.getLong(column); return rs.wasNull() ? null : value; }
}
