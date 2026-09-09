package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 流程审批时间轴条目。 */
@Data
public class FlowHistoryItemVO {
    private String taskId;
    private String taskName;
    private String assigneeId;
    private String assigneeName;
    private String action;
    private String comment;
    private String signature;
    /** 动态审批要点字段，保留兼容历史 JSON 结构。 */
    private List<Map<String, Object>> approvalPointResults;
    private String createTime;
    private String completeTime;
}
