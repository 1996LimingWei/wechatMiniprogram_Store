package com.shop.module.product.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialAssetRespVO {

    private Long id;
    private String url;
    private String objectKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String bizType;
    private Integer referenceCount;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
