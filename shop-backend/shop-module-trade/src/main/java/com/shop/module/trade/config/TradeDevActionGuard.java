package com.shop.module.trade.config;

import com.shop.common.exception.ServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 交易开发动作保护器。
 *
 * <p>模拟支付、模拟发货和模拟退款只能在显式开启的开发环境使用，
 * 不能依赖前端隐藏按钮作为安全边界。</p>
 */
@Component
public class TradeDevActionGuard {

    private final boolean enabled;

    public TradeDevActionGuard(@Value("${trade.dev-actions-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void checkEnabled() {
        if (!enabled) {
            throw new ServerException(404, "接口不存在");
        }
    }
}
