package com.shop.module.member.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.member.dal.dataobject.MemberFeedbackDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberFeedbackMapper extends BaseMapperX<MemberFeedbackDO> {
}
