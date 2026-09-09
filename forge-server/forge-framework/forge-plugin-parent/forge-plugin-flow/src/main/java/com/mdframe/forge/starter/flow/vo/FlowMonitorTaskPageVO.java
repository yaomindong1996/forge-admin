package com.mdframe.forge.starter.flow.vo;

import com.mdframe.forge.starter.flow.entity.FlowTask;
import lombok.Data;

import java.util.List;

/** 管理员流程实例任务分页及树形摘要。 */
@Data
public class FlowMonitorTaskPageVO {

    private List<FlowTask> list;
    private long total;
    private long pageNum;
    private long pageSize;
    private List<FlowMonitorTaskTreeNodeVO> taskTree;
    private List<String> currentTaskIds;
    private boolean taskTreeTruncated;
    private boolean degraded;
    private String errorCode;
}
