package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MarketingShippingRuleDO;
import com.shop.module.trade.dal.mysql.MarketingShippingRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketingShippingService {

    private final MarketingShippingRuleMapper shippingRuleMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 计算运费（只有金额、没有商品明细时使用，购物车凑单提示等场景）。
     * 运费规则一律以数据库中启用的规则为准，不再读取配置文件兜底。
     */
    public int calculateFreight(int goodsTotalPrice) {
        return feeByRule(requireCurrentRule(), goodsTotalPrice);
    }

    /**
     * 计算订单运费：每个商品按自己的运费规则算出一条候选运费，整单取最大值。
     * 这样一单多件商品时运费不会叠加，高价商品也可以单独设置包邮门槛。
     */
    public int calculateFreight(List<FreightLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return 0;
        }
        MarketingShippingRuleDO rule = requireCurrentRule();
        int maxFee = 0;
        for (FreightLine line : lines) {
            if (line == null) {
                continue;
            }
            maxFee = Math.max(maxFee, feeForGoods(rule, line));
        }
        return maxFee;
    }

    /** 购物车与结算页展示的包邮门槛，基础运费为 0 或未配置规则时不提示凑单。 */
    public int getFreeThreshold() {
        MarketingShippingRuleDO rule = getCurrentRule();
        if (rule == null || rule.getFreeThreshold() == null || rule.getBaseFee() == null
                || rule.getFreeThreshold() < 0 || rule.getBaseFee() < 0) {
            return 0;
        }
        return rule.getBaseFee() == 0 ? 0 : rule.getFreeThreshold();
    }

    /**
     * 全局运费规则优先级：在有效期内且启用的数据库规则优先，按生效时间和 ID 最新规则胜出。
     */
    public MarketingShippingRuleDO getCurrentRule() {
        LocalDateTime now = LocalDateTime.now();
        return shippingRuleMapper.selectOne(
                new LambdaQueryWrapper<MarketingShippingRuleDO>()
                        .eq(MarketingShippingRuleDO::getStatus, 1)
                        .and(wrapper -> wrapper.isNull(MarketingShippingRuleDO::getStartTime)
                                .or()
                                .le(MarketingShippingRuleDO::getStartTime, now))
                        .and(wrapper -> wrapper.isNull(MarketingShippingRuleDO::getEndTime)
                                .or()
                                .gt(MarketingShippingRuleDO::getEndTime, now))
                        .orderByDesc(MarketingShippingRuleDO::getStartTime)
                        .orderByDesc(MarketingShippingRuleDO::getId)
                        .last("LIMIT 1"));
    }

    /** 单品维度：商品设置了自己的运费或包邮门槛时优先使用，未设置的项仍跟随全局规则。 */
    private int feeForGoods(MarketingShippingRuleDO rule, FreightLine line) {
        Integer baseFee = rule.getBaseFee();
        Integer freeThreshold = rule.getFreeThreshold();
        SpuFreightRule spuRule = loadSpuFreightRule(line.spuId());
        if (spuRule != null) {
            if (spuRule.baseFee() != null) {
                baseFee = spuRule.baseFee();
            }
            if (spuRule.freeThreshold() != null) {
                freeThreshold = spuRule.freeThreshold();
            }
        }
        return line.amount() > 0 && line.amount() < freeThreshold ? baseFee : 0;
    }

    private SpuFreightRule loadSpuFreightRule(Long spuId) {
        if (spuId == null) {
            return null;
        }
        List<SpuFreightRule> rows = jdbcTemplate.query(
                "SELECT freight_base_fee, freight_free_threshold FROM product_spu WHERE id = ? AND deleted = b'0'",
                (rs, index) -> new SpuFreightRule(
                        rs.getObject("freight_base_fee", Integer.class),
                        rs.getObject("freight_free_threshold", Integer.class)),
                spuId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private MarketingShippingRuleDO requireCurrentRule() {
        MarketingShippingRuleDO rule = getCurrentRule();
        if (rule == null) {
            throw new ServerException(500, "运费规则未配置，请先在后台「营销 → 包邮规则」设置");
        }
        if (rule.getBaseFee() == null || rule.getFreeThreshold() == null
                || rule.getBaseFee() < 0 || rule.getFreeThreshold() < 0) {
            throw new ServerException(500, "运费规则配置不正确，请检查后台包邮规则");
        }
        return rule;
    }

    private int feeByRule(MarketingShippingRuleDO rule, int goodsTotalPrice) {
        if (rule.getBaseFee() == 0) {
            return 0;
        }
        return goodsTotalPrice > 0 && goodsTotalPrice < rule.getFreeThreshold() ? rule.getBaseFee() : 0;
    }

    /** 参与运费计算的商品行：spuId 与商品小计金额（分）。 */
    public record FreightLine(Long spuId, int amount) {
    }

    private record SpuFreightRule(Integer baseFee, Integer freeThreshold) {
    }
}
