package com.mdframe.forge.plugin.generator.constant;

import java.util.Set;

/**
 * 业务对象和页面扩展钩子。
 */
public final class BusinessExtensionHook {

    public static final Set<String> CLIENT_PAGE = Set.of(
            "PAGE_INIT", "FORM_CHANGE", "BEFORE_SUBMIT", "AFTER_SUBMIT", "ROW_ACTION"
    );

    public static final Set<String> SCOPED_STYLE = Set.of("PAGE_INIT");

    public static final Set<String> ALL = Set.of(
            "BEFORE_CREATE", "AFTER_CREATE", "BEFORE_UPDATE", "AFTER_UPDATE",
            "BEFORE_DELETE", "AFTER_DELETE", "BEFORE_IMPORT", "AFTER_IMPORT",
            "BEFORE_EXPORT", "AFTER_EXPORT", "BEFORE_LIST", "AFTER_LIST",
            "BEFORE_DETAIL", "AFTER_DETAIL", "BEFORE_SUMMARY", "AFTER_SUMMARY",
            "PAGE_INIT", "FORM_CHANGE", "BEFORE_SUBMIT", "AFTER_SUBMIT", "ROW_ACTION"
    );

    public static Set<String> allowedForType(String extensionType) {
        if (BusinessExtensionType.CLIENT_JS.equals(extensionType)) {
            return CLIENT_PAGE;
        }
        if (BusinessExtensionType.SCOPED_CSS.equals(extensionType)) {
            return SCOPED_STYLE;
        }
        return ALL;
    }

    private BusinessExtensionHook() {
    }
}
