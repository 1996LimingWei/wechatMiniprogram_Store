package com.shop.module.product.controller.app;

import com.shop.common.pojo.CommonResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 通用只读支持接口（帮助/FAQ 等）。优惠券接口已迁移至 AppCouponController。 */
@RestController
public class AppSupportController {

    @RequestMapping("/app-api/helpissue/typeList")
    public CommonResult<Map<String, Object>> helpTypeList() {
        return CommonResult.success(Map.of("list", List.of(Map.of("id", 1, "name", "商品相关"), Map.of("id", 2, "name", "订单相关"), Map.of("id", 3, "name", "配送相关"))));
    }

    @RequestMapping("/app-api/helpissue/issueList")
    public CommonResult<Map<String, Object>> helpIssueList() {
        return CommonResult.success(Map.of("list", List.of(Map.of("id", 1, "question", "如何退货？", "answer", "在订单详情页点击申请退货即可"), Map.of("id", 2, "question", "发货时间？", "answer", "一般下单后48小时内发货"))));
    }
}
