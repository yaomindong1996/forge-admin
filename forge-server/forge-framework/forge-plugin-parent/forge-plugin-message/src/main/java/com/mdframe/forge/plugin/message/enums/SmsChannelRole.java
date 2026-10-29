package com.mdframe.forge.plugin.message.enums;

/**
 * 短信通道角色。
 */
public enum SmsChannelRole {
    PRIMARY,
    SECONDARY;

    public String getCode() {
        return name();
    }

    public boolean matches(String value) {
        return name().equalsIgnoreCase(String.valueOf(value == null ? "" : value).trim());
    }

    public static SmsChannelRole from(String value) {
        if (PRIMARY.matches(value)) {
            return PRIMARY;
        }
        return SECONDARY;
    }
}
