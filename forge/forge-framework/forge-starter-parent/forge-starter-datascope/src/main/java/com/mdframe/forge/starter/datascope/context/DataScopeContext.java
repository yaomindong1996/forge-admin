package com.mdframe.forge.starter.datascope.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 数据权限上下文信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeContext {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户所属组织ID列表
     */
    private List<Long> orgIds;
    
    /**
     * 用户角色ID列表
     */
    private List<Long> roleIds;
    
    /**
     * 最小数据权限范围（取所有角色中权限范围最小的）
     */
    private Integer minDataScope;
    
    /**
     * 自定义数据权限组织ID集合
     */
    private Set<Long> customOrgIds;
    
    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户所属行政区划编码（对应 sys_region_code.code）
     */
    private String regionCode;

    /**
     * 行政级别（1-省/自治区, 2-市/盟, 3-区/县/旗, 4-街道）
     */
    private Integer regionLevel;

    /**
     * 行政区划祖级编码链（如："150000,150100,150102"）
     */
    private String regionAncestors;
}
