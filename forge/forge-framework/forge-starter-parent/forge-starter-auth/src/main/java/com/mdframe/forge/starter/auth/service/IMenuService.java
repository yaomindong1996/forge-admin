package com.mdframe.forge.starter.auth.service;

import com.mdframe.forge.starter.auth.domain.UserResourceTreeVO;

import java.util.List;

/**
 * 当前用户菜单/权限查询接口
 * 由 forge-plugin-system 的 SysResourceService 实现
 */
public interface IMenuService {

    List<UserResourceTreeVO> selectCurrentUserResourceTree();

    List<UserResourceTreeVO> selectCurrentUserMenuTree();

    List<String> selectCurrentUserPermissions();
}
