package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.List;

/** 管理员任务树中的审批节点。 */
@Data
public class FlowMonitorTaskTreeNodeVO {

    private String key;
    private String label;
    private String nodeKey;
    private List<FlowMonitorTaskTreeItemVO> children;
}
