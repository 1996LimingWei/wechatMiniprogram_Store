package com.shop.module.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.member.dal.dataobject.MemberFeedbackDO;
import com.shop.module.member.dal.mysql.MemberFeedbackMapper;
import com.shop.module.member.vo.FeedbackCreateReqVO;
import com.shop.module.member.vo.FeedbackHandleReqVO;
import com.shop.module.member.vo.FeedbackRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberFeedbackService {

    private static final Map<Integer, String> TYPE_NAMES = Map.of(
            1, "商品相关", 2, "物流状况", 3, "客户服务", 4, "优惠活动",
            5, "功能异常", 6, "产品建议", 7, "其他");
    private static final Map<Integer, String> STATUS_NAMES = Map.of(
            0, "待处理", 1, "处理中", 2, "已完成");

    private final MemberFeedbackMapper feedbackMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, FeedbackCreateReqVO request) {
        if (request == null || !TYPE_NAMES.containsKey(request.getType())) {
            throw new ServerException(400, "反馈类型不正确");
        }
        String content = normalize(request.getContent());
        if (content.length() < 5 || content.length() > 500) {
            throw new ServerException(400, "反馈内容应为 5 至 500 个字符");
        }
        String mobile = normalize(request.getMobile());
        if (!mobile.isEmpty() && !mobile.matches("^1\\d{10}$")) {
            throw new ServerException(400, "联系电话格式不正确");
        }
        LocalDateTime now = LocalDateTime.now();
        Long recentCount = feedbackMapper.selectCount(new LambdaQueryWrapper<MemberFeedbackDO>()
                .eq(MemberFeedbackDO::getUserId, userId)
                .ge(MemberFeedbackDO::getCreateTime, now.minusMinutes(10)));
        if (recentCount != null && recentCount >= 3) {
            throw new ServerException(429, "提交过于频繁，请稍后再试");
        }
        Long duplicateCount = feedbackMapper.selectCount(new LambdaQueryWrapper<MemberFeedbackDO>()
                .eq(MemberFeedbackDO::getUserId, userId)
                .eq(MemberFeedbackDO::getType, request.getType())
                .eq(MemberFeedbackDO::getContent, content)
                .ge(MemberFeedbackDO::getCreateTime, now.minusMinutes(10)));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new ServerException(409, "相同反馈已提交，请勿重复提交");
        }

        MemberFeedbackDO feedback = new MemberFeedbackDO();
        feedback.setUserId(userId);
        feedback.setType(request.getType());
        feedback.setContent(content);
        feedback.setMobile(mobile);
        feedback.setStatus(0);
        feedback.setHandleRemark("");
        feedbackMapper.insert(feedback);
        return feedback.getId();
    }

    public PageResult<FeedbackRespVO> getPage(PageParam pageParam, Integer status, Integer type) {
        LambdaQueryWrapper<MemberFeedbackDO> wrapper = new LambdaQueryWrapper<MemberFeedbackDO>()
                .orderByDesc(MemberFeedbackDO::getCreateTime);
        if (status != null) {
            requireStatus(status);
            wrapper.eq(MemberFeedbackDO::getStatus, status);
        }
        if (type != null) {
            if (!TYPE_NAMES.containsKey(type)) throw new ServerException(400, "反馈类型不正确");
            wrapper.eq(MemberFeedbackDO::getType, type);
        }
        IPage<MemberFeedbackDO> page = feedbackMapper.selectPage(
                new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        return new PageResult<>(assemble(page.getRecords(), false), page.getTotal());
    }

    public FeedbackRespVO getDetail(Long id) {
        MemberFeedbackDO feedback = feedbackMapper.selectById(id);
        if (feedback == null) throw new ServerException(404, "反馈不存在");
        return assemble(List.of(feedback), true).getFirst();
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(Long adminUserId, FeedbackHandleReqVO request) {
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new ServerException(400, "反馈编号不正确");
        }
        requireStatus(request.getStatus());
        String remark = normalize(request.getHandleRemark());
        if (request.getStatus() != 0 && (remark.length() < 2 || remark.length() > 500)) {
            throw new ServerException(400, "处理备注应为 2 至 500 个字符");
        }
        if (request.getStatus() == 0 && !remark.isEmpty()) {
            throw new ServerException(400, "待处理状态不能填写处理备注");
        }
        MemberFeedbackDO existing = feedbackMapper.selectById(request.getId());
        if (existing == null) throw new ServerException(404, "反馈不存在");
        if (existing.getStatus() == 2) {
            throw new ServerException(409, "已完成的反馈不能回退状态");
        }
        if (request.getStatus() < existing.getStatus()) {
            throw new ServerException(400, "反馈状态不能回退");
        }
        LocalDateTime handleTime = request.getStatus() == 0 ? null : LocalDateTime.now();
        int updated = feedbackMapper.update(null, new LambdaUpdateWrapper<MemberFeedbackDO>()
                .eq(MemberFeedbackDO::getId, request.getId())
                .eq(MemberFeedbackDO::getUpdateTime, existing.getUpdateTime())
                .set(MemberFeedbackDO::getStatus, request.getStatus())
                .set(MemberFeedbackDO::getHandlerAdminId, request.getStatus() == 0 ? null : adminUserId)
                .set(MemberFeedbackDO::getHandleRemark, remark)
                .set(MemberFeedbackDO::getHandleTime, handleTime));
        if (updated != 1) throw new ServerException(409, "反馈状态已变化，请刷新后重试");
    }

    private List<FeedbackRespVO> assemble(List<MemberFeedbackDO> records, boolean includeFullMobile) {
        if (records.isEmpty()) return List.of();
        Set<Long> userIds = records.stream().map(MemberFeedbackDO::getUserId).collect(Collectors.toSet());
        Set<Long> adminIds = records.stream().map(MemberFeedbackDO::getHandlerAdminId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userNames = queryNames("member_user", "nickname", userIds);
        Map<Long, String> adminNames = queryNames("sys_admin_user", "nickname", adminIds);
        return records.stream().map(item -> {
            FeedbackRespVO response = new FeedbackRespVO();
            response.setId(item.getId());
            response.setType(item.getType());
            response.setTypeName(TYPE_NAMES.getOrDefault(item.getType(), "其他"));
            response.setContent(item.getContent());
            response.setMobile(includeFullMobile ? item.getMobile() : maskMobile(item.getMobile()));
            response.setStatus(item.getStatus());
            response.setStatusName(STATUS_NAMES.getOrDefault(item.getStatus(), "未知"));
            response.setUserNickname(userNames.getOrDefault(item.getUserId(), "已注销用户"));
            response.setHandlerAdminId(item.getHandlerAdminId());
            response.setHandlerName(item.getHandlerAdminId() == null ? "" : adminNames.getOrDefault(item.getHandlerAdminId(), "已删除管理员"));
            response.setHandleRemark(item.getHandleRemark());
            response.setHandleTime(item.getHandleTime());
            response.setCreateTime(item.getCreateTime());
            return response;
        }).toList();
    }

    private Map<Long, String> queryNames(String table, String column, Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        String placeholders = ids.stream().map(value -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, " + column + " FROM " + table + " WHERE id IN (" + placeholders + ")", ids.toArray());
        Map<Long, String> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("id")).longValue(), String.valueOf(row.get(column)));
        }
        return result;
    }

    private void requireStatus(Integer status) {
        if (!STATUS_NAMES.containsKey(status)) throw new ServerException(400, "反馈状态不正确");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) return "";
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
