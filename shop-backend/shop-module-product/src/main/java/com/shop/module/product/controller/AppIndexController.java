package com.shop.module.product.controller;

import com.shop.module.product.service.HomeContentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 首页内容接口。 */
@RestController
@RequestMapping("/app-api/index")
@RequiredArgsConstructor
public class AppIndexController {
    private final HomeContentQueryService homeContentQueryService;

    @RequestMapping("/banner")
    public Map<String, Object> banner() { return ok(homeContentQueryService.banner()); }

    @RequestMapping("/channel")
    public Map<String, Object> channel() { return ok(homeContentQueryService.channel()); }

    @RequestMapping("/brand")
    public Map<String, Object> brand() { return ok(homeContentQueryService.brand()); }

    @RequestMapping("/topic")
    public Map<String, Object> topic() { return ok(homeContentQueryService.topic()); }

    @RequestMapping("/newGoods")
    public Map<String, Object> newGoods() { return ok(homeContentQueryService.newGoods()); }

    @RequestMapping("/hotGoods")
    public Map<String, Object> hotGoods() { return ok(homeContentQueryService.hotGoods()); }

    @RequestMapping("/category")
    public Map<String, Object> category() { return ok(homeContentQueryService.category()); }

    private Map<String, Object> ok(Map<String, Object> data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 0);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }
}
