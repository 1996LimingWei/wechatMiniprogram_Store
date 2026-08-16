SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `member_feedback` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL COMMENT '提交用户ID',
    `type` tinyint NOT NULL COMMENT '反馈类型 1=商品 2=物流 3=客服 4=活动 5=功能 6=建议 7=其他',
    `content` varchar(500) NOT NULL COMMENT '反馈内容',
    `mobile` varchar(20) NOT NULL DEFAULT '' COMMENT '联系手机号',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0=待处理 1=处理中 2=已完成',
    `handler_admin_id` bigint DEFAULT NULL COMMENT '处理管理员ID',
    `handle_remark` varchar(500) NOT NULL DEFAULT '' COMMENT '处理备注',
    `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_status_time` (`status`, `create_time`, `id`),
    KEY `idx_feedback_user_time` (`user_id`, `create_time`, `id`),
    CONSTRAINT `chk_member_feedback_type` CHECK (`type` BETWEEN 1 AND 7),
    CONSTRAINT `chk_member_feedback_status` CHECK (`status` BETWEEN 0 AND 2)
) ENGINE=InnoDB COMMENT='用户意见反馈表';
