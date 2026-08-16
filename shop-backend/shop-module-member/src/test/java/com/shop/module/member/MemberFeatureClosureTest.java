package com.shop.module.member;

import com.shop.common.exception.ServerException;
import com.shop.framework.security.TokenService;
import com.shop.module.member.config.MemberFeatureProperties;
import com.shop.module.member.controller.AppMemberController;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import com.shop.module.member.vo.MemberProfileUpdateReqVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MemberFeatureClosureTest {

    @Test
    void shouldRejectMembershipAndProfileWritesWhenFeaturesAreDisabled() {
        MemberFeatureProperties properties = new MemberFeatureProperties();
        AppMemberController controller = new AppMemberController(
                mock(MemberUserMapper.class), mock(TokenService.class), properties);

        ServerException membershipException = assertThrows(ServerException.class, controller::center);
        ServerException profileException = assertThrows(ServerException.class,
                () -> controller.updateProfile(new MemberProfileUpdateReqVO(), mock(jakarta.servlet.http.HttpServletRequest.class)));

        assertEquals(403, membershipException.getCode());
        assertEquals(403, profileException.getCode());
    }
}
