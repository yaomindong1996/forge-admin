package com.mdframe.forge.plugin.system.enums;

public enum PluginTaskOperation {
    INSTALL("install"), REPLACE("replace");
    private final String code;

    PluginTaskOperation(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
