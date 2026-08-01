package com.shop.module.product.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductSpuMapper extends BaseMapperX<ProductSpuDO> {

    @Select("""
            SELECT p.name
            FROM product_spu p
            WHERE p.status = 1 AND p.deleted = b'0'
            GROUP BY p.name
            ORDER BY MAX(p.sales_count) DESC, MAX(p.id) DESC
            LIMIT #{limit}
            """)
    List<String> selectHotKeywords(@Param("limit") int limit);

    @Select("""
            SELECT p.name
            FROM product_spu p
            JOIN product_category c ON c.id = p.category_id
            WHERE p.status = 1 AND p.deleted = b'0'
              AND c.status = 1 AND c.deleted = b'0'
              AND (p.name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.keyword LIKE CONCAT('%', #{keyword}, '%')
                   OR p.introduction LIKE CONCAT('%', #{keyword}, '%')
                   OR c.name LIKE CONCAT('%', #{keyword}, '%'))
            GROUP BY p.name
            ORDER BY MAX(p.sales_count) DESC, MAX(p.id) DESC
            LIMIT #{limit}
            """)
    List<String> selectSearchSuggestions(@Param("keyword") String keyword, @Param("limit") int limit);
}
