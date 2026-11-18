package com.shop.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({"prod", "staging"})
@RequiredArgsConstructor
public class DatabaseMigrationValidator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer historyTable = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'schema_migration_history'
                """, Integer.class);
        if (historyTable == null || historyTable != 1) {
            throw new IllegalStateException("数据库迁移历史不存在，禁止启动生产/预发布服务");
        }
        java.util.Properties required = new java.util.Properties();
        try (var input = getClass().getResourceAsStream("/delivery-migrations.properties")) {
            if (input == null) throw new IllegalStateException("发布迁移清单缺失，禁止启动");
            required.load(input);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("无法读取发布迁移清单", exception);
        }
        if (required.isEmpty()) throw new IllegalStateException("发布迁移清单为空，禁止启动");
        for (String version : new java.util.TreeSet<>(required.stringPropertyNames())) {
            java.util.List<String> checksums = jdbcTemplate.queryForList(
                    "SELECT checksum FROM schema_migration_history WHERE version = ?", String.class, version);
            if (checksums.size() != 1 || !required.getProperty(version).equals(checksums.get(0))) {
                throw new IllegalStateException("数据库迁移未执行或校验和不符：" + version + "，禁止启动生产/预发布服务");
            }
        }
    }
}
