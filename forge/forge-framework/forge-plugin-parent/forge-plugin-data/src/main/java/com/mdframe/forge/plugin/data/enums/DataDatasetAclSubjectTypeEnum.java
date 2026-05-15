package com.mdframe.forge.plugin.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataDatasetAclSubjectTypeEnum {

    USER("USER", "用户"),
    ROLE("ROLE", "角色"),
    ORG("ORG", "组织");

    private final String code;

    private final String description;

    public static boolean isValid(String subjectType) {
        if (subjectType == null) {
            return false;
        }
        for (DataDatasetAclSubjectTypeEnum item : values()) {
            if (item.getCode().equalsIgnoreCase(subjectType)) {
                return true;
            }
        }
        return false;
    }
}
