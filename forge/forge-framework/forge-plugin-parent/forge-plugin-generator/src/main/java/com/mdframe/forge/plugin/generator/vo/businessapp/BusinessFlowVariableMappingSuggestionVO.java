package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 单据字段到流程变量的自动映射建议。
 */
@Data
public class BusinessFlowVariableMappingSuggestionVO {

    private String formField;

    private String fieldLabel;

    private String flowVariable;

    private String variableDisplayName;

    private Integer confidence;

    private String reason;
}
