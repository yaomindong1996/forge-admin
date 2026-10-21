package com.mdframe.forge.plugin.system.enums;

import java.util.Locale;

/**
 * 租户已安装内容资产类型（可扩展）。
 */
public enum ContentAssetKind {

    PRINT("print"),
    FORM("form"),
    PRINT_COMPONENT("print-component");

    private final String code;

    ContentAssetKind(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equalsIgnoreCase(String.valueOf(value == null ? "" : value).trim());
    }

    public static ContentAssetKind fromCode(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim().toLowerCase(Locale.ROOT);
        for (ContentAssetKind kind : values()) {
            if (kind.code.equals(normalized)) {
                return kind;
            }
        }
        return null;
    }

    public static ContentAssetKind require(String value) {
        ContentAssetKind kind = fromCode(value);
        if (kind == null) {
            throw new com.mdframe.forge.starter.core.exception.BusinessException("不支持的内容资产类型");
        }
        return kind;
    }
}
