package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 审批人计算请求；流程变量保留为动态字段，其余协议字段由 Controller 显式声明。 */
@Data
public class FlowApproverCalculationDTO {

    private Map<String, Object> variables = new LinkedHashMap<>();

    public Map<String, Object> resolveVariables() {
        return variables == null ? new LinkedHashMap<>() : variables;
    }
}
