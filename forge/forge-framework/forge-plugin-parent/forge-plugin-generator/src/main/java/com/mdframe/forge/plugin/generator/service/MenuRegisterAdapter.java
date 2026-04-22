package com.mdframe.forge.plugin.generator.service;

public interface MenuRegisterAdapter {

    Long registerMenu(String menuName, Long parentId, String configKey, Integer sort);

    void updateMenu(Long menuResourceId, String menuName, Integer sort);

    void deleteMenu(Long menuResourceId);

    /**
     * 检查指定菜单资源是否已被某个角色赋权
     *
     * @param menuResourceId 菜单资源 ID
     * @return true 表示已有角色赋权，不能删除
     */
    default boolean hasRolePermission(Long menuResourceId) {
        return false;
    }
}
