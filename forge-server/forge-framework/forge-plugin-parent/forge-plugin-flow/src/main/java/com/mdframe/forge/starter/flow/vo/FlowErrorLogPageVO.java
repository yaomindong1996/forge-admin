package com.mdframe.forge.starter.flow.vo;

import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import lombok.Data;

import java.util.List;

/** 流程错误日志分页响应。 */
@Data
public class FlowErrorLogPageVO {
    private List<FlowErrorLog> list;
    private long total;
    private long pageNum;
    private long pageSize;
    private boolean degraded;
    private String errorCode;
}
