package com.shop.module.system.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.common.exception.ServerException;
import com.shop.framework.security.SecurityUtils;
import com.shop.module.system.service.SystemAdministrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


/** 仅保存公开展示资料，微信账号名称及支付密钥不属于该配置。 */
@RestController
@RequiredArgsConstructor
public class ShopDisplayController {
    private final JdbcTemplate jdbc;
    private final SystemAdministrationService administration;

    @GetMapping("/app-api/index/shop-config")
    public CommonResult<DisplayConfig> publicConfig() { return CommonResult.success(read()); }

    @GetMapping("/admin-api/content/shop-config")
    public CommonResult<DisplayConfig> previewConfig() { return CommonResult.success(read()); }

    @GetMapping("/admin-api/system/shop")
    public CommonResult<DisplayConfig> adminConfig() {
        administration.requireSuperAdmin(SecurityUtils.getRequiredAdminId());
        return CommonResult.success(read());
    }

    @PostMapping("/admin-api/system/shop")
    @Transactional
    public CommonResult<Boolean> save(@RequestBody DisplayConfig body) {
        administration.requireSuperAdmin(SecurityUtils.getRequiredAdminId());
        String name = text(body.displayName(), "displayName", 30);
        if (name.isBlank()) throw new ServerException(400, "请填写店铺展示名称");
        String logo = text(body.logoUrl(), "logoUrl", 1024);
        if (!logo.isEmpty() && !logo.matches("https://[^\\s<>\"']+"))
            throw new ServerException(400, "Logo 请使用正式 HTTPS 图片地址");
        if (!logo.isEmpty()) {
            Integer registered = jdbc.queryForObject("SELECT COUNT(*) FROM material_asset WHERE url=? AND deleted=b'0'", Integer.class, logo);
            if (registered == null || registered == 0) throw new ServerException(400, "请从素材库选择店铺 Logo");
        }
        int updated = jdbc.update("UPDATE shop_display_config SET display_name=?,logo_url=?,introduction=?,customer_service=? WHERE id=1",
                name, logo, text(body.introduction(), "introduction", 200), text(body.customerService(), "customerService", 100));
        if (updated != 1) throw new ServerException(409, "店铺配置不存在，请联系管理员检查数据库迁移");
        return CommonResult.success(true);
    }

    public record DisplayConfig(String displayName, String logoUrl, String introduction, String customerService) {}

    private DisplayConfig read() {
        return jdbc.queryForObject("SELECT display_name,logo_url,introduction,customer_service FROM shop_display_config WHERE id=1",
                (rs, index) -> new DisplayConfig(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    private String text(String value, String key, int limit) {
        if (value == null) value = "";
        value = value.trim();
        if (value.length() > limit || value.contains("<") || value.contains(">"))
            throw new ServerException(400, "字段长度或格式不正确：" + key);
        return value;
    }
}
