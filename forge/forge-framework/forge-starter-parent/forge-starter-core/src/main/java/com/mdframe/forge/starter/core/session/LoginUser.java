package com.mdframe.forge.starter.core.session;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 登录用户信息
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 用户类型（0-系统管理员，1-租户管理员，2-普通用户）
     */
    private Integer userType;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户状态（0-禁用，1-正常，2-锁定）
     */
    private Integer userStatus;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 角色编码列表
     */
    private Set<String> roleKeys;

    /**
     * 权限标识列表（按钮权限）
     */
    private Set<String> permissions;

    /**
     * API接口权限列表（用于接口权限校验，支持通配符）
     * 示例：["/system/**", "/system/user/add"]
     */
    private List<String> apiPermissions;

    /**
     * 组织ID列表
     */
    private List<Long> orgIds;

    /**
     * 主组织ID
     */
    private Long mainOrgId;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 登录IP
     */
    private String loginIp;
    
    private String deptName;

    /**
     * 是否为超级管理员
     */
    public boolean isAdmin() {
        return userType != null && userType == 0;
    }

    /**
     * 是否为租户管理员
     */
    public boolean isTenantAdmin() {
        return userType != null && userType == 1;
    }
}
