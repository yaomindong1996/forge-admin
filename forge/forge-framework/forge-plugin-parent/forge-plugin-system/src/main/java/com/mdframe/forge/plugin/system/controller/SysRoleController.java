package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.RoleUserQuery;
import com.mdframe.forge.plugin.system.dto.SysRoleDTO;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.service.ISysRoleService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理Controller
 */
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysRoleController {

    private final ISysRoleService roleService;

    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysRole>> page(SysRoleQuery query) {
        IPage<SysRole> page = roleService.selectRolePage(query);
        return RespInfo.success(page);
    }

    /**
     * 根据ID查询角色详情
     */
    @PostMapping("/getById")
    public RespInfo<SysRole> getById(@RequestParam Long id) {
        SysRole role = roleService.selectRoleById(id);
        return RespInfo.success(role);
    }

    /**
     * 新增角色
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysRoleDTO dto) {
        boolean result = roleService.insertRole(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改角色
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysRoleDTO dto) {
        boolean result = roleService.updateRole(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除角色
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = roleService.deleteRoleById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除角色
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = roleService.deleteRoleByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    /**
     * 给角色绑定资源（菜单/按钮/接口）
     */
    @PostMapping("/{roleId}/resources")
    public RespInfo<Void> bindResources(@PathVariable Long roleId, @RequestBody Long[] resourceIds) {
        boolean result = roleService.bindRoleResources(roleId, resourceIds);
        return result ? RespInfo.success() : RespInfo.error("绑定资源失败");
    }

    /**
     * 解除角色资源
     */
    @PostMapping("/{roleId}/resources/unbind")
    public RespInfo<Void> unbindResources(@PathVariable Long roleId, @RequestBody Long[] resourceIds) {
        boolean result = roleService.unbindRoleResources(roleId, resourceIds);
        return result ? RespInfo.success() : RespInfo.error("解除资源失败");
    }

    /**
     * 查询角色的资源ID列表
     */
    @GetMapping("/{roleId}/resources")
    public RespInfo<List<Long>> getRoleResourceIds(@PathVariable Long roleId) {
        List<Long> resourceIds = roleService.selectRoleResourceIds(roleId);
        return RespInfo.success(resourceIds);
    }

    /**
     * 查询角色下的用户列表（分页）
     */
    @GetMapping("/{roleId}/users")
    public RespInfo<IPage<SysUser>> getRoleUsers(@PathVariable Long roleId, RoleUserQuery query) {
        query.setRoleId(roleId);
        IPage<SysUser> page = roleService.selectRoleUsers(query);
        return RespInfo.success(page);
    }

    /**
     * 移除角色用户
     */
    @PostMapping("/removeUserRole")
    public RespInfo<Void> removeUserRole(@RequestParam Long roleId, @RequestParam Long userId) {
        boolean result = roleService.removeUserRole(roleId, userId);
        return result ? RespInfo.success() : RespInfo.error("移除用户失败");
    }
}
