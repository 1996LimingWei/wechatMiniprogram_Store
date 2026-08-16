package com.shop.module.member.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shop.framework.mybatis.core.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 用户意见反馈。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_feedback")
public class MemberFeedbackDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private String content;
    private String mobile;
    /** 0=待处理，1=处理中，2=已完成。 */
    private Integer status;
    private Long handlerAdminId;
    private String handleRemark;
    private LocalDateTime handleTime;
}
