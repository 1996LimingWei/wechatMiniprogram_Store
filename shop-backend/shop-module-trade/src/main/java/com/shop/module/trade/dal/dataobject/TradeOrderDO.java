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
@TableName("trade_order")
public class TradeOrderDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSn;
    private String requestId;
    private Long userId;
    private Integer status;
    /** 履约状态 0=待备货 1=备货中 2=待揽收 3=已发货 4=已签收 5=物流异常 */
    private Integer fulfillmentStatus;
    private LocalDateTime fulfillmentUpdateTime;
    private Integer payStatus;
    private Integer goodsPrice;
    private Integer freightPrice;
    private Integer couponPrice;
    private Integer orderPrice;
    private Integer actualPrice;
    private Integer refundedAmount;
    private Long addressId;
    private String consignee;
    private String mobile;
    private String fullRegion;
    private String address;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private LocalDateTime closeTime;
    private String closeReason;
    private LocalDateTime finishTime;
    private String adminRemark;
    private Long couponId;
    private String discountSource;
}
