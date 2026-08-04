package com.shop.module.product.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_topic_product")
public class ContentTopicProductDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private Long spuId;
    private Integer sort;
    private LocalDateTime createTime;
}
