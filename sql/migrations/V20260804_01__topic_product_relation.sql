-- 专题与商品多对多关联表
CREATE TABLE IF NOT EXISTS `content_topic_product` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `topic_id` bigint NOT NULL COMMENT '专题ID',
    `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越大越前)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_spu` (`topic_id`, `spu_id`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB COMMENT='专题关联商品表';
