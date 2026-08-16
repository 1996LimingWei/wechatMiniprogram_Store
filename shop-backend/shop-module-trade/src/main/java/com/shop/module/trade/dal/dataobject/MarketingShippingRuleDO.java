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
@TableName("marketing_shipping_rule")
public class MarketingShippingRuleDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String name;

    /** 包邮门槛(分) */
    private Integer freeThreshold;

    /** 基础运费(分) */
    private Integer baseFee;

    /** 1=启用 0=禁用 */
    private Integer status;

    /** 生效时间，空表示立即生效 */
    private LocalDateTime startTime;

    /** 停用时间，空表示长期有效 */
    private LocalDateTime endTime;
}
