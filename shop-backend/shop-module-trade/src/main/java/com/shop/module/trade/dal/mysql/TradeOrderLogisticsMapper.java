package com.shop.module.trade.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeOrderLogisticsMapper extends BaseMapper<TradeOrderLogisticsDO> {
}
