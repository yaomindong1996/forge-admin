package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 动态加签候选关系及审计信息。 */
@Data
public class FlowTaskSignRelationVO {

    private Long id;
    private String taskId;
    private String parentTaskId;
    private String childTaskId;
    private String processInstanceId;
    private String targetUserId;
    private String signMode;
    private String source;
    private Integer status;
    private String operatorId;
    private String reason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
