package com.shop.module.trade.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.pojo.CommonResult;
import com.shop.common.pojo.PageParam;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.dal.dataobject.MarketingCouponDO;
import com.shop.module.trade.dal.dataobject.MarketingCouponTemplateDO;
import com.shop.module.trade.dal.dataobject.MarketingPromotionRuleDO;
import com.shop.module.trade.dal.dataobject.MarketingShippingRuleDO;
import com.shop.module.trade.dal.mysql.MarketingCouponMapper;
import com.shop.module.trade.dal.mysql.MarketingCouponTemplateMapper;
import com.shop.module.trade.dal.mysql.MarketingPromotionRuleMapper;
import com.shop.module.trade.dal.mysql.MarketingShippingRuleMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import com.shop.module.trade.util.TradeRequestUtils;
import com.shop.module.trade.service.MarketingShippingService;
import com.shop.module.trade.vo.MarketingShippingAuditRespVO;
import com.shop.module.trade.vo.MarketingShippingRuleRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminMarketingController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MarketingCouponTemplateMapper templateMapper;
    private final MarketingCouponMapper couponMapper;
    private final MarketingPromotionRuleMapper promotionRuleMapper;
    private final MarketingShippingRuleMapper shippingRuleMapper;
    private final MarketingShippingService marketingShippingService;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 优惠券模板 CRUD ====================

    @GetMapping("/admin-api/marketing/coupon-template/list")
    public CommonResult<Map<String, Object>> listCouponTemplates(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "type", required = false) Integer type) {
        LambdaQueryWrapper<MarketingCouponTemplateDO> wrapper = new LambdaQueryWrapper<MarketingCouponTemplateDO>()
                .eq(status != null, MarketingCouponTemplateDO::getStatus, status)
                .eq(type != null, MarketingCouponTemplateDO::getType, type)
                .orderByDesc(MarketingCouponTemplateDO::getId);
        PageResult<MarketingCouponTemplateDO> page = templateMapper.selectPage(
                toPageParam(pageNo, pageSize), wrapper);
        List<Map<String, Object>> list = page.getList().stream().map(this::toTemplateItem).toList();
        return CommonResult.success(Map.of("list", list, "total", page.getTotal()));
    }

    @PostMapping("/admin-api/marketing/coupon-template/create")
    public CommonResult<Map<String, Object>> createCouponTemplate(@RequestBody Map<String, Object> body) {
        MarketingCouponTemplateDO tpl = new MarketingCouponTemplateDO();
        populateTemplate(tpl, body);
        tpl.setClaimedCount(0);
        templateMapper.insert(tpl);
        return CommonResult.success(toTemplateItem(tpl));
    }

    @PostMapping("/admin-api/marketing/coupon-template/update")
    public CommonResult<Map<String, Object>> updateCouponTemplate(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        MarketingCouponTemplateDO tpl = templateMapper.selectById(id);
        if (tpl == null) {
            throw new com.shop.common.exception.ServerException(400, "优惠券模板不存在");
        }
        populateTemplate(tpl, body);
        templateMapper.updateById(tpl);
        return CommonResult.success(toTemplateItem(tpl));
    }

    @PostMapping("/admin-api/marketing/coupon-template/delete")
    public CommonResult<Boolean> deleteCouponTemplate(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        templateMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @PutMapping("/admin-api/marketing/coupon-template/update-status")
    public CommonResult<Boolean> updateCouponTemplateStatus(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        Integer status = TradeRequestUtils.getInt(body, "status", 0);
        templateMapper.update(null, new LambdaUpdateWrapper<MarketingCouponTemplateDO>()
                .eq(MarketingCouponTemplateDO::getId, id)
                .set(MarketingCouponTemplateDO::getStatus, status));
        return CommonResult.success(true);
    }

    // ==================== 满减规则 CRUD ====================

    @GetMapping("/admin-api/marketing/promotion/list")
    public CommonResult<Map<String, Object>> listPromotions(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<MarketingPromotionRuleDO> wrapper = new LambdaQueryWrapper<MarketingPromotionRuleDO>()
                .eq(status != null, MarketingPromotionRuleDO::getStatus, status)
                .orderByAsc(MarketingPromotionRuleDO::getThresholdAmount);
        PageResult<MarketingPromotionRuleDO> page = promotionRuleMapper.selectPage(
                toPageParam(pageNo, pageSize), wrapper);
        List<Map<String, Object>> list = page.getList().stream().map(this::toPromotionItem).toList();
        return CommonResult.success(Map.of("list", list, "total", page.getTotal()));
    }

    @PostMapping("/admin-api/marketing/promotion/create")
    public CommonResult<Map<String, Object>> createPromotion(@RequestBody Map<String, Object> body) {
        MarketingPromotionRuleDO rule = new MarketingPromotionRuleDO();
        populatePromotion(rule, body);
        promotionRuleMapper.insert(rule);
        return CommonResult.success(toPromotionItem(rule));
    }

    @PostMapping("/admin-api/marketing/promotion/update")
    public CommonResult<Map<String, Object>> updatePromotion(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        MarketingPromotionRuleDO rule = promotionRuleMapper.selectById(id);
        if (rule == null) {
            throw new com.shop.common.exception.ServerException(400, "满减规则不存在");
        }
        populatePromotion(rule, body);
        promotionRuleMapper.updateById(rule);
        return CommonResult.success(toPromotionItem(rule));
    }

    @PostMapping("/admin-api/marketing/promotion/delete")
    public CommonResult<Boolean> deletePromotion(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        promotionRuleMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @PutMapping("/admin-api/marketing/promotion/update-status")
    public CommonResult<Boolean> updatePromotionStatus(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        Integer status = TradeRequestUtils.getInt(body, "status", 0);
        promotionRuleMapper.update(null, new LambdaUpdateWrapper<MarketingPromotionRuleDO>()
                .eq(MarketingPromotionRuleDO::getId, id)
                .set(MarketingPromotionRuleDO::getStatus, status));
        return CommonResult.success(true);
    }

    // ==================== 包邮规则 CRUD ====================

    @GetMapping("/admin-api/marketing/shipping/list")
    public CommonResult<Map<String, Object>> listShippingRules(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        PageResult<MarketingShippingRuleDO> page = shippingRuleMapper.selectPage(
                toPageParam(pageNo, pageSize),
                new LambdaQueryWrapper<MarketingShippingRuleDO>().orderByDesc(MarketingShippingRuleDO::getId));
        List<Map<String, Object>> list = page.getList().stream().map(this::toShippingItem).toList();
        return CommonResult.success(Map.of("list", list, "total", page.getTotal()));
    }

    @GetMapping("/admin-api/marketing/shipping/current")
    public CommonResult<MarketingShippingRuleRespVO> getCurrentShippingRule() {
        return CommonResult.success(toShippingVO(marketingShippingService.getCurrentRule()));
    }

    @GetMapping("/admin-api/marketing/shipping/audit-page")
    public CommonResult<PageResult<MarketingShippingAuditRespVO>> listShippingAudits(
            PageParam pageParam,
            @RequestParam(value = "ruleId", required = false) Long ruleId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
                 WHERE l.request_uri IN (
                       '/admin-api/marketing/shipping/create',
                       '/admin-api/marketing/shipping/update',
                       '/admin-api/marketing/shipping/update-status'
                 )
                """);
        if (ruleId != null && ruleId > 0) {
            where.append(" AND l.business_ref = ?");
            args.add(String.valueOf(ruleId));
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_operation_log l" + where,
                Long.class,
                args.toArray());
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(pageParam.getPageSize());
        queryArgs.add((pageParam.getPageNo() - 1) * pageParam.getPageSize());
        List<MarketingShippingAuditRespVO> rows = jdbcTemplate.query("""
                SELECT l.id, l.admin_user_id, COALESCE(u.username, '') username, COALESCE(u.nickname, '') nickname,
                       l.method, l.request_uri, l.business_ref, l.success, l.ip, l.duration_ms, l.message, l.create_time
                  FROM sys_operation_log l
                  LEFT JOIN sys_admin_user u ON u.id = l.admin_user_id
                """ + where + " ORDER BY l.create_time DESC, l.id DESC LIMIT ? OFFSET ?",
                (rs, index) -> {
                    MarketingShippingAuditRespVO item = new MarketingShippingAuditRespVO();
                    item.setId(rs.getLong("id"));
                    item.setAdminUserId(rs.getLong("admin_user_id"));
                    item.setUsername(rs.getString("username"));
                    item.setNickname(rs.getString("nickname"));
                    item.setMethod(rs.getString("method"));
                    item.setRequestUri(rs.getString("request_uri"));
                    item.setBusinessRef(rs.getString("business_ref"));
                    item.setSuccess(rs.getInt("success"));
                    item.setIp(rs.getString("ip"));
                    item.setDurationMs(rs.getLong("duration_ms"));
                    item.setMessage(rs.getString("message"));
                    Timestamp createTime = rs.getTimestamp("create_time");
                    item.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
                    return item;
                }, queryArgs.toArray());
        return CommonResult.success(new PageResult<>(rows, total == null ? 0L : total));
    }

    @PostMapping("/admin-api/marketing/shipping/create")
    public CommonResult<Map<String, Object>> createShippingRule(@RequestBody Map<String, Object> body) {
        MarketingShippingRuleDO rule = new MarketingShippingRuleDO();
        populateShipping(rule, body);
        shippingRuleMapper.insert(rule);
        return CommonResult.success(toShippingItem(rule));
    }

    @PostMapping("/admin-api/marketing/shipping/update")
    public CommonResult<Map<String, Object>> updateShippingRule(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        MarketingShippingRuleDO rule = shippingRuleMapper.selectById(id);
        if (rule == null) {
            throw new com.shop.common.exception.ServerException(400, "包邮规则不存在");
        }
        populateShipping(rule, body);
        shippingRuleMapper.updateById(rule);
        return CommonResult.success(toShippingItem(rule));
    }

    @PutMapping("/admin-api/marketing/shipping/update-status")
    public CommonResult<Boolean> updateShippingStatus(@RequestBody Map<String, Object> body) {
        Long id = TradeRequestUtils.getLong(body, "id", 0L);
        Integer status = TradeRequestUtils.getInt(body, "status", 0);
        if (status == null || (status != 0 && status != 1)) {
            throw new com.shop.common.exception.ServerException(400, "状态不正确");
        }
        shippingRuleMapper.update(null, new LambdaUpdateWrapper<MarketingShippingRuleDO>()
                .eq(MarketingShippingRuleDO::getId, id)
                .set(MarketingShippingRuleDO::getStatus, status));
        return CommonResult.success(true);
    }

    // ==================== 优惠券实例查询 ====================

    @GetMapping("/admin-api/marketing/coupon/instance/list")
    public CommonResult<Map<String, Object>> listCouponInstances(
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<MarketingCouponDO> wrapper = new LambdaQueryWrapper<MarketingCouponDO>()
                .eq(templateId != null, MarketingCouponDO::getTemplateId, templateId)
                .eq(userId != null, MarketingCouponDO::getUserId, userId)
                .eq(status != null, MarketingCouponDO::getStatus, status)
                .orderByDesc(MarketingCouponDO::getId);
        PageResult<MarketingCouponDO> page = couponMapper.selectPage(toPageParam(pageNo, pageSize), wrapper);
        List<Map<String, Object>> list = page.getList().stream().map(coupon -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", coupon.getId());
            item.put("userId", coupon.getUserId());
            item.put("templateId", coupon.getTemplateId());
            item.put("status", coupon.getStatus());
            item.put("orderId", coupon.getOrderId());
            item.put("expireTime", coupon.getExpireTime() == null ? "" : coupon.getExpireTime().format(TIME_FORMATTER));
            item.put("usedTime", coupon.getUsedTime() == null ? "" : coupon.getUsedTime().format(TIME_FORMATTER));
            MarketingCouponTemplateDO tpl = templateMapper.selectById(coupon.getTemplateId());
            if (tpl != null) {
                item.put("name", tpl.getName());
                item.put("discountAmount", TradeMoneyUtils.formatYuan(tpl.getDiscountAmount()));
            }
            return item;
        }).toList();
        return CommonResult.success(Map.of("list", list, "total", page.getTotal()));
    }

    // ==================== 辅助方法 ====================

    private PageParam toPageParam(Integer pageNo, Integer pageSize) {
        PageParam param = new PageParam();
        param.setPageNo(pageNo);
        param.setPageSize(pageSize);
        return param;
    }

    private void populateTemplate(MarketingCouponTemplateDO tpl, Map<String, Object> body) {
        if (body.containsKey("name")) tpl.setName((String) body.get("name"));
        if (body.containsKey("type")) tpl.setType(TradeRequestUtils.getInt(body, "type", 1));
        if (body.containsKey("thresholdAmount")) tpl.setThresholdAmount(TradeRequestUtils.getInt(body, "thresholdAmount", 0));
        if (body.containsKey("discountAmount")) tpl.setDiscountAmount(TradeRequestUtils.getInt(body, "discountAmount", 0));
        if (body.containsKey("totalCount")) tpl.setTotalCount(TradeRequestUtils.getInt(body, "totalCount", 0));
        if (body.containsKey("perUserLimit")) tpl.setPerUserLimit(TradeRequestUtils.getInt(body, "perUserLimit", 1));
        if (body.containsKey("validityType")) tpl.setValidityType(TradeRequestUtils.getInt(body, "validityType", 1));
        if (body.containsKey("validDays")) tpl.setValidDays(TradeRequestUtils.getInt(body, "validDays", null));
        if (body.containsKey("validStartTime")) {
            String val = (String) body.get("validStartTime");
            tpl.setValidStartTime(val == null || val.isBlank() ? null : LocalDateTime.parse(val, TIME_FORMATTER));
        }
        if (body.containsKey("validEndTime")) {
            String val = (String) body.get("validEndTime");
            tpl.setValidEndTime(val == null || val.isBlank() ? null : LocalDateTime.parse(val, TIME_FORMATTER));
        }
        if (tpl.getDiscountAmount() == null || tpl.getDiscountAmount() <= 0) {
            throw new com.shop.common.exception.ServerException(400, "优惠金额必须大于 0");
        }
    }

    private void populatePromotion(MarketingPromotionRuleDO rule, Map<String, Object> body) {
        if (body.containsKey("name")) rule.setName((String) body.get("name"));
        if (body.containsKey("type")) rule.setType(TradeRequestUtils.getInt(body, "type", 1));
        if (body.containsKey("thresholdAmount")) rule.setThresholdAmount(TradeRequestUtils.getInt(body, "thresholdAmount", 0));
        if (body.containsKey("discountAmount")) rule.setDiscountAmount(TradeRequestUtils.getInt(body, "discountAmount", 0));
        if (body.containsKey("priority")) rule.setPriority(TradeRequestUtils.getInt(body, "priority", 0));
        if (body.containsKey("startTime")) {
            String val = (String) body.get("startTime");
            rule.setStartTime(val == null || val.isBlank() ? null : LocalDateTime.parse(val, TIME_FORMATTER));
        }
        if (body.containsKey("endTime")) {
            String val = (String) body.get("endTime");
            rule.setEndTime(val == null || val.isBlank() ? null : LocalDateTime.parse(val, TIME_FORMATTER));
        }
        if (rule.getThresholdAmount() == null || rule.getThresholdAmount() <= 0) {
            throw new com.shop.common.exception.ServerException(400, "满减门槛必须大于 0");
        }
        if (rule.getDiscountAmount() == null || rule.getDiscountAmount() <= 0) {
            throw new com.shop.common.exception.ServerException(400, "优惠金额必须大于 0");
        }
        if (rule.getDiscountAmount() > rule.getThresholdAmount()) {
            throw new com.shop.common.exception.ServerException(400, "优惠金额不能大于满减门槛");
        }
    }

    private void populateShipping(MarketingShippingRuleDO rule, Map<String, Object> body) {
        if (body.containsKey("name")) rule.setName((String) body.get("name"));
        if (body.containsKey("freeThreshold")) rule.setFreeThreshold(TradeRequestUtils.getInt(body, "freeThreshold", 0));
        if (body.containsKey("baseFee")) rule.setBaseFee(TradeRequestUtils.getInt(body, "baseFee", 0));
        if (body.containsKey("status")) rule.setStatus(TradeRequestUtils.getInt(body, "status", 1));
        if (body.containsKey("startTime")) rule.setStartTime(parseTime((String) body.get("startTime")));
        if (body.containsKey("endTime")) rule.setEndTime(parseTime((String) body.get("endTime")));
        if (rule.getName() == null || rule.getName().trim().length() < 2 || rule.getName().trim().length() > 64) {
            throw new com.shop.common.exception.ServerException(400, "规则名称长度需为 2 至 64 字");
        }
        rule.setName(rule.getName().trim());
        if (rule.getFreeThreshold() == null || rule.getFreeThreshold() < 0) {
            throw new com.shop.common.exception.ServerException(400, "包邮门槛不能为负数");
        }
        if (rule.getBaseFee() == null || rule.getBaseFee() < 0) {
            throw new com.shop.common.exception.ServerException(400, "基础运费不能为负数");
        }
        if (rule.getStatus() == null || (rule.getStatus() != 0 && rule.getStatus() != 1)) {
            throw new com.shop.common.exception.ServerException(400, "状态不正确");
        }
        if (rule.getStartTime() != null && rule.getEndTime() != null
                && !rule.getEndTime().isAfter(rule.getStartTime())) {
            throw new com.shop.common.exception.ServerException(400, "停用时间必须晚于生效时间");
        }
    }

    private LocalDateTime parseTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value, TIME_FORMATTER);
    }

    private Map<String, Object> toTemplateItem(MarketingCouponTemplateDO tpl) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", tpl.getId());
        item.put("name", tpl.getName());
        item.put("type", tpl.getType());
        item.put("thresholdAmount", TradeMoneyUtils.formatYuan(tpl.getThresholdAmount()));
        item.put("discountAmount", TradeMoneyUtils.formatYuan(tpl.getDiscountAmount()));
        item.put("totalCount", tpl.getTotalCount());
        item.put("claimedCount", tpl.getClaimedCount());
        item.put("perUserLimit", tpl.getPerUserLimit());
        item.put("validityType", tpl.getValidityType());
        item.put("validStartTime", tpl.getValidStartTime() == null ? "" : tpl.getValidStartTime().format(TIME_FORMATTER));
        item.put("validEndTime", tpl.getValidEndTime() == null ? "" : tpl.getValidEndTime().format(TIME_FORMATTER));
        item.put("validDays", tpl.getValidDays());
        item.put("status", tpl.getStatus());
        item.put("createTime", tpl.getCreateTime() == null ? "" : tpl.getCreateTime().format(TIME_FORMATTER));
        return item;
    }

    private Map<String, Object> toPromotionItem(MarketingPromotionRuleDO rule) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rule.getId());
        item.put("name", rule.getName());
        item.put("type", rule.getType());
        item.put("thresholdAmount", TradeMoneyUtils.formatYuan(rule.getThresholdAmount()));
        item.put("discountAmount", TradeMoneyUtils.formatYuan(rule.getDiscountAmount()));
        item.put("priority", rule.getPriority());
        item.put("startTime", rule.getStartTime() == null ? "" : rule.getStartTime().format(TIME_FORMATTER));
        item.put("endTime", rule.getEndTime() == null ? "" : rule.getEndTime().format(TIME_FORMATTER));
        item.put("status", rule.getStatus());
        item.put("createTime", rule.getCreateTime() == null ? "" : rule.getCreateTime().format(TIME_FORMATTER));
        return item;
    }

    private Map<String, Object> toShippingItem(MarketingShippingRuleDO rule) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rule.getId());
        item.put("name", rule.getName());
        item.put("freeThreshold", TradeMoneyUtils.formatYuan(rule.getFreeThreshold()));
        item.put("baseFee", TradeMoneyUtils.formatYuan(rule.getBaseFee()));
        item.put("status", rule.getStatus());
        item.put("startTime", rule.getStartTime() == null ? "" : rule.getStartTime().format(TIME_FORMATTER));
        item.put("endTime", rule.getEndTime() == null ? "" : rule.getEndTime().format(TIME_FORMATTER));
        item.put("currentActive", isCurrentActive(rule));
        item.put("createTime", rule.getCreateTime() == null ? "" : rule.getCreateTime().format(TIME_FORMATTER));
        return item;
    }

    private MarketingShippingRuleRespVO toShippingVO(MarketingShippingRuleDO rule) {
        if (rule == null) {
            return null;
        }
        MarketingShippingRuleRespVO item = new MarketingShippingRuleRespVO();
        item.setId(rule.getId());
        item.setName(rule.getName());
        item.setFreeThreshold(TradeMoneyUtils.formatYuan(rule.getFreeThreshold()));
        item.setBaseFee(TradeMoneyUtils.formatYuan(rule.getBaseFee()));
        item.setStatus(rule.getStatus());
        item.setStartTime(rule.getStartTime() == null ? "" : rule.getStartTime().format(TIME_FORMATTER));
        item.setEndTime(rule.getEndTime() == null ? "" : rule.getEndTime().format(TIME_FORMATTER));
        item.setCurrentActive(isCurrentActive(rule));
        item.setCreateTime(rule.getCreateTime() == null ? "" : rule.getCreateTime().format(TIME_FORMATTER));
        return item;
    }

    private boolean isCurrentActive(MarketingShippingRuleDO rule) {
        if (rule == null || rule.getStatus() == null || rule.getStatus() != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (rule.getStartTime() == null || !rule.getStartTime().isAfter(now))
                && (rule.getEndTime() == null || rule.getEndTime().isAfter(now));
    }
}
