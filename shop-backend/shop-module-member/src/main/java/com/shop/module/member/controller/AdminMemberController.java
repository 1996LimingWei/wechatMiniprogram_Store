package com.shop.module.member.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    @RequestMapping("/page")
    public CommonResult<PageResult<MemberUserDO>> page(
            PageParam pageParam,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String mobile) {
        return CommonResult.success(adminMemberService.getUserPage(pageParam, nickname, mobile));
    }

    /**
     * 会员详情（含地址、订单统计、收藏数）
     */
    @RequestMapping("/detail")
    public CommonResult<Map<String, Object>> detail(@RequestParam Long id) {
        return CommonResult.success(adminMemberService.getUserDetail(id));
    }

    /**
     * 更新会员信息
     */
    @RequestMapping("/update")
    public CommonResult<Boolean> update(@RequestBody MemberUserDO user) {
        adminMemberService.updateUser(user);
        return CommonResult.success(true);
    }
}
