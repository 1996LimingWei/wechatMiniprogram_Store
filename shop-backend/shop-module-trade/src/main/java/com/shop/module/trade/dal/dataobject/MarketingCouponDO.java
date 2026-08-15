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
@TableName("marketing_coupon")
public class MarketingCouponDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会员用户ID */
    private Long userId;

    /** 优惠券模板ID */
    private Long templateId;

    /** 0=未使用 1=已使用 2=已过期 */
    private Integer status;

    /** 使用的订单ID */
    private Long orderId;

    /** 使用时间 */
    private LocalDateTime usedTime;

    /** 过期时间 */
    private LocalDateTime expireTime;
}
