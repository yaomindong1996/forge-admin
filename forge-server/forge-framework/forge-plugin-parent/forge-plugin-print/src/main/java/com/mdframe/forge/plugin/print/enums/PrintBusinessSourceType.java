package com.mdframe.forge.plugin.print.enums;

/**
 * 独立打印来源类型。API 为协议保留值，启用前必须接入受管连接实现。
 */
public enum PrintBusinessSourceType {

    SERVICE,
    DATASET,
    API;

    public String getCode() {
        return name();
    }

    public boolean matches(String value) {
        return getCode().equals(value);
    }
}
