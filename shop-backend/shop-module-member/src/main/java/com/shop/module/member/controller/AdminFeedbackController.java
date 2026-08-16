package com.shop.module.member.controller;

import com.shop.common.exception.ServerException;
import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.framework.security.LoginUser;
import com.shop.module.member.service.MemberFeedbackService;
import com.shop.module.member.vo.FeedbackHandleReqVO;
import com.shop.module.member.vo.FeedbackRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final MemberFeedbackService feedbackService;

    @GetMapping("/page")
    public CommonResult<PageResult<FeedbackRespVO>> page(
            PageParam pageParam,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type) {
        return CommonResult.success(feedbackService.getPage(pageParam, status, type));
    }

    @GetMapping("/detail")
    public CommonResult<FeedbackRespVO> detail(@RequestParam Long id) {
        return CommonResult.success(feedbackService.getDetail(id));
    }

    @PostMapping("/handle")
    public CommonResult<Boolean> handle(@RequestBody FeedbackHandleReqVO request) {
        feedbackService.handle(currentAdminUserId(), request);
        return CommonResult.success(true);
    }

    private Long currentAdminUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser user
                && Integer.valueOf(2).equals(user.getUserType())) {
            return user.getUserId();
        }
        throw new ServerException(401, "管理员未登录");
    }
}
