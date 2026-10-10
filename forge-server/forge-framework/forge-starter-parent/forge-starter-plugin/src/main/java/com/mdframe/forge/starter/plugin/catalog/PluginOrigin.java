package com.mdframe.forge.starter.plugin.catalog;

import com.fasterxml.jackson.annotation.JsonValue;

/** 来源与发行版是不同维度，内置模块不允许通过源码插件卸载。 */
public enum PluginOrigin {
    BUILTIN("builtin"), EXTERNAL("external");

    private final String code;

    PluginOrigin(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
