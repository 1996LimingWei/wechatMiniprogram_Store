package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_spu")
public class ProductSpuDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private String keyword;
    private String introduction;
    private String description;
    private String picUrl;
    private String sliderPicUrls;
    private String videoUrl;
    private Integer type; // 1=实物 2=虚拟
    private Integer price; // 单位：分
    private Integer marketPrice;
    private Integer stock;
    private Integer salesCount;
    private Integer sort;
    private Integer status; // 0=下架 1=上架
    /** 单品基础运费（分）；为空时跟随全局运费规则 */
    private Integer freightBaseFee;
    /** 单品包邮门槛（分）；为空时跟随全局运费规则 */
    private Integer freightFreeThreshold;
}
