package com.shop.module.product.controller.admin;

import com.shop.common.pojo.CommonResult;
import com.shop.module.product.dal.dataobject.*;
import com.shop.module.product.service.ContentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 — 内容管理（Banner / 频道 / 品牌 / 专题）
 */
@RestController
@RequestMapping("/admin-api/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentAdminService contentAdminService;

    // ==================== Banner ====================

    @GetMapping("/banner/list")
    public CommonResult<List<ContentBannerDO>> bannerList() {
        return CommonResult.success(contentAdminService.bannerList());
    }

    @PostMapping("/banner/create")
    public CommonResult<Boolean> createBanner(@RequestBody ContentBannerDO banner) {
        contentAdminService.createBanner(banner);
        return CommonResult.success(true);
    }

    @PutMapping("/banner/update")
    public CommonResult<Boolean> updateBanner(@RequestBody ContentBannerDO banner) {
        contentAdminService.updateBanner(banner);
        return CommonResult.success(true);
    }

    @DeleteMapping("/banner/delete")
    public CommonResult<Boolean> deleteBanner(@RequestParam Long id) {
        contentAdminService.deleteBanner(id);
        return CommonResult.success(true);
    }

    // ==================== 频道 ====================

    @GetMapping("/channel/list")
    public CommonResult<List<ContentChannelDO>> channelList() {
        return CommonResult.success(contentAdminService.channelList());
    }

    @PostMapping("/channel/create")
    public CommonResult<Boolean> createChannel(@RequestBody ContentChannelDO channel) {
        contentAdminService.createChannel(channel);
        return CommonResult.success(true);
    }

    @PutMapping("/channel/update")
    public CommonResult<Boolean> updateChannel(@RequestBody ContentChannelDO channel) {
        contentAdminService.updateChannel(channel);
        return CommonResult.success(true);
    }

    @DeleteMapping("/channel/delete")
    public CommonResult<Boolean> deleteChannel(@RequestParam Long id) {
        contentAdminService.deleteChannel(id);
        return CommonResult.success(true);
    }

    // ==================== 品牌 ====================

    @GetMapping("/brand/list")
    public CommonResult<List<ContentBrandDO>> brandList() {
        return CommonResult.success(contentAdminService.brandList());
    }

    @PostMapping("/brand/create")
    public CommonResult<Boolean> createBrand(@RequestBody ContentBrandDO brand) {
        contentAdminService.createBrand(brand);
        return CommonResult.success(true);
    }

    @PutMapping("/brand/update")
    public CommonResult<Boolean> updateBrand(@RequestBody ContentBrandDO brand) {
        contentAdminService.updateBrand(brand);
        return CommonResult.success(true);
    }

    @DeleteMapping("/brand/delete")
    public CommonResult<Boolean> deleteBrand(@RequestParam Long id) {
        contentAdminService.deleteBrand(id);
        return CommonResult.success(true);
    }

    // ==================== 专题 ====================

    @GetMapping("/topic/list")
    public CommonResult<List<ContentTopicDO>> topicList() {
        return CommonResult.success(contentAdminService.topicList());
    }

    @PostMapping("/topic/create")
    public CommonResult<Boolean> createTopic(@RequestBody ContentTopicDO topic) {
        contentAdminService.createTopic(topic);
        return CommonResult.success(true);
    }

    @PutMapping("/topic/update")
    public CommonResult<Boolean> updateTopic(@RequestBody ContentTopicDO topic) {
        contentAdminService.updateTopic(topic);
        return CommonResult.success(true);
    }

    @DeleteMapping("/topic/delete")
    public CommonResult<Boolean> deleteTopic(@RequestParam Long id) {
        contentAdminService.deleteTopic(id);
        return CommonResult.success(true);
    }

    // ==================== 专题关联商品 ====================

    @GetMapping("/topic/products")
    public CommonResult<List<Long>> getTopicProducts(@RequestParam Long topicId) {
        return CommonResult.success(contentAdminService.getTopicProductIds(topicId));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/topic/products")
    public CommonResult<Boolean> setTopicProducts(@RequestBody Map<String, Object> body) {
        Long topicId = ((Number) body.get("topicId")).longValue();
        List<Long> spuIds = ((List<Number>) body.get("spuIds")).stream()
                .map(Number::longValue)
                .toList();
        contentAdminService.setTopicProducts(topicId, spuIds);
        return CommonResult.success(true);
    }
}
