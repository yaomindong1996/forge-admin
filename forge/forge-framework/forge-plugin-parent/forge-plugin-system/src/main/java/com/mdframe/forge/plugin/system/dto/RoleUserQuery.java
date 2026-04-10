package com.mdframe.forge.plugin.system.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色用户查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleUserQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 用户真实姓名（模糊查询）
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态（0-禁用，1-正常，2-锁定）
     */
    private Integer userStatus;
}