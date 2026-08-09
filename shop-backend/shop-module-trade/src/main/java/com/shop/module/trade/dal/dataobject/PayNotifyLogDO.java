package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_notify_log")
public class PayNotifyLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationId;
    private Long payOrderId;
    private String paySn;
    private String channelTradeNo;
    private String eventType;
    private Integer status;
    private String message;
    private String rawBody;
}
