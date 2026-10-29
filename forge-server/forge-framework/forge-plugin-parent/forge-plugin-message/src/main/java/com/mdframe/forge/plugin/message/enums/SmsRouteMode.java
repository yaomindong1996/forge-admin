package com.mdframe.forge.plugin.message.enums;

/**
 * 短信选路模式。
 */
public enum SmsRouteMode {
    WEIGHTED_RR,
    PRIMARY_ONLY;

    public String getCode() {
        return name();
    }

    public boolean matches(String value) {
        return name().equalsIgnoreCase(String.valueOf(value == null ? "" : value).trim());
    }

    public static SmsRouteMode from(String value) {
        if (PRIMARY_ONLY.matches(value)) {
            return PRIMARY_ONLY;
        }
        return WEIGHTED_RR;
    }
}
