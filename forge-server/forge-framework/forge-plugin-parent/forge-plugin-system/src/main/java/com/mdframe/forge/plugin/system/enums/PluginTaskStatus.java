package com.mdframe.forge.plugin.system.enums;

public enum PluginTaskStatus {
    AWAIT_CONFIRMATION("await_confirmation"), BLOCKED("blocked"), QUEUED("queued"), CANCELLED("cancelled");
    private final String code;

    PluginTaskStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }

    public boolean cancellable() {
        return this == AWAIT_CONFIRMATION || this == QUEUED;
    }
}
