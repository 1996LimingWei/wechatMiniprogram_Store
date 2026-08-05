package com.shop.module.product.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.product.dal.dataobject.ProductCommentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品评论 Mapper
 */
@Mapper
public interface ProductCommentMapper extends BaseMapperX<ProductCommentDO> {
}
