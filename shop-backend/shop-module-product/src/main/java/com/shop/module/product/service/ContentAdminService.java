package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.product.dal.dataobject.*;
import com.shop.module.product.dal.mysql.*;
import com.shop.common.exception.ServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;

/**
 * 内容管理（Banner / 频道 / 品牌 / 专题）CRUD 服务
 */
@Service
@RequiredArgsConstructor
public class ContentAdminService {

    private final ContentBannerMapper bannerMapper;
    private final ContentChannelMapper channelMapper;
    private final ContentBrandMapper brandMapper;
    private final ContentTopicMapper topicMapper;
    private final ContentTopicProductMapper topicProductMapper;
    private final ProductSpuMapper productSpuMapper;

    // ==================== Banner ====================

    public List<ContentBannerDO> bannerList() {
        return bannerMapper.selectList(new LambdaQueryWrapper<ContentBannerDO>()
                .orderByDesc(ContentBannerDO::getSort)
                .orderByAsc(ContentBannerDO::getId));
    }

    public void createBanner(ContentBannerDO banner) {
        validateBanner(banner, null);
        bannerMapper.insert(banner);
    }

    public void updateBanner(ContentBannerDO banner) {
        requireId(banner == null ? null : banner.getId(), bannerMapper.selectById(banner == null ? null : banner.getId()));
        validateBanner(banner, banner.getId());
        ensureUpdated(bannerMapper.updateById(banner));
    }

    public void deleteBanner(Long id) {
        requireId(id, id == null ? null : bannerMapper.selectById(id));
        bannerMapper.deleteById(id);
    }

    // ==================== 频道 ====================

    public List<ContentChannelDO> channelList() {
        return channelMapper.selectList(new LambdaQueryWrapper<ContentChannelDO>()
                .orderByDesc(ContentChannelDO::getSort)
                .orderByAsc(ContentChannelDO::getId));
    }

    public void createChannel(ContentChannelDO channel) {
        validateChannel(channel, null);
        channelMapper.insert(channel);
    }

    public void updateChannel(ContentChannelDO channel) {
        requireId(channel == null ? null : channel.getId(), channelMapper.selectById(channel == null ? null : channel.getId()));
        validateChannel(channel, channel.getId());
        ensureUpdated(channelMapper.updateById(channel));
    }

    public void deleteChannel(Long id) {
        requireId(id, id == null ? null : channelMapper.selectById(id));
        channelMapper.deleteById(id);
    }

    // ==================== 品牌 ====================

    public List<ContentBrandDO> brandList() {
        return brandMapper.selectList(new LambdaQueryWrapper<ContentBrandDO>()
                .orderByDesc(ContentBrandDO::getSort)
                .orderByAsc(ContentBrandDO::getId));
    }

    public void createBrand(ContentBrandDO brand) {
        validateBrand(brand, null);
        brandMapper.insert(brand);
    }

    public void updateBrand(ContentBrandDO brand) {
        requireId(brand == null ? null : brand.getId(), brandMapper.selectById(brand == null ? null : brand.getId()));
        validateBrand(brand, brand.getId());
        ensureUpdated(brandMapper.updateById(brand));
    }

    public void deleteBrand(Long id) {
        requireId(id, id == null ? null : brandMapper.selectById(id));
        brandMapper.deleteById(id);
    }

    // ==================== 专题 ====================

    public List<ContentTopicDO> topicList() {
        return topicMapper.selectList(new LambdaQueryWrapper<ContentTopicDO>()
                .orderByDesc(ContentTopicDO::getSort)
                .orderByAsc(ContentTopicDO::getId));
    }

    public void createTopic(ContentTopicDO topic) {
        validateTopic(topic, null);
        topicMapper.insert(topic);
    }

    public void updateTopic(ContentTopicDO topic) {
        requireId(topic == null ? null : topic.getId(), topicMapper.selectById(topic == null ? null : topic.getId()));
        validateTopic(topic, topic.getId());
        ensureUpdated(topicMapper.updateById(topic));
    }

    public void deleteTopic(Long id) {
        requireId(id, id == null ? null : topicMapper.selectById(id));
        topicMapper.deleteById(id);
        // 同时删除关联商品关系
        topicProductMapper.delete(new LambdaQueryWrapper<ContentTopicProductDO>()
                .eq(ContentTopicProductDO::getTopicId, id));
    }

    // ==================== 专题关联商品 ====================

    /** 获取专题关联的商品 ID 列表 */
    public List<Long> getTopicProductIds(Long topicId) {
        return topicProductMapper.selectList(
                new LambdaQueryWrapper<ContentTopicProductDO>()
                        .eq(ContentTopicProductDO::getTopicId, topicId)
                        .orderByDesc(ContentTopicProductDO::getSort))
                .stream()
                .map(ContentTopicProductDO::getSpuId)
                .toList();
    }

    /** 设置专题关联商品（全量替换） */
    @Transactional
    public void setTopicProducts(Long topicId, List<Long> spuIds) {
        requireId(topicId, topicId == null ? null : topicMapper.selectById(topicId));
        List<Long> normalizedIds = spuIds == null ? List.of() : new LinkedHashSet<>(spuIds).stream().toList();
        if (normalizedIds.size() > 100) {
            throw new ServerException(400, "单个专题最多关联 100 个商品");
        }
        for (Long spuId : normalizedIds) {
            ProductSpuDO spu = spuId == null ? null : productSpuMapper.selectById(spuId);
            if (spu == null) throw new ServerException(400, "专题关联商品不存在: " + spuId);
        }
        // 先删除旧的关联
        topicProductMapper.delete(new LambdaQueryWrapper<ContentTopicProductDO>()
                .eq(ContentTopicProductDO::getTopicId, topicId));
        // 再插入新的关联
        if (!normalizedIds.isEmpty()) {
            for (int i = 0; i < normalizedIds.size(); i++) {
                ContentTopicProductDO assoc = new ContentTopicProductDO();
                assoc.setTopicId(topicId);
                assoc.setSpuId(normalizedIds.get(i));
                assoc.setSort(normalizedIds.size() - i); // 排在前面的权重更高
                topicProductMapper.insert(assoc);
            }
        }
    }

