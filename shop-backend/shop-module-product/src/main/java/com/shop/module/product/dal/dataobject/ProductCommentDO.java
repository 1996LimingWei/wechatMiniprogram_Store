package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品评论 DO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_comment")
public class ProductCommentDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评论用户ID */
    private Long userId;

    /** 商品SPU ID */
    private Long spuId;

    /** 评论内容 */
    private String content;

    /** 状态 1=显示 0=隐藏 */
    private Integer status;
}
