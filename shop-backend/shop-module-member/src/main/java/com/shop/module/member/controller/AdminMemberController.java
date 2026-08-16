package com.shop.module.member.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.member.service.AdminMemberService;
import com.shop.module.member.vo.MemberUserRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 管理后台 — 会员管理
 */
@RestController
@RequestMapping("/admin-api/member/user")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    /**
     * 会员分页列表
     */
    @GetMapping("/page")
    public CommonResult<PageResult<MemberUserRespVO>> page(
            PageParam pageParam,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String mobile) {
        return CommonResult.success(adminMemberService.getUserPage(pageParam, nickname, mobile));
    }

    /** 会员详情。 */
    @GetMapping("/detail")
    public CommonResult<MemberUserRespVO> detail(@RequestParam Long id) {
        return CommonResult.success(adminMemberService.getUserDetail(id));
    }
}
