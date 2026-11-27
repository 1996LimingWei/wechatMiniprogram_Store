package com.shop.module.trade.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.dal.dataobject.ShopOrderNotifyLogDO;
import com.shop.module.trade.dal.mysql.ShopOrderNotifyLogMapper;
import com.shop.module.trade.service.OrderNotifyMailService;
import com.shop.module.trade.service.ShopNotifyConfigService;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.util.TradeSecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 — 商家新订单通知设置（当前支持邮件 SMTP）。
 */
@RestController
@RequestMapping("/admin-api/trade/notify-config")
@RequiredArgsConstructor
public class AdminShopNotifyController {

    private final ShopNotifyConfigService shopNotifyConfigService;
    private final OrderNotifyMailService orderNotifyMailService;
    private final ShopOrderNotifyLogMapper notifyLogMapper;

    @GetMapping
    public CommonResult<Map<String, Object>> getConfig() {
        return CommonResult.success(shopNotifyConfigService.getConfig(TradeSecurityUtils.getRequiredUserId()));
    }

    @PostMapping("/save")
    public CommonResult<Map<String, Object>> save(@RequestBody(required = false) String rawBody,
                                                 @RequestParam Map<String, Object> params) {
        Map<String, Object> request = TradeRequestUtils.parse(rawBody, params);
        return CommonResult.success(shopNotifyConfigService.saveConfig(
                TradeSecurityUtils.getRequiredUserId(), request));
    }

    /** 使用已保存的配置发送一封测试邮件 */
    @PostMapping("/test")
    public CommonResult<Boolean> test() {
        Long adminId = TradeSecurityUtils.getRequiredUserId();
        shopNotifyConfigService.requireSuperAdmin(adminId);
        orderNotifyMailService.sendTestMail(shopNotifyConfigService.getRawConfig());
        return CommonResult.success(true);
    }

    @GetMapping("/logs")
    public CommonResult<PageResult<Map<String, Object>>> logs(PageParam pageParam) {
        shopNotifyConfigService.requireSuperAdmin(TradeSecurityUtils.getRequiredUserId());
        PageResult<ShopOrderNotifyLogDO> page = notifyLogMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ShopOrderNotifyLogDO>().orderByDesc(ShopOrderNotifyLogDO::getId));
        List<Map<String, Object>> list = page.getList().stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("orderId", item.getOrderId());
            row.put("orderSn", item.getOrderSn());
            row.put("channel", item.getChannel());
            row.put("receiver", item.getReceiver());
            row.put("success", item.getSuccess());
            row.put("message", item.getMessage());
            row.put("createTime", item.getCreateTime());
            return row;
        }).toList();
        return CommonResult.success(new PageResult<>(list, page.getTotal()));
    }
}
