package com.shop.module.member.dal.mysql;

import com.shop.framework.mybatis.core.BaseMapperX;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员用户 Mapper
 */
@Mapper
public interface MemberUserMapper extends BaseMapperX<MemberUserDO> {
}
