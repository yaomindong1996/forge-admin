package com.mdframe.forge.starter.flow.vo;

import com.mdframe.forge.starter.flow.entity.FlowForm;
import lombok.Data;

import java.util.List;

/** 流程表单定义分页响应。 */
@Data
public class FlowFormPageVO {
    private List<FlowForm> records;
    private long total;
    private long page;
    private long pageNum;
    private long pageSize;
}
