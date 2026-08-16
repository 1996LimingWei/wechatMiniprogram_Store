package com.shop.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import com.shop.module.member.vo.MemberUserRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * 管理后台 — 会员服务
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberUserMapper memberUserMapper;

    /**
     * 会员分页列表
     */
    public PageResult<MemberUserRespVO> getUserPage(PageParam pageParam, String nickname, String mobile) {
        LambdaQueryWrapper<MemberUserDO> wrapper = new LambdaQueryWrapper<>();
        if (nickname != null && !nickname.isBlank()) {
            wrapper.like(MemberUserDO::getNickname, nickname.trim());
        }
        if (mobile != null && !mobile.isBlank()) {
            wrapper.like(MemberUserDO::getMobile, mobile.trim());
        }
        wrapper.orderByDesc(MemberUserDO::getCreateTime);
        PageResult<MemberUserDO> page = memberUserMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    /** 会员详情。 */
    public MemberUserRespVO getUserDetail(Long id) {
        MemberUserDO user = memberUserMapper.selectById(id);
        if (user == null) {
            throw new ServerException(404, "会员不存在");
        }

        return toResp(user);
    }

    private MemberUserRespVO toResp(MemberUserDO user) {
        MemberUserRespVO response = new MemberUserRespVO();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setMobile(maskMobile(user.getMobile()));
        response.setStatus(user.getStatus());
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) return "";
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
