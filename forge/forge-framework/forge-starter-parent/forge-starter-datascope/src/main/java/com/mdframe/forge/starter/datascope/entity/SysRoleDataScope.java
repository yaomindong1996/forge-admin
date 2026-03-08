package com.mdframe.forge.starter.datascope.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-自定义数据权限关联实体
 */
@Data
@TableName("sys_role_data_scope")
public class SysRoleDataScope implements Serializable {
    
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
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 组织ID
     */
    private Long orgId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
