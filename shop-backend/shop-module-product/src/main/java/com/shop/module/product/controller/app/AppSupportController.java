package com.shop.module.product.controller.app;

import com.shop.common.pojo.CommonResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 尚未持久化的通用只读支持接口。 */
@RestController
public class AppSupportController {
    @RequestMapping("/app-api/coupon/list")
    public CommonResult<List<Map<String, Object>>> couponList() {
        return CommonResult.success(List.of(
                Map.of("id", 1, "name", "新人专享券", "typeMoney", 10, "minGoodsAmount", 99, "useEndDate", "2026-12-31", "couponStatus", 1),
                Map.of("id", 2, "name", "满减优惠券", "typeMoney", 20, "minGoodsAmount", 199, "useEndDate", "2026-08-31", "couponStatus", 1),
                Map.of("id", 3, "name", "会员折扣券", "typeMoney", 50, "minGoodsAmount", 399, "useEndDate", "2026-06-01", "couponStatus", 3)));
    }

    @RequestMapping("/app-api/user/info")
    public CommonResult<Map<String, Object>> userInfo() {
        return CommonResult.success(Map.of("userInfo", Map.of("nickName", "测试用户", "avatarUrl", "", "mobile", "138****8888")));
    }

    @RequestMapping("/app-api/helpissue/typeList")
    public CommonResult<Map<String, Object>> helpTypeList() {
        return CommonResult.success(Map.of("list", List.of(Map.of("id", 1, "name", "商品相关"), Map.of("id", 2, "name", "订单相关"), Map.of("id", 3, "name", "配送相关"))));
    }

    @RequestMapping("/app-api/helpissue/issueList")
    public CommonResult<Map<String, Object>> helpIssueList() {
        return CommonResult.success(Map.of("list", List.of(Map.of("id", 1, "question", "如何退货？", "answer", "在订单详情页点击申请退货即可"), Map.of("id", 2, "question", "发货时间？", "answer", "一般下单后48小时内发货"))));
    }
}
