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
@TableName("trade_after_sale")
public class TradeAfterSaleDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private String afterSaleSn;
    private Integer type;
    private Integer status;
    private Integer refundAmount;
    private String reason;
    private String applyRemark;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
}
