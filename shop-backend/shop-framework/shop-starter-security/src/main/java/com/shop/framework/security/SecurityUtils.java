package com.shop.framework.security;

import com.shop.common.exception.ServerException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getRequiredAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser
                && Integer.valueOf(2).equals(loginUser.getUserType())) {
            return loginUser.getUserId();
        }
        throw new ServerException(401, "管理员未登录");
    }
}
