package com.mdframe.forge.plugin.data.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataDatasetRowScopeTypeEnum {

    ALL(1, "全部数据"),
    TENANT_ALL(2, "本租户数据"),
    ORG(3, "本组织数据"),
    ORG_AND_CHILD(4, "本组织及子组织数据"),
    SELF(5, "本人数据"),
    TENANT_ALL_LEGACY(6, "租户全部数据"),
    REGION(7, "本行政区划数据");

    private final Integer code;

    private final String description;

    public static DataDatasetRowScopeTypeEnum getByCode(Integer code) {
        if (code == null) {
            return SELF;
        }
        for (DataDatasetRowScopeTypeEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return SELF;
    }
}
