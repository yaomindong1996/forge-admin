package com.mdframe.forge.plugin.system.enums;

/** 实际使用的 Gate 来源，不根据配置开关猜测是否已授权。 */
public enum RuntimeLicenseMode {
    COMMUNITY("community"), LICENSE("license"), CUSTOM("custom"), UNAVAILABLE("unavailable");

    private final String code;

    RuntimeLicenseMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }
}
