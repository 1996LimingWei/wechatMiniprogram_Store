package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家新订单通知配置，整表固定单行（id=1），不带逻辑删除。
 */
@Data
@TableName("shop_notify_config")
public class ShopNotifyConfigDO implements Serializable {

    @TableId(type = IdType.INPUT)
    private Integer id;

    /** 总开关 1=开启 0=关闭 */
    private Integer enabled;

    /** 邮件渠道 1=开启 0=关闭 */
    private Integer mailEnabled;

    /** 收件人，多个用英文逗号分隔 */
    private String mailRecipients;

    private String smtpHost;

    private Integer smtpPort;

    private String smtpUsername;

    /** SMTP 授权码，读取接口统一掩码返回 */
    private String smtpPassword;

    /** 发件人地址，留空取 SMTP 账号 */
    private String mailFrom;

    /** 1=SSL/TLS 直连 0=明文 */
    private Integer sslEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
