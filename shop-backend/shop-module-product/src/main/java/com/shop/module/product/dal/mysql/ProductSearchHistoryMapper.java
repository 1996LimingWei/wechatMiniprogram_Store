package com.shop.module.product.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.product.dal.dataobject.ProductSearchHistoryDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSearchHistoryMapper extends BaseMapperX<ProductSearchHistoryDO> {

    @Insert("""
            INSERT INTO product_search_history(user_id, keyword)
            VALUES (#{userId}, #{keyword})
            ON DUPLICATE KEY UPDATE deleted = b'0', update_time = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Update("""
            UPDATE product_search_history
            SET deleted = b'1', update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND deleted = b'0'
            """)
    int clearByUserId(@Param("userId") Long userId);
}
