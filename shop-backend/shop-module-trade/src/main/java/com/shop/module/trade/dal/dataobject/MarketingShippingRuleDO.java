package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
