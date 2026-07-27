package com.shop.module.trade.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.trade.dal.dataobject.TradeOrderLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeOrderLogMapper extends BaseMapperX<TradeOrderLogDO> {
}