    private void validateBanner(ContentBannerDO value, Long currentId) {
        if (value == null) throw new ServerException(400, "Banner 信息不能为空");
        value.setTitle(requireText(value.getTitle(), "Banner 标题", 128));
        value.setPicUrl(requireResourceUrl(value.getPicUrl(), "Banner 图片"));
        value.setUrl(requireLink(value.getUrl()));
        validateCommon(value.getStatus(), value.getSort());
        LambdaQueryWrapper<ContentBannerDO> duplicate = new LambdaQueryWrapper<ContentBannerDO>()
                .eq(ContentBannerDO::getUrl, value.getUrl());
        if (currentId != null) duplicate.ne(ContentBannerDO::getId, currentId);
        if (bannerMapper.selectCount(duplicate) > 0) throw new ServerException(400, "Banner 跳转地址不能重复");
    }

    private void validateChannel(ContentChannelDO value, Long currentId) {
        if (value == null) throw new ServerException(400, "频道信息不能为空");
        value.setName(requireText(value.getName(), "频道名称", 64));
        value.setIconUrl(requireResourceUrl(value.getIconUrl(), "频道图标"));
        value.setUrl(requireLink(value.getUrl()));
        validateCommon(value.getStatus(), value.getSort());
        LambdaQueryWrapper<ContentChannelDO> duplicate = new LambdaQueryWrapper<ContentChannelDO>()
                .and(wrapper -> wrapper.eq(ContentChannelDO::getName, value.getName())
                        .or().eq(ContentChannelDO::getUrl, value.getUrl()));
        if (currentId != null) duplicate.ne(ContentChannelDO::getId, currentId);
        if (channelMapper.selectCount(duplicate) > 0) throw new ServerException(400, "频道名称或跳转地址不能重复");
    }

    private void validateBrand(ContentBrandDO value, Long currentId) {
        if (value == null) throw new ServerException(400, "品牌信息不能为空");
        value.setName(requireText(value.getName(), "品牌名称", 64));
        if (value.getPicUrl() != null && !value.getPicUrl().isBlank()) {
            value.setPicUrl(requireResourceUrl(value.getPicUrl(), "品牌图片"));
        }
        if (value.getFloorPrice() != null && value.getFloorPrice() < 0) throw new ServerException(400, "品牌起售价不能为负数");
        validateCommon(value.getStatus(), value.getSort());
        LambdaQueryWrapper<ContentBrandDO> duplicate = new LambdaQueryWrapper<ContentBrandDO>().eq(ContentBrandDO::getName, value.getName());
        if (currentId != null) duplicate.ne(ContentBrandDO::getId, currentId);
        if (brandMapper.selectCount(duplicate) > 0) throw new ServerException(400, "品牌名称不能重复");
    }

    private void validateTopic(ContentTopicDO value, Long currentId) {
        if (value == null) throw new ServerException(400, "专题信息不能为空");
        value.setTitle(requireText(value.getTitle(), "专题标题", 128));
        if (value.getSubtitle() != null && value.getSubtitle().length() > 255) throw new ServerException(400, "专题副标题过长");
        if (value.getPicUrl() != null && !value.getPicUrl().isBlank()) value.setPicUrl(requireResourceUrl(value.getPicUrl(), "专题图片"));
        validateCommon(value.getStatus(), value.getSort());
        LambdaQueryWrapper<ContentTopicDO> duplicate = new LambdaQueryWrapper<ContentTopicDO>().eq(ContentTopicDO::getTitle, value.getTitle());
        if (currentId != null) duplicate.ne(ContentTopicDO::getId, currentId);
        if (topicMapper.selectCount(duplicate) > 0) throw new ServerException(400, "专题标题不能重复");
    }

    private void validateCommon(Integer status, Integer sort) {
        if (status == null || (status != 0 && status != 1)) throw new ServerException(400, "内容状态不正确");
        if (sort == null || sort < -100000 || sort > 100000) throw new ServerException(400, "排序值应在 -100000 至 100000 之间");
    }

    private String requireText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) throw new ServerException(400, label + "长度不正确");
        return normalized;
    }

    private String requireResourceUrl(String value, String label) {
        String normalized = requireText(value, label, 512);
        if (!normalized.startsWith("https://") && !normalized.startsWith("/static/")) {
            throw new ServerException(400, label + "必须使用 HTTPS 或小程序静态资源路径");
        }
        return normalized;
    }

    private String requireLink(String value) {
        String normalized = requireText(value, "跳转地址", 255);
        if (!normalized.startsWith("/pages/") && !normalized.startsWith("https://")) {
            throw new ServerException(400, "跳转地址必须是小程序页面路径或 HTTPS 地址");
        }
        return normalized;
    }

    private void requireId(Long id, Object value) {
        if (id == null || id <= 0 || value == null) throw new ServerException(404, "运营内容不存在");
    }

    private void ensureUpdated(int updated) {
        if (updated != 1) throw new ServerException(409, "运营内容已变化，请刷新后重试");
    }
}
