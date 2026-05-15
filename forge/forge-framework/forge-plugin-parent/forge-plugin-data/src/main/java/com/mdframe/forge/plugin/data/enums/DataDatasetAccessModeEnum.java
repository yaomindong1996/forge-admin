package com.mdframe.forge.plugin.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataDatasetAccessModeEnum {

    PUBLIC("PUBLIC", "公开"),
    PRIVATE("PRIVATE", "私有");

    private final String code;

    private final String description;

    public static String normalize(String accessMode) {
        if (PRIVATE.getCode().equalsIgnoreCase(accessMode)) {
            return PRIVATE.getCode();
        }
        return PUBLIC.getCode();
    }

    public static boolean isPrivate(String accessMode) {
        return PRIVATE.getCode().equalsIgnoreCase(accessMode);
    }
}
