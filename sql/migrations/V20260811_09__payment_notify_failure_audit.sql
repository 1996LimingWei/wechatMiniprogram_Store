CREATE TABLE pay_notify_failure_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id VARCHAR(64) NOT NULL DEFAULT '',
    pay_sn VARCHAR(32) NOT NULL DEFAULT '',
    wechatpay_serial VARCHAR(128) NOT NULL DEFAULT '',
    request_timestamp VARCHAR(32) NOT NULL DEFAULT '',
    body_sha256 CHAR(64) NOT NULL,
    error_message VARCHAR(255) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pay_notify_failure_time (create_time),
    KEY idx_pay_notify_failure_notification (notification_id),
    KEY idx_pay_notify_failure_pay_sn (pay_sn)
) ENGINE=InnoDB COMMENT='支付通知失败审计';
