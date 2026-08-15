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
@TableName("marketing_promotion_rule")
public class MarketingPromotionRuleDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动名称 */
    private String name;

    /** 1=全店满减 */
    private Integer type;

    /** 满减门槛(分) */
    private Integer thresholdAmount;

    /** 优惠金额(分) */
    private Integer discountAmount;

    /** 1=启用 0=禁用 */
    private Integer status;

    /** 排序优先级 */
    private Integer priority;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;
}
