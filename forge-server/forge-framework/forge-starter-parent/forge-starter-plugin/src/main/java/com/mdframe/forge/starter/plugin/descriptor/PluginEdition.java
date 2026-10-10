package com.mdframe.forge.starter.plugin.descriptor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** 描述协议的发行类型，不与租户启用状态或维保状态混用。 */
public enum PluginEdition {
    COMMUNITY("community"),
    ENTERPRISE("ee");

    private final String code;

    PluginEdition(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static PluginEdition fromCode(String code) {
        for (PluginEdition edition : values()) {
            if (edition.code.equals(code)) {
                return edition;
            }
        }
        throw new IllegalArgumentException("非法插件发行类型：" + code);
    }
}
