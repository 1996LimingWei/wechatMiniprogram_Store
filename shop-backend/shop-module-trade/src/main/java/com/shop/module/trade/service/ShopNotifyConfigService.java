package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.ShopNotifyConfigDO;
import com.shop.module.trade.dal.mysql.ShopNotifyConfigMapper;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 商家新订单通知配置：SMTP 参数、收件人、渠道开关。
 * 配置存放在数据库单行表 shop_notify_config，不依赖 .env / yml。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopNotifyConfigService {

    public static final int CONFIG_ID = 1;
    /** 授权码对外统一掩码，回传该值表示不修改 */
    private static final String MASK = "******";
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ShopNotifyConfigMapper configMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 内部使用：读取原始配置（含明文授权码） */
    public ShopNotifyConfigDO getRawConfig() {
        ShopNotifyConfigDO config = configMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = new ShopNotifyConfigDO();
            config.setId(CONFIG_ID);
            config.setEnabled(0);
            config.setMailEnabled(1);
            config.setMailRecipients("");
            config.setSmtpHost("");
            config.setSmtpPort(465);
            config.setSmtpUsername("");
            config.setSmtpPassword("");
            config.setMailFrom("");
            config.setSslEnabled(1);
        }
        return config;
    }

    /** 后台读取：授权码脱敏 */
    public Map<String, Object> getConfig(Long adminId) {
        requireSuperAdmin(adminId);
        ShopNotifyConfigDO config = getRawConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", config.getEnabled());
        result.put("mailEnabled", config.getMailEnabled());
        result.put("mailRecipients", config.getMailRecipients());
        result.put("smtpHost", config.getSmtpHost());
        result.put("smtpPort", config.getSmtpPort());
        result.put("smtpUsername", config.getSmtpUsername());
        result.put("smtpPassword", hasText(config.getSmtpPassword()) ? MASK : "");
        result.put("smtpPasswordConfigured", hasText(config.getSmtpPassword()));
        result.put("mailFrom", config.getMailFrom());
        result.put("sslEnabled", config.getSslEnabled());
        result.put("updatedAt", config.getUpdateTime());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveConfig(Long adminId, Map<String, Object> body) {
        requireSuperAdmin(adminId);
        ShopNotifyConfigDO existing = getRawConfig();

        int enabled = TradeRequestUtils.getInt(body, "enabled", 0);
        int mailEnabled = TradeRequestUtils.getInt(body, "mailEnabled", 1);
        if (enabled != 0 && enabled != 1) throw new ServerException(400, "通知总开关取值不正确");
        if (mailEnabled != 0 && mailEnabled != 1) throw new ServerException(400, "邮件渠道开关取值不正确");

        List<String> recipients = parseRecipients(TradeRequestUtils.getString(body, "mailRecipients", ""));
        if (recipients.size() > 10) throw new ServerException(400, "收件邮箱最多 10 个");
        for (String recipient : recipients) {
            if (!EMAIL.matcher(recipient).matches()) {
                throw new ServerException(400, "收件邮箱格式不正确：" + recipient);
            }
        }

        String host = TradeRequestUtils.getString(body, "smtpHost", "").trim();
        int port = TradeRequestUtils.getInt(body, "smtpPort", 465);
        String username = TradeRequestUtils.getString(body, "smtpUsername", "").trim();
        String rawPassword = TradeRequestUtils.getString(body, "smtpPassword", "");
        String mailFrom = TradeRequestUtils.getString(body, "mailFrom", "").trim();
        int sslEnabled = TradeRequestUtils.getInt(body, "sslEnabled", 1);
        if (sslEnabled != 0 && sslEnabled != 1) throw new ServerException(400, "SSL 开关取值不正确");

        if (enabled == 1) {
            if (!hasText(host)) throw new ServerException(400, "请填写 SMTP 服务器地址");
            if (port < 1 || port > 65535) throw new ServerException(400, "SMTP 端口应在 1 至 65535 之间");
            if (!hasText(username)) throw new ServerException(400, "请填写 SMTP 登录账号");
            if (!EMAIL.matcher(username).matches()) throw new ServerException(400, "SMTP 登录账号需为邮箱地址");
            if (recipients.isEmpty()) throw new ServerException(400, "请至少填写一个收件邮箱");
            boolean passwordKept = MASK.equals(rawPassword) && hasText(existing.getSmtpPassword());
            if (!passwordKept && !hasText(rawPassword)) throw new ServerException(400, "请填写 SMTP 授权码");
        }
        if (hasText(host) && host.contains(" ")) throw new ServerException(400, "SMTP 服务器地址不能包含空格");
        if (hasText(mailFrom) && !EMAIL.matcher(mailFrom).matches()) {
            throw new ServerException(400, "发件人地址格式不正确");
        }

        // 掩码或留空都视为沿用旧授权码，避免前端回显导致密码被清空
        String password = (MASK.equals(rawPassword) || rawPassword.isBlank())
                ? existing.getSmtpPassword() : rawPassword.trim();
        String from = hasText(mailFrom) ? mailFrom : (hasText(existing.getMailFrom())
                ? existing.getMailFrom() : username);

        ShopNotifyConfigDO update = new ShopNotifyConfigDO();
        update.setId(CONFIG_ID);
        update.setEnabled(enabled);
        update.setMailEnabled(mailEnabled);
        update.setMailRecipients(String.join(",", recipients));
        update.setSmtpHost(host);
        update.setSmtpPort(port);
        update.setSmtpUsername(username);
        update.setSmtpPassword(password);
        update.setMailFrom(from);
        update.setSslEnabled(sslEnabled);
        configMapper.updateById(update);
        log.info("商家订单通知配置已更新 adminId={} enabled={} mailEnabled={} recipients={}",
                adminId, enabled, mailEnabled, recipients.size());
        return getConfig(adminId);
    }

    /** 是否可发送：总开关与邮件渠道都开启，且有收件人 */
    public boolean mailReady(ShopNotifyConfigDO config) {
        return config != null
                && Integer.valueOf(1).equals(config.getEnabled())
                && Integer.valueOf(1).equals(config.getMailEnabled())
                && !parseRecipients(config.getMailRecipients()).isEmpty();
    }

    /** 仅超管可维护通知配置 */
    public void requireSuperAdmin(Long adminId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_admin_user u
                JOIN sys_admin_user_role ur ON ur.admin_user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id AND r.deleted = b'0' AND r.status = 1
                WHERE u.id = ? AND u.status = 1 AND u.deleted = b'0' AND r.code = 'SUPER_ADMIN'
                """, Integer.class, adminId);
        if (count == null || count < 1) {
            throw new ServerException(403, "仅超级管理员可维护订单通知设置");
        }
    }

    public static List<String> parseRecipients(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split("[,，;；\\s]+")) {
            String value = part.trim();
            if (!value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
