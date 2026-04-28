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
    TENANT_ALL(6, "租户全部数据权限"),

    /**
     * 本行政区划数据权限
     * 按用户所属行政区划（regionCode）过滤：
     * - 省级(level=1)：不限制（等同 ALL）
     * - 市级及以下：匹配本级编码 + 其直接下级区划（子查询 sys_region_code.parent_code）
     * 支持可选的用户表 area_code 联合 OR 条件
     */
    REGION(7, "本行政区划数据权限");
    
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
