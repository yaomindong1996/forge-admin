package com.mdframe.forge.starter.flow.enums;

import lombok.Getter;

/** 模型草稿与已发布快照的关系，对应 flow_model_publication_state 字典。 */
@Getter
public enum FlowModelPublicationState {
    UNPUBLISHED("UNPUBLISHED"),
    SYNCED("SYNCED"),
    CHANGED("CHANGED"),
    UNKNOWN("UNKNOWN");

    private final String code;

    FlowModelPublicationState(String code) {
        this.code = code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }
}
