package com.mdframe.forge.plugin.capability.flowaction.source;

public record FlowActionSubmissionField(
        String field,
        String label,
        String dataType,
        Integer length,
        Integer precision,
        boolean required,
        String dictType,
        String description,
        Object example) {
}
