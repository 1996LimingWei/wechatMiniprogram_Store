package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家订单通知发送记录，仅用于排查漏发，不做逻辑删除。
 */
@Data
@TableName("shop_order_notify_log")
public class ShopOrderNotifyLogDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String orderSn;

    /** 通知渠道，当前仅 email */
    private String channel;

    /** 接收方（多个收件人逗号拼接） */
    private String receiver;

    /** 1=成功 0=失败 */
    private Integer success;

    /** 失败原因或响应摘要 */
    private String message;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
