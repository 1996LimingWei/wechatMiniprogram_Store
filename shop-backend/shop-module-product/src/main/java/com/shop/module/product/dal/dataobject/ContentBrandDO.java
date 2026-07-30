package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_brand")
public class ContentBrandDO extends BaseDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String picUrl;
    private Integer floorPrice;
    private Integer sort;
    private Integer status;
}
