package com.shop.module.product.controller.app;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.service.HomeContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 商品内容正式接口。 */
@RestController
@RequiredArgsConstructor
public class AppContentController {
    private final HomeContentQueryService service;

    @RequestMapping("/app-api/goods/hot") public CommonResult<Map<String, Object>> goodsHot() { return CommonResult.success(service.goodsHot()); }
    @RequestMapping("/app-api/goods/new") public CommonResult<Map<String, Object>> goodsNew() { return CommonResult.success(service.goodsNew()); }
    @RequestMapping("/app-api/brand/list") public CommonResult<Map<String, Object>> brandList() { return CommonResult.success(service.brandList()); }
    @RequestMapping("/app-api/brand/detail") public CommonResult<Map<String, Object>> brandDetail(@RequestParam(defaultValue = "1") Long id) { return CommonResult.success(service.brandDetail(id)); }
    @RequestMapping("/app-api/topic/list") public CommonResult<Map<String, Object>> topicList(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) { return CommonResult.success(service.topicList(page, size)); }
    @RequestMapping("/app-api/topic/detail") public CommonResult<Map<String, Object>> topicDetail(@RequestParam(defaultValue = "1") Long id) { return CommonResult.success(service.topicDetail(id)); }
    @RequestMapping("/app-api/topic/related") public CommonResult<List<Map<String, Object>>> topicRelated(@RequestParam(defaultValue = "1") Long id) { return CommonResult.success(service.topicRelated(id)); }
}
