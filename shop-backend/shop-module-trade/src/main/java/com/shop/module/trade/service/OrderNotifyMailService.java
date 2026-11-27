package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.trade.dal.dataobject.ShopNotifyConfigDO;
import com.shop.module.trade.dal.dataobject.ShopOrderNotifyLogDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.mysql.ShopOrderNotifyLogMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * 客户支付成功后即时提醒商家备货发货。
 * SMTP 参数全部来自 shop_notify_config，支持后台随时调整，无需重启或改配置文件。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotifyMailService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ShopNotifyConfigService shopNotifyConfigService;
    private final ShopOrderNotifyLogMapper notifyLogMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;

    /** 支付成功后异步推送，失败只记日志，不影响支付主流程 */
    @Async
    public void notifyOrderPaid(Long orderId) {
        ShopNotifyConfigDO config = shopNotifyConfigService.getRawConfig();
        if (!shopNotifyConfigService.mailReady(config)) {
            log.debug("商家新订单通知未开启，跳过发送 orderId={}", orderId);
            return;
        }
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在，跳过新订单通知 orderId={}", orderId);
            return;
        }
        List<String> recipients = ShopNotifyConfigService.parseRecipients(config.getMailRecipients());
        try {
            sendMail(config, recipients, order);
            writeLog(order, recipients, true, "发送成功");
            log.info("新订单邮件通知已发送 orderSn={} recipients={}", order.getOrderSn(), recipients.size());
        } catch (Exception exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            writeLog(order, recipients, false, truncate(message, 500));
            log.warn("新订单邮件通知发送失败 orderSn={} reason={}", order.getOrderSn(), message);
        }
    }

    /** 后台「发送测试邮件」，同步返回结果便于管理员立即看到报错 */
    public void sendTestMail(ShopNotifyConfigDO config) {
        List<String> recipients = ShopNotifyConfigService.parseRecipients(config.getMailRecipients());
        if (recipients.isEmpty()) {
            throw new com.shop.common.exception.ServerException(400, "请先填写收件邮箱");
        }
        try {
            sendRaw(config, recipients, "【店铺】通知测试邮件",
                    "<p>这是一封来自店铺后台的测试邮件。</p>"
                            + "<p>收到此邮件说明 SMTP 配置可用，客户支付成功后将自动推送新订单提醒。</p>");
        } catch (Exception exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            throw new com.shop.common.exception.ServerException(500, "邮件发送失败：" + truncate(message, 300));
        }
    }

    private void sendMail(ShopNotifyConfigDO config, List<String> recipients, TradeOrderDO order) throws Exception {
        String subject = "【新订单】" + order.getOrderSn() + " 已支付，请及时备货发货";
        String html = buildHtml(order);
        sendRaw(config, recipients, subject, html);
    }

    private void sendRaw(ShopNotifyConfigDO config, List<String> recipients,
                         String subject, String html) throws Exception {
        JavaMailSenderImpl sender = buildSender(config);
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        String from = config.getMailFrom() == null || config.getMailFrom().isBlank()
                ? config.getSmtpUsername() : config.getMailFrom();
        helper.setFrom(from);
        helper.setTo(recipients.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(html, true);
        sender.send(message);
    }

    private JavaMailSenderImpl buildSender(ShopNotifyConfigDO config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort() == null ? 465 : config.getSmtpPort());
        sender.setUsername(config.getSmtpUsername());
        sender.setPassword(config.getSmtpPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", "8000");
        properties.put("mail.smtp.timeout", "8000");
        properties.put("mail.smtp.writetimeout", "8000");
        if (Integer.valueOf(1).equals(config.getSslEnabled())) {
            // 465 端口：SSL 直连
            properties.put("mail.smtp.ssl.enable", "true");
        } else {
            // 587/25 端口：STARTTLS 升级
            properties.put("mail.smtp.starttls.enable", "true");
        }
        return sender;
    }

    private String buildHtml(TradeOrderDO order) {
        List<TradeOrderItemDO> items = tradeOrderItemMapper.selectList(
                new LambdaQueryWrapper<TradeOrderItemDO>()
                        .eq(TradeOrderItemDO::getOrderId, order.getId())
                        .orderByAsc(TradeOrderItemDO::getId));
        StringBuilder goods = new StringBuilder();
        for (TradeOrderItemDO item : items) {
            String spec = item.getSpecName() == null || item.getSpecName().isBlank()
                    ? "" : "（" + item.getSpecName() + "）";
            goods.append("<tr>")
                    .append("<td style=\"padding:6px 8px;border-bottom:1px solid #eee;\">")
                    .append(escape(item.getGoodsName())).append(escape(spec)).append("</td>")
                    .append("<td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:center;\">")
                    .append(item.getCount()).append("</td>")
                    .append("<td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:right;\">¥")
                    .append(TradeMoneyUtils.formatYuan(item.getTotalPrice())).append("</td>")
                    .append("</tr>");
        }
        if (goods.isEmpty()) {
            goods.append("<tr><td colspan=\"3\" style=\"padding:6px 8px;\">商品明细缺失，请到后台订单详情查看</td></tr>");
        }
        String payTime = order.getPayTime() == null
                ? LocalDateTime.now().format(TIME_FORMATTER) : order.getPayTime().format(TIME_FORMATTER);
        String address = (order.getFullRegion() == null ? "" : order.getFullRegion())
                + (order.getAddress() == null ? "" : order.getAddress());

        return "<div style=\"font-family:-apple-system,'PingFang SC',sans-serif;color:#1f2d3d;\">"
                + "<h2 style=\"margin:0 0 8px;color:#c0392b;\">新订单已支付，请尽快备货发货</h2>"
                + "<p style=\"margin:0 0 16px;color:#606266;\">订单号：" + escape(order.getOrderSn())
                + "　支付时间：" + payTime + "</p>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;\">"
                + "<thead><tr style=\"background:#fafafa;\">"
                + "<th style=\"padding:8px;text-align:left;\">商品</th>"
                + "<th style=\"padding:8px;text-align:center;\">数量</th>"
                + "<th style=\"padding:8px;text-align:right;\">小计</th>"
                + "</tr></thead><tbody>" + goods + "</tbody></table>"
                + "<p style=\"margin:16px 0 4px;\">商品金额：¥" + TradeMoneyUtils.formatYuan(order.getGoodsPrice())
                + "　运费：¥" + TradeMoneyUtils.formatYuan(order.getFreightPrice())
                + "　优惠：¥" + TradeMoneyUtils.formatYuan(order.getCouponPrice()) + "</p>"
                + "<p style=\"margin:0 0 4px;font-size:16px;\"><strong>实付金额：¥"
                + TradeMoneyUtils.formatYuan(order.getActualPrice()) + "</strong></p>"
                + "<p style=\"margin:12px 0 4px;\">收货人：" + escape(order.getConsignee())
                + "　电话：" + escape(order.getMobile()) + "</p>"
                + "<p style=\"margin:0;\">收货地址：" + escape(address) + "</p>"
                + "<p style=\"margin:16px 0 0;color:#909399;font-size:12px;\">"
                + "请登录管理后台 → 订单管理，更新履约状态（备货中 / 待揽收 / 已发货）。</p>"
                + "</div>";
    }

    private void writeLog(TradeOrderDO order, List<String> recipients, boolean success, String message) {
        ShopOrderNotifyLogDO logDO = new ShopOrderNotifyLogDO();
        logDO.setOrderId(order.getId());
        logDO.setOrderSn(order.getOrderSn());
        logDO.setChannel("email");
        logDO.setReceiver(truncate(String.join(",", recipients), 1000));
        logDO.setSuccess(success ? 1 : 0);
        logDO.setMessage(truncate(message, 500));
        try {
            notifyLogMapper.insert(logDO);
        } catch (Exception exception) {
            // 日志落库失败不影响通知语义
            log.warn("订单通知日志写入失败 orderSn={} reason={}", order.getOrderSn(), exception.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
