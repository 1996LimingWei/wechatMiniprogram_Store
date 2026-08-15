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
@TableName("marketing_coupon_template")
public class MarketingCouponTemplateDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券名称 */
    private String name;

    /** 1=满减券 2=新人券 */
    private Integer type;

    /** 满减门槛(分)，0=无门槛 */
    private Integer thresholdAmount;

    /** 优惠金额(分) */
    private Integer discountAmount;

    /** 发行总量，0=不限量 */
    private Integer totalCount;

    /** 已领取数量 */
    private Integer claimedCount;

    /** 每人限领 */
    private Integer perUserLimit;

    /** 1=固定日期 2=领取后N天 */
    private Integer validityType;

    /** 有效期开始(固定日期) */
    private LocalDateTime validStartTime;

    /** 有效期结束(固定日期) */
    private LocalDateTime validEndTime;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 1=启用 0=禁用 */
    private Integer status;
}
