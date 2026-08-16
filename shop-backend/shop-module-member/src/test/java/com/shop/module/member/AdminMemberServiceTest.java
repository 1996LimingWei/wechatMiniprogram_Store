package com.shop.module.member;

import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import com.shop.module.member.service.AdminMemberService;
import com.shop.module.member.vo.MemberUserRespVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminMemberServiceTest {

    @Test
    void shouldOnlyExposeWhitelistedMemberFields() {
        MemberUserMapper mapper = mock(MemberUserMapper.class);
        MemberUserDO user = new MemberUserDO();
        user.setId(1L);
        user.setNickname("测试用户");
        user.setMobile("13800138000");
        user.setOpenid("openid-secret");
        user.setSessionKey("session-key-secret");
        when(mapper.selectById(1L)).thenReturn(user);

        MemberUserRespVO response = new AdminMemberService(mapper).getUserDetail(1L);
        Set<String> fields = Arrays.stream(MemberUserRespVO.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());

        assertEquals("138****8000", response.getMobile());
        assertFalse(fields.contains("openid"));
        assertFalse(fields.contains("unionid"));
        assertFalse(fields.contains("sessionKey"));
    }
}
