package com.mdframe.forge.plugin.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum DataDatasetAccessLevelEnum {

    VIEW("VIEW", "查看"),
    QUERY("QUERY", "查询"),
    MANAGE("MANAGE", "管理");

    private final String code;

    private final String description;

    public static DataDatasetAccessLevelEnum getByCode(String code) {
        if (code == null) {
            return VIEW;
        }
        for (DataDatasetAccessLevelEnum item : values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item;
            }
        }
        return VIEW;
    }

    public static List<String> allowedLevels(DataDatasetAccessLevelEnum requiredLevel) {
        DataDatasetAccessLevelEnum normalized = requiredLevel != null ? requiredLevel : VIEW;
        return switch (normalized) {
            case VIEW -> List.of(VIEW.getCode(), QUERY.getCode(), MANAGE.getCode());
            case QUERY -> List.of(QUERY.getCode(), MANAGE.getCode());
            case MANAGE -> List.of(MANAGE.getCode());
        };
    }
}
