package com.shop.module.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.product.dal.dataobject.*;
import com.shop.module.product.dal.mysql.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // ==================== Banner ====================

    public List<ContentBannerDO> bannerList() {
        return bannerMapper.selectList(new LambdaQueryWrapper<ContentBannerDO>()
                .orderByDesc(ContentBannerDO::getSort)
                .orderByAsc(ContentBannerDO::getId));
    }

    public void createBanner(ContentBannerDO banner) {
        bannerMapper.insert(banner);
    }

    public void updateBanner(ContentBannerDO banner) {
        bannerMapper.updateById(banner);
    }

    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
    }

    // ==================== 频道 ====================

    public List<ContentChannelDO> channelList() {
        return channelMapper.selectList(new LambdaQueryWrapper<ContentChannelDO>()
                .orderByDesc(ContentChannelDO::getSort)
                .orderByAsc(ContentChannelDO::getId));
    }

    public void createChannel(ContentChannelDO channel) {
        channelMapper.insert(channel);
    }

    public void updateChannel(ContentChannelDO channel) {
        channelMapper.updateById(channel);
    }

    public void deleteChannel(Long id) {
        channelMapper.deleteById(id);
    }

    // ==================== 品牌 ====================

    public List<ContentBrandDO> brandList() {
        return brandMapper.selectList(new LambdaQueryWrapper<ContentBrandDO>()
                .orderByDesc(ContentBrandDO::getSort)
                .orderByAsc(ContentBrandDO::getId));
    }

    public void createBrand(ContentBrandDO brand) {
        brandMapper.insert(brand);
    }

    public void updateBrand(ContentBrandDO brand) {
        brandMapper.updateById(brand);
    }

    public void deleteBrand(Long id) {
        brandMapper.deleteById(id);
    }

    // ==================== 专题 ====================

    public List<ContentTopicDO> topicList() {
        return topicMapper.selectList(new LambdaQueryWrapper<ContentTopicDO>()
                .orderByDesc(ContentTopicDO::getSort)
                .orderByAsc(ContentTopicDO::getId));
    }

    public void createTopic(ContentTopicDO topic) {
        topicMapper.insert(topic);
    }

    public void updateTopic(ContentTopicDO topic) {
        topicMapper.updateById(topic);
    }

    public void deleteTopic(Long id) {
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
        // 先删除旧的关联
        topicProductMapper.delete(new LambdaQueryWrapper<ContentTopicProductDO>()
                .eq(ContentTopicProductDO::getTopicId, topicId));
        // 再插入新的关联
        if (spuIds != null) {
            for (int i = 0; i < spuIds.size(); i++) {
                ContentTopicProductDO assoc = new ContentTopicProductDO();
                assoc.setTopicId(topicId);
                assoc.setSpuId(spuIds.get(i));
                assoc.setSort(spuIds.size() - i); // 排在前面的权重更高
                topicProductMapper.insert(assoc);
            }
        }
    }
}
