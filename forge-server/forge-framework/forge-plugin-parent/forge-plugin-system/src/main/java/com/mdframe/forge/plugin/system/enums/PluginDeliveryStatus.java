package com.mdframe.forge.plugin.system.enums;
public enum PluginDeliveryStatus {
    QUEUED("queued"), RUNNING("running"), SUCCEEDED("succeeded"), FAILED("failed"),
    UNCERTAIN("uncertain"), RECONCILED("reconciled");
    private final String code;
    PluginDeliveryStatus(String code) { this.code = code; }
    public String getCode() { return code; }
    public boolean matches(String value) { return code.equals(value); }
}
