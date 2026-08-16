package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_order")
public class PayOrderDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String paySn;
    private Long orderId;
    private Long userId;
    private Integer amount;
    private Integer refundedAmount;
    private String channel;
    private String channelTradeNo;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime lastQueryTime;
    private String wechatTradeState;
    private Integer wechatAmount;
    private String syncMessage;
}
