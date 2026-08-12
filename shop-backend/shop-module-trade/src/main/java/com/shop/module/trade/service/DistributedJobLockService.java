package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistributedJobLockService {

    private final JdbcTemplate jdbcTemplate;
    private final String owner = UUID.randomUUID().toString();

    public boolean tryLock(String lockName, Duration duration) {
        long seconds = Math.max(5, duration.toSeconds());
        int affected = jdbcTemplate.update("""
                INSERT INTO sys_job_lock(lock_name, lock_owner, locked_until)
                VALUES (?, ?, TIMESTAMPADD(SECOND, ?, NOW()))
                ON DUPLICATE KEY UPDATE
                    lock_owner = IF(locked_until <= NOW(), VALUES(lock_owner), lock_owner),
                    locked_until = IF(locked_until <= NOW(), VALUES(locked_until), locked_until)
                """, lockName, owner, seconds);
        if (affected == 1 || affected == 2) {
            String currentOwner = jdbcTemplate.queryForObject(
                    "SELECT lock_owner FROM sys_job_lock WHERE lock_name = ?", String.class, lockName);
            return owner.equals(currentOwner);
        }
        return false;
    }

    public void release(String lockName) {
        jdbcTemplate.update("""
                UPDATE sys_job_lock SET locked_until = NOW()
                 WHERE lock_name = ? AND lock_owner = ?
                """, lockName, owner);
    }
}
