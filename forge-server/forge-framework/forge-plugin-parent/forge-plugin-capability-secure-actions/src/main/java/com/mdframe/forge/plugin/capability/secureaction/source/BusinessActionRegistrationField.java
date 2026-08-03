package com.mdframe.forge.plugin.capability.secureaction.source;

/**
 * 业务动作能力可从发布模型快照中选择的受控字段。
 */
public record BusinessActionRegistrationField(
        String fieldCode,
        String fieldName,
        String columnName,
        String dataType,
        Integer length,
        Integer precision,
        Boolean required,
        String dictType,
        String fieldStatus,
        String remark) {
}
