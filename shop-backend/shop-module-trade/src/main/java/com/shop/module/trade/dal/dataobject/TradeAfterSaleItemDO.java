package com.shop.module.trade.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_after_sale_item")
public class TradeAfterSaleItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long afterSaleId;
    private Long orderItemId;
    private Long spuId;
    private Long skuId;
    private String goodsName;
    private String specName;
    private Integer price;
    private Integer applyCount;
    private Integer refundAmount;
}
