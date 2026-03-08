package com.mdframe.forge.starter.auth.service;

import java.util.List;

/**
 * 权限服务接口
 */
public interface IPermissionService {

    /**
     * 获取当前用户的API权限列表（用于接口权限校验）
     * 返回API资源的apiUrl列表，支持通配符
     *
     * @return API权限列表，如：["/system/user/**", "/system/role/list"]
     */
    List<String> getCurrentUserApiPermissions();

    /**
     * 检查当前用户是否有访问指定API的权限
     *
     * @param apiUrl API地址，如：/system/user/add
     * @return 是否有权限
     */
    boolean hasApiPermission(String apiUrl);
}
