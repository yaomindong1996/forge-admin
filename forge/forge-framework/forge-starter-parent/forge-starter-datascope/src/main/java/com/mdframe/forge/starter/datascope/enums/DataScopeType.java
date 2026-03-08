package com.mdframe.forge.starter.datascope.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限范围枚举
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {
    
    /**
     * 全部数据权限
     */
    ALL(1, "全部数据权限"),
    
    /**
     * 本人数据权限
     */
    SELF(2, "本人数据权限"),
    
    /**
     * 本组织数据权限
     */
    ORG(3, "本组织数据权限"),
    
    /**
     * 本组织及子组织数据权限
     */
    ORG_AND_CHILD(4, "本组织及子组织数据权限"),
    
    /**
     * 自定义数据权限
     */
    CUSTOM(5, "自定义数据权限"),
    
    /**
     * 租户全部数据权限
     */
    TENANT_ALL(6, "租户全部数据权限");
    
    private final Integer code;
    private final String description;
    
    /**
     * 根据code获取枚举
     */
    public static DataScopeType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScopeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
