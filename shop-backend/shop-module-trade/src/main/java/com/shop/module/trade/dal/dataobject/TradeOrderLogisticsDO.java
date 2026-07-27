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
@TableName("trade_order_logistics")
public class TradeOrderLogisticsDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String logisticsCompany;
    private String logisticsNo;
    private LocalDateTime deliveryTime;
}
