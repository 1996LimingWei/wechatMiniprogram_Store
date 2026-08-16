package com.shop.module.product.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.product.dal.dataobject.MaterialAssetDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MaterialAssetMapper extends BaseMapperX<MaterialAssetDO> {

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM product_category WHERE deleted = b'0' AND icon = #{url})
            + (SELECT COUNT(*) FROM product_spu WHERE deleted = b'0' AND (pic_url = #{url} OR slider_pic_urls LIKE CONCAT('%', #{url}, '%') OR description LIKE CONCAT('%', #{url}, '%')))
            + (SELECT COUNT(*) FROM product_sku WHERE deleted = b'0' AND pic_url = #{url})
            + (SELECT COUNT(*) FROM content_banner WHERE deleted = b'0' AND pic_url = #{url})
            + (SELECT COUNT(*) FROM content_channel WHERE deleted = b'0' AND icon_url = #{url})
            + (SELECT COUNT(*) FROM content_brand WHERE deleted = b'0' AND pic_url = #{url})
            + (SELECT COUNT(*) FROM content_topic WHERE deleted = b'0' AND pic_url = #{url})
            """)
    int countReferences(@Param("url") String url);

    @Select("""
            SELECT CONCAT('商品分类 #', id, ' ', name, '：分类图标') FROM product_category WHERE deleted = b'0' AND icon = #{url}
            UNION ALL
            SELECT CONCAT('商品 #', id, ' ', name, '：主图') FROM product_spu WHERE deleted = b'0' AND pic_url = #{url}
            UNION ALL
            SELECT CONCAT('商品 #', id, ' ', name, '：轮播图') FROM product_spu WHERE deleted = b'0' AND slider_pic_urls LIKE CONCAT('%', #{url}, '%')
            UNION ALL
            SELECT CONCAT('商品 #', id, ' ', name, '：详情图') FROM product_spu WHERE deleted = b'0' AND description LIKE CONCAT('%', #{url}, '%')
            UNION ALL
            SELECT CONCAT('SKU #', id, '：规格图片') FROM product_sku WHERE deleted = b'0' AND pic_url = #{url}
            UNION ALL
            SELECT CONCAT('Banner #', id, ' ', title, '：轮播图片') FROM content_banner WHERE deleted = b'0' AND pic_url = #{url}
            UNION ALL
            SELECT CONCAT('频道 #', id, ' ', name, '：频道图标') FROM content_channel WHERE deleted = b'0' AND icon_url = #{url}
            UNION ALL
            SELECT CONCAT('品牌 #', id, ' ', name, '：品牌图片') FROM content_brand WHERE deleted = b'0' AND pic_url = #{url}
            UNION ALL
            SELECT CONCAT('专题 #', id, ' ', title, '：专题图片') FROM content_topic WHERE deleted = b'0' AND pic_url = #{url}
            """)
    List<String> selectReferences(@Param("url") String url);
}
