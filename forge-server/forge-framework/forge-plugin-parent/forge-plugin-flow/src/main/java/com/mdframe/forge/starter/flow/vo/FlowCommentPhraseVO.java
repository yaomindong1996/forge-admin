package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 常用审批意见响应。 */
@Data
public class FlowCommentPhraseVO {

    private Long id;

    private Integer ownerType;

    private Long userId;

    private String scene;

    private String content;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
