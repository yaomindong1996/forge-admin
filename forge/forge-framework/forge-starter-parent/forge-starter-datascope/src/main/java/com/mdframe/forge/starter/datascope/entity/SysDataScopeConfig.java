package com.mdframe.forge.starter.datascope.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据权限配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_scope_config")
public class SysDataScopeConfig extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户编号
     */
    private Long tenantId;
    
    /**
     * 资源编码（对应接口路径或功能模块）
     */
    private String resourceCode;
    
    /**
     * 资源名称
     */
    private String resourceName;
    
    /**
     * Mapper方法（如：com.example.mapper.UserMapper.selectList）
     */
    private String mapperMethod;
    
    /**
     * 主表别名
     */
    private String tableAlias;
    
    /**
     * 用户ID字段名
     * 简单模式：直接填字段名，如 "user_id"、"create_by"
     * 复杂模式：以 <sql> 开头，支持占位符 #{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}
     * 示例：<sql>(lc.current_handler_id = #{userId} OR lc.register_person_id = #{userId})
     */
    private String userIdColumn;
    
    /**
     * 组织ID字段名
     * 简单模式：直接填字段名，如 "org_id"、"dept_id"
     * 复杂模式：以 <sql> 开头，支持占位符 #{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}
     * 示例：<sql>t.dept_id IN (#{orgIds})
     */
    private String orgIdColumn;
    
    /**
     * 租户ID字段名
     * 简单模式：直接填字段名，如 "tenant_id"
     * 复杂模式：以 <sql> 开头，支持占位符 #{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}
     * 示例：<sql>t.tenant_id = #{tenantId}
     */
    private String tenantIdColumn;
    
    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Integer enabled;
    
    /**
     * 备注
     */
    private String remark;
}
