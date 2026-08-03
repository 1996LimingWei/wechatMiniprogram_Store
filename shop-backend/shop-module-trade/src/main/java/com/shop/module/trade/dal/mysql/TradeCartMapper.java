package com.shop.module.trade.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradeCartMapper extends BaseMapperX<TradeCartDO> {

    @Delete("<script>DELETE FROM trade_cart WHERE user_id = #{userId} AND sku_id IN "
            + "<foreach collection='skuIds' item='skuId' open='(' separator=',' close=')'>#{skuId}</foreach></script>")
    int physicalDeleteByUserAndSkuIds(@Param("userId") Long userId, @Param("skuIds") List<Long> skuIds);

    @Delete("DELETE FROM trade_cart WHERE user_id = #{userId} AND checked = 1")
    int physicalDeleteCheckedByUserId(@Param("userId") Long userId);
}
