package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.List;

/** 管理员流程实例监控分页结果。 */
@Data
public class FlowMonitorProcessInstancePageVO {

    private List<FlowMonitorProcessInstanceVO> list;
    private long total;
    private long pageNum;
    private long pageSize;
    /** 查询降级时为 true，避免前端把故障误显示为空列表。 */
    private boolean degraded;
    private String errorCode;
}
