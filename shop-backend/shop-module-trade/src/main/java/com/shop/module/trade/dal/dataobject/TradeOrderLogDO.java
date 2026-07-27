package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order_log")
public class TradeOrderLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private String operatorType;
    private Long operatorId;
    private String action;
    private Integer fromStatus;
    private Integer toStatus;
    private Integer fromPayStatus;
    private Integer toPayStatus;
    private String remark;
}
