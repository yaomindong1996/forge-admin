package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.List;

/** 分页流程审批时间轴。 */
@Data
public class FlowHistoryPageVO {
    private long pageNum;
    private long pageSize;
    private long total;
    private boolean hasMore;
    private List<FlowHistoryItemVO> records;
}
