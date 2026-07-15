package com.mdframe.forge.plugin.generator.constant;

import java.util.Set;

/**
 * 首期受治理扩展类型。
 */
public final class BusinessExtensionType {

    public static final String VISUAL_RULE = "VISUAL_RULE";
    public static final String CLIENT_JS = "CLIENT_JS";
    public static final String SCOPED_CSS = "SCOPED_CSS";
    public static final String SERVER_BINDING = "SERVER_BINDING";

    public static final Set<String> ENABLED_TYPES = Set.of(VISUAL_RULE, CLIENT_JS, SCOPED_CSS, SERVER_BINDING);

    private BusinessExtensionType() {
    }
}
