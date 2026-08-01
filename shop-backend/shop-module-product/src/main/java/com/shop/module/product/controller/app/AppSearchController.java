package com.shop.module.product.controller.app;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app-api/search")
@RequiredArgsConstructor
public class AppSearchController {
    private final ProductSearchService searchService;

    @RequestMapping("/index")
    public CommonResult<Map<String, Object>> index() {
        return CommonResult.success(searchService.index());
    }

    @RequestMapping("/helper")
    public CommonResult<List<String>> helper(@RequestParam(defaultValue = "") String keyword) {
        return CommonResult.success(searchService.suggestions(keyword));
    }

    @RequestMapping("/clearhistory")
    public CommonResult<Map<String, Object>> clearHistory() {
        return CommonResult.success(searchService.clearHistory());
    }
}
