package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_asset")
public class MaterialAssetDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String objectKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String bizType;
    private Integer referenceCount;
    private Long createdBy;
}
