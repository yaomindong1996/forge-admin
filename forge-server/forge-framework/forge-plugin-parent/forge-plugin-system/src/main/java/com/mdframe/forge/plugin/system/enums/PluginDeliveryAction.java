package com.mdframe.forge.plugin.system.enums;
import java.util.Arrays;
public enum PluginDeliveryAction {
    PUBLISH("publish"), DEPLOY("deploy"), RESTORE("restore");
    private final String code;
    PluginDeliveryAction(String code) { this.code = code; }
    public String getCode() { return code; }
    public boolean matches(String value) { return code.equals(value); }
    public static PluginDeliveryAction parse(String value) {
        return Arrays.stream(values()).filter(action -> action.matches(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("交付动作无效"));
    }
}
