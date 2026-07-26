package com.mdframe.forge.plugin.generator.constant;

import java.util.List;

/**
 * 应用协调发布固定步骤。
 */
public final class BusinessApplicationPublishStep {

    public static final String PRECHECK = "PRECHECK";
    public static final String SNAPSHOT = "SNAPSHOT";
    public static final String OBJECTS = "OBJECTS";
    public static final String ENTRIES = "ENTRIES";
    public static final String PAGE_MENUS = "PAGE_MENUS";
    public static final String EXTENSIONS = "EXTENSIONS";
    public static final String COMMIT = "COMMIT";

    public static final List<String> ORDERED_STEPS = List.of(
            PRECHECK, SNAPSHOT, OBJECTS, ENTRIES, PAGE_MENUS, EXTENSIONS, COMMIT
    );

    private BusinessApplicationPublishStep() {
    }
}
