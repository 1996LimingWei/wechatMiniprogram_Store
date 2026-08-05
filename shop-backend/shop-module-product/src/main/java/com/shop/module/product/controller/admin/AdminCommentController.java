package com.shop.module.product.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.product.dal.dataobject.ProductCommentDO;
import com.shop.module.product.dal.dataobject.ProductSpuDO;
import com.shop.module.product.dal.mysql.ProductCommentMapper;
import com.shop.module.product.dal.mysql.ProductSpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理后台 — 评论管理
 */
@RestController
@RequestMapping("/admin-api/product/comment")
@RequiredArgsConstructor
public class AdminCommentController {

    private final ProductCommentMapper commentMapper;
    private final ProductSpuMapper productSpuMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 评论分页列表（关联用户昵称+商品名）
     */
    @RequestMapping("/page")
    public CommonResult<PageResult<Map<String, Object>>> page(
            PageParam pageParam,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<ProductCommentDO> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ProductCommentDO::getStatus, status);
        }
        wrapper.orderByDesc(ProductCommentDO::getCreateTime);

        IPage<ProductCommentDO> pageResult = commentMapper.selectPage(
                new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        List<ProductCommentDO> records = pageResult.getRecords();

        // 批量查询用户昵称
        Set<Long> userIds = records.stream().map(ProductCommentDO::getUserId).collect(Collectors.toSet());
        Map<Long, String> userNicknames = Map.of();
        if (!userIds.isEmpty()) {
            userNicknames = jdbcTemplate.queryForList(
                    "SELECT id, nickname FROM member_user WHERE id IN (" +
                    userIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")")
                    .stream().collect(Collectors.toMap(
                            row -> ((Number) row.get("id")).longValue(),
                            row -> row.get("nickname") == null ? "" : row.get("nickname").toString()));
        }

        // 批量查询商品名称
        Set<Long> spuIds = records.stream().map(ProductCommentDO::getSpuId).collect(Collectors.toSet());
        Map<Long, String> spuNames = Map.of();
        if (!spuIds.isEmpty()) {
            List<ProductSpuDO> spus = productSpuMapper.selectBatchIds(spuIds);
            spuNames = spus.stream().collect(Collectors.toMap(ProductSpuDO::getId, ProductSpuDO::getName));
        }

        // 组装响应
        Map<Long, String> finalUserNicknames = userNicknames;
        Map<Long, String> finalSpuNames = spuNames;
        List<Map<String, Object>> list = records.stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("userId", c.getUserId());
            item.put("userNickname", finalUserNicknames.getOrDefault(c.getUserId(), "未知用户"));
            item.put("spuId", c.getSpuId());
            item.put("spuName", finalSpuNames.getOrDefault(c.getSpuId(), "已删除商品"));
            item.put("content", c.getContent());
            item.put("status", c.getStatus());
            item.put("createTime", c.getCreateTime());
            return item;
        }).toList();

        return CommonResult.success(new PageResult<>(list, pageResult.getTotal()));
    }

    /**
     * 评论状态变更
     */
    @RequestMapping("/status")
    public CommonResult<Boolean> updateStatus(@RequestBody Map<String, Object> body) {
        Long id = ((Number) body.get("id")).longValue();
        Integer status = ((Number) body.get("status")).intValue();

        ProductCommentDO comment = new ProductCommentDO();
        comment.setId(id);
        comment.setStatus(status);
        commentMapper.updateById(comment);
        return CommonResult.success(true);
    }
}
