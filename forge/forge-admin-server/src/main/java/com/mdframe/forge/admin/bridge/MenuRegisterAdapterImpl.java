package com.mdframe.forge.admin.bridge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuRegisterAdapterImpl implements MenuRegisterAdapter {

    private final ISysResourceService resourceService;
    private final SysRoleResourceMapper roleResourceMapper;

    @Override
    public Long registerMenu(String menuName, Long parentId, String configKey, Integer sort) {
        SysResource resource = new SysResource();
        resource.setResourceName(menuName);
        resource.setParentId(parentId);
        resource.setResourceType(2);
        resource.setSort(sort);
        resource.setPath("/ai/crud-page/" + configKey);
        resource.setComponent("ai/crud-page");
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(1);
        resource.setVisible(1);
        resource.setPerms("ai:crud:" + configKey);
        resource.setKeepAlive(0);
        resource.setAlwaysShow(0);
        resourceService.save(resource);

        Long menuId = resource.getId();
        log.info("[MenuRegisterAdapter] 注册菜单成功: menuName={}, configKey={}, menuId={}", menuName, configKey, menuId);
        return menuId;
    }

    @Override
    public void updateMenu(Long menuResourceId, String menuName, Integer sort) {
        SysResource resource = new SysResource();
        resource.setId(menuResourceId);
        resource.setResourceName(menuName);
        resource.setSort(sort);
        resourceService.updateById(resource);
        log.info("[MenuRegisterAdapter] 更新菜单成功: menuId={}, menuName={}", menuResourceId, menuName);
    }

    @Override
    public void deleteMenu(Long menuResourceId) {
        resourceService.removeById(menuResourceId);
        log.info("[MenuRegisterAdapter] 删除菜单成功: menuId={}", menuResourceId);
    }

    @Override
    public boolean hasRolePermission(Long menuResourceId) {
        if (menuResourceId == null) {
            return false;
        }
        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getResourceId, menuResourceId);
        return roleResourceMapper.selectCount(wrapper) > 0;
    }
}
