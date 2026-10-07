package com.mdframe.forge.plugin.system.enums;

/** 加载状态不能冒充健康状态，也不能与源码安装任务状态混用。 */
public enum PluginLoadState {
    BACKEND_LOADED("backend_loaded"), METADATA_ONLY("metadata_only");

    private final String code;

    PluginLoadState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }
}
