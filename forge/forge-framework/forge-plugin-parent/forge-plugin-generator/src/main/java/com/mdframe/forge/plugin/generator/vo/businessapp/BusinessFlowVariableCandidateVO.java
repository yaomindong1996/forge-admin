package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 流程变量候选项。
 */
@Data
public class BusinessFlowVariableCandidateVO {

    private String variableName;

    private String displayName;

    private String source;

    private String sourceLabel;

    private String dataType;

    private String expression;

    private String description;

    private Boolean builtIn;

    private Boolean required;
}
