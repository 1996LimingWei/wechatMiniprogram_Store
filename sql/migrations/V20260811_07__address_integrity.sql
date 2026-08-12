-- 收货地址完整性：每个存在地址的用户始终且只能有一个默认地址。
UPDATE member_address a
JOIN (
    SELECT user_id, MAX(id) AS keep_id
    FROM member_address
    WHERE deleted = 0 AND is_default = 1
    GROUP BY user_id
) d ON d.user_id = a.user_id
SET a.is_default = 0
WHERE a.deleted = 0 AND a.is_default = 1 AND a.id <> d.keep_id;

UPDATE member_address a
JOIN (
    SELECT user_id, MAX(id) AS keep_id
    FROM member_address
    WHERE deleted = 0
    GROUP BY user_id
    HAVING SUM(is_default = 1) = 0
) d ON d.keep_id = a.id
SET a.is_default = 1;

ALTER TABLE member_address
    ADD COLUMN default_user_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN is_default = 1 AND deleted = 0 THEN user_id ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY uk_member_address_default_user (default_user_id),
    ADD KEY idx_member_address_user_update (user_id, deleted, update_time);
