package com.shop.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.member.dal.dataobject.MemberUserDO;
import com.shop.module.member.dal.mysql.MemberUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台 — 会员服务
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberUserMapper memberUserMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 会员分页列表
     */
    public PageResult<MemberUserDO> getUserPage(PageParam pageParam, String nickname, String mobile) {
        LambdaQueryWrapper<MemberUserDO> wrapper = new LambdaQueryWrapper<>();
        if (nickname != null && !nickname.isBlank()) {
            wrapper.like(MemberUserDO::getNickname, nickname.trim());
        }
        if (mobile != null && !mobile.isBlank()) {
            wrapper.like(MemberUserDO::getMobile, mobile.trim());
        }
        wrapper.orderByDesc(MemberUserDO::getCreateTime);
        return memberUserMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 会员详情（含地址、订单统计、收藏数）
     */
    public Map<String, Object> getUserDetail(Long id) {
        MemberUserDO user = memberUserMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("会员不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("mobile", user.getMobile());
        result.put("status", user.getStatus());
        result.put("memberLevel", user.getMemberLevel() != null ? user.getMemberLevel() : 1);
        result.put("createTime", user.getCreateTime());

        // 收货地址列表
        List<Map<String, Object>> addresses = jdbcTemplate.queryForList(
                "SELECT id, user_name, tel_number, full_region, detail_info, is_default " +
                "FROM member_address WHERE user_id = ? AND deleted = 0 ORDER BY is_default DESC, id DESC", id);
        result.put("addresses", addresses);

        // 订单统计
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_order WHERE user_id = ? AND deleted = 0", Integer.class, id);
        result.put("orderCount", orderCount != null ? orderCount : 0);

        // 最近订单（最近 5 条）
        List<Map<String, Object>> recentOrders = jdbcTemplate.queryForList(
                "SELECT id, order_sn, status, pay_status, actual_price, create_time " +
                "FROM trade_order WHERE user_id = ? AND deleted = 0 ORDER BY create_time DESC LIMIT 5", id);
        result.put("recentOrders", recentOrders);

        // 收藏数
        Integer collectCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member_collect WHERE user_id = ? AND deleted = 0", Integer.class, id);
        result.put("collectCount", collectCount != null ? collectCount : 0);

        // 评论数
        Integer commentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_comment WHERE user_id = ? AND deleted = 0", Integer.class, id);
        result.put("commentCount", commentCount != null ? commentCount : 0);

        return result;
    }

    /**
     * 更新会员信息（昵称、手机号、头像、状态）
     */
    public void updateUser(MemberUserDO update) {
        MemberUserDO existing = memberUserMapper.selectById(update.getId());
        if (existing == null) {
            throw new RuntimeException("会员不存在");
        }
        memberUserMapper.updateById(update);
    }
}
