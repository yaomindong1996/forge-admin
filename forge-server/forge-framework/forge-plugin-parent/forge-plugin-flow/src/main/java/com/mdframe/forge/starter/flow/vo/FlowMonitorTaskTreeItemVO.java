package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员任务树中的任务节点。 */
@Data
public class FlowMonitorTaskTreeItemVO {

    private String key;
    private String label;
    private String taskId;
    private Integer status;
    private Boolean active;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
