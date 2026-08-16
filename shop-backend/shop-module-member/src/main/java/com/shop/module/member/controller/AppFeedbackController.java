package com.shop.module.member.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.framework.security.LoginUser;
import com.shop.module.member.service.MemberFeedbackService;
import com.shop.module.member.vo.FeedbackCreateReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/app-api/feedback")
@RequiredArgsConstructor
public class AppFeedbackController {

    private final MemberFeedbackService feedbackService;

    @PostMapping("/submit")
    public CommonResult<Map<String, Long>> submit(@RequestBody FeedbackCreateReqVO request) {
        return CommonResult.success(Map.of("id", feedbackService.create(currentUserId(), request)));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser user
                && Integer.valueOf(1).equals(user.getUserType())) {
            return user.getUserId();
        }
        throw new com.shop.common.exception.ServerException(401, "请先登录");
    }
}
