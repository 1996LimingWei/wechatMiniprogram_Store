package com.shop.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DatabaseMigrationValidatorTest {
    @Test void missingNewShopConfigurationMigrationRejectsStartup() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        stubManifest(jdbc);
        when(jdbc.queryForList(anyString(), eq(String.class), eq("20261104_02"))).thenReturn(java.util.List.of());
        var failure = assertThrows(IllegalStateException.class,
                () -> new DatabaseMigrationValidator(jdbc).run(null));
        assertTrue(failure.getMessage().contains("20261104_02"));
    }

    @Test void allRequiredReleaseMigrationsAllowStartup() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        stubManifest(jdbc);
        assertDoesNotThrow(() -> new DatabaseMigrationValidator(jdbc).run(null));
    }
    private void stubManifest(JdbcTemplate jdbc) {
        java.util.Properties required = new java.util.Properties();
        try (var input = getClass().getResourceAsStream("/delivery-migrations.properties")) {
            required.load(input);
        } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        when(jdbc.queryForList(anyString(), eq(String.class), anyString())).thenAnswer(call ->
                java.util.List.of(required.getProperty(call.getArgument(2))));
    }
}
