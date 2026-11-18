package com.shop.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 运行模式同时约束入口、定时任务和渠道配置，未知值一律拒绝启动。 */
@Component
public class DeliveryMode {
    private final boolean commerce;

    public DeliveryMode(@Value("${app.delivery-mode:commerce}") String value) {
        if (!"catalog".equals(value) && !"commerce".equals(value)) {
            throw new IllegalArgumentException("APP_DELIVERY_MODE 必须明确配置为 catalog 或 commerce");
        }
        commerce = "commerce".equals(value);
    }

    public boolean isCommerce() { return commerce; }
}
