SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_observability_alert` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    `alert_type` varchar(64) NOT NULL COMMENT '告警类型',
    `level` varchar(16) NOT NULL DEFAULT 'WARN' COMMENT '告警级别',
    `title` varchar(128) NOT NULL DEFAULT '' COMMENT '告警标题',
    `message` varchar(512) NOT NULL DEFAULT '' COMMENT '告警内容',
    `business_ref` varchar(128) NOT NULL DEFAULT '' COMMENT '业务关联编号',
    `current_value` int NOT NULL DEFAULT 0 COMMENT '当前值',
    `threshold_value` int NOT NULL DEFAULT 0 COMMENT '阈值',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1已恢复',
    `first_trigger_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次触发时间',
    `last_trigger_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近触发时间',
    `resolve_time` datetime NULL COMMENT '恢复时间',
    `trigger_count` int NOT NULL DEFAULT 1 COMMENT '触发次数',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_alert_status_time` (`status`, `last_trigger_time`, `id`),
    KEY `idx_alert_type_ref` (`alert_type`, `business_ref`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统可观测告警事件';

CREATE TABLE IF NOT EXISTS `sys_job_execution_metric` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务指标ID',
    `job_name` varchar(64) NOT NULL COMMENT '任务名称',
    `last_status` varchar(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT '最近状态',
    `last_message` varchar(255) NOT NULL DEFAULT '' COMMENT '最近执行说明',
    `processed_count` int NOT NULL DEFAULT 0 COMMENT '最近处理数量',
    `success_count` int NOT NULL DEFAULT 0 COMMENT '成功次数',
    `failure_count` int NOT NULL DEFAULT 0 COMMENT '失败次数',
    `consecutive_failures` int NOT NULL DEFAULT 0 COMMENT '连续失败次数',
    `last_run_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近执行时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_name` (`job_name`),
    KEY `idx_job_status` (`last_status`, `consecutive_failures`, `last_run_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务执行指标';

INSERT INTO `sys_permission` (`id`, `code`, `name`, `path_pattern`, `http_method`, `status`)
VALUES
(56, 'trade:observability-read', '查看运行监控', '/admin-api/trade/observability/**', 'GET', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path_pattern` = VALUES(`path_pattern`),
                        `http_method` = VALUES(`http_method`), `status` = VALUES(`status`), `deleted` = b'0';

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
 WHERE r.code IN ('SUPER_ADMIN', 'FINANCE', 'ORDER_CUSTOMER_SERVICE', 'READONLY')
   AND p.code = 'trade:observability-read'
   AND p.deleted = b'0';
