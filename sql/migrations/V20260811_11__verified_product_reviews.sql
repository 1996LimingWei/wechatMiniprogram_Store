ALTER TABLE product_comment
    ADD COLUMN order_id BIGINT NULL AFTER user_id,
    ADD UNIQUE KEY uk_product_comment_order_spu_user (order_id, spu_id, user_id),
    ADD KEY idx_product_comment_order (order_id);
