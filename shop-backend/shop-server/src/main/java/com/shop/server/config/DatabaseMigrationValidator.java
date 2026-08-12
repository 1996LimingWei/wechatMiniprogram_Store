package com.shop.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class DatabaseMigrationValidator implements ApplicationRunner {

    private static final String REQUIRED_VERSION = "20260811_14";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer historyTable = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'schema_migration_history'
                """, Integer.class);
        if (historyTable == null || historyTable != 1) {
            throw new IllegalStateException("数据库迁移历史不存在，禁止启动生产服务");
        }
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_migration_history WHERE version = ?",
                Integer.class, REQUIRED_VERSION);
        if (applied == null || applied != 1) {
            throw new IllegalStateException("数据库未执行必需迁移 " + REQUIRED_VERSION + "，禁止启动生产服务");
        }
    }
}
