package com.mdframe.forge.plugin.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataDatasetRowScopeStrategyEnum {

    SELF("SELF", "仅本级"),
    SELF_AND_CHILDREN("SELF_AND_CHILDREN", "本级及直接下级"),
    SELF_AND_DESCENDANTS("SELF_AND_DESCENDANTS", "本级及所有下级");

    private final String code;

    private final String description;

    public static DataDatasetRowScopeStrategyEnum getByCode(String code) {
        if (code == null) {
            return SELF_AND_DESCENDANTS;
        }
        for (DataDatasetRowScopeStrategyEnum item : values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item;
            }
        }
        return SELF_AND_DESCENDANTS;
    }
}
