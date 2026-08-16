package com.shop.module.member.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackRespVO {
    private Long id;
    private Integer type;
    private String typeName;
    private String content;
    private String mobile;
    private Integer status;
    private String statusName;
    private String userNickname;
    private Long handlerAdminId;
    private String handlerName;
    private String handleRemark;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
