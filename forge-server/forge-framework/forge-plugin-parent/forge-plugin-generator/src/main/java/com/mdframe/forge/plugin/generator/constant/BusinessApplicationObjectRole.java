package com.mdframe.forge.plugin.generator.constant;

import java.util.Set;

/**
 * 应用内业务对象角色。
 */
public final class BusinessApplicationObjectRole {

    public static final String PRIMARY = "PRIMARY";
    public static final String DETAIL = "DETAIL";
    public static final String REFERENCE = "REFERENCE";
    public static final String SHARED = "SHARED";

    private static final Set<String> SUPPORTED_ROLES = Set.of(PRIMARY, DETAIL, REFERENCE, SHARED);

    private BusinessApplicationObjectRole() {
    }

    public static Set<String> supportedRoles() {
        return SUPPORTED_ROLES;
    }
}
