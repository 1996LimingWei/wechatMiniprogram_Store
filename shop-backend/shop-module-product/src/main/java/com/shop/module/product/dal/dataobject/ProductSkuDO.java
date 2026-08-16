package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_sku")
public class ProductSkuDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    private String skuCode;
    private String properties;
    private Integer price;
    private Integer marketPrice;
    private Integer stock;
    private String picUrl;
    private Double weight;
    private Double volume;
}
