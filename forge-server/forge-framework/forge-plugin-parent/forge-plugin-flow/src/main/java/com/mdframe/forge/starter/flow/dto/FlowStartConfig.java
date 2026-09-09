package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程发起前需要由申请人补充的运行参数。
 */
@Data
public class FlowStartConfig {

    private String modelKey;

    private List<ApproverNode> initiatorSelectNodes = new ArrayList<>();

    /** 发起后按 BPMN 首条可达路径解析出的下一审批节点预览。 */
    private List<ApproverNode> nextNodes = new ArrayList<>();

    /** 发起前静态审批人预检结果；动态表达式仍需结合业务变量运行时解析。 */
    private Boolean preflightPassed = true;

    private List<String> diagnostics = new ArrayList<>();

    @Data
    public static class ApproverNode {
        private String nodeKey;
        private String nodeName;
        private Boolean multiple;

        /** BPMN 中保存的处理人策略，仅用于发起前预览，不代表运行时最终解析结果。 */
        private String assignee;

        private String candidateUsers;

        private String candidateGroups;
    }
}
