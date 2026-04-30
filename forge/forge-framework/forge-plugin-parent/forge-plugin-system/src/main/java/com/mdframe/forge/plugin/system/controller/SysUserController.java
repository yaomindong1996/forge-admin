package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.dto.UserOrgBindDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysUserController {

    private final ISysUserService userService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysUser>> page(SysUserQuery query) {
        IPage<SysUser> page = userService.selectUserPage(query);
        return RespInfo.success(page);
    }

    /**
     * 根据ID查询用户详情
     */
    @PostMapping("/getById")
    public RespInfo<SysUser> getById(@RequestParam Long id) {
        SysUser user = userService.selectUserById(id);
        return RespInfo.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysUserDTO dto) {
        boolean result = userService.insertUser(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改用户
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysUserDTO dto) {
        boolean result = userService.updateUser(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除用户
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = userService.deleteUserById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }
    
    @PostMapping("/doUntieDisable")
    public RespInfo<Void> doUntieDisable(@RequestParam Long id) {
        userService.doUntieDisable(id);
        return RespInfo.success();
    }

    /**
     * 批量删除用户
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = userService.deleteUserByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    /**
     * 给用户绑定角色
     */
    @PostMapping("/{userId}/roles")
    public RespInfo<Void> bindRoles(@PathVariable Long userId, @RequestBody Long[] roleIds) {
        boolean result = userService.bindUserRoles(userId, roleIds);
        return result ? RespInfo.success() : RespInfo.error("绑定角色失败");
    }

    /**
     * 解除用户角色
     */
    @PostMapping("/{userId}/roles/unbind")
    public RespInfo<Void> unbindRoles(@PathVariable Long userId, @RequestBody Long[] roleIds) {
        boolean result = userService.unbindUserRoles(userId, roleIds);
        return result ? RespInfo.success() : RespInfo.error("解除角色失败");
    }

    /**
     * 给用户绑定组织
     */
    @PostMapping("/{userId}/org")
    public RespInfo<Void> bindOrg(@PathVariable Long userId, @RequestParam Long orgId, @RequestParam(required = false, defaultValue = "0") Integer isMain) {
        boolean result = userService.bindUserOrg(userId, orgId, isMain);
        return result ? RespInfo.success() : RespInfo.error("绑定组织失败");
    }

    /**
     * 解除用户组织
     */
    @PostMapping("/{userId}/org/unbind")
    public RespInfo<Void> unbindOrg(@PathVariable Long userId, @RequestParam Long orgId) {
        boolean result = userService.unbindUserOrg(userId, orgId);
        return result ? RespInfo.success() : RespInfo.error("解除组织失败");
    }

    /**
     * 查询用户的角色ID列表
     */
    @GetMapping("/{userId}/roles")
    public RespInfo<List<Long>> getUserRoleIds(@PathVariable Long userId) {
        List<Long> roleIds = userService.selectUserRoleIds(userId);
        return RespInfo.success(roleIds);
    }

    /**
     * 查询用户的组织ID列表
     */
    @GetMapping("/{userId}/orgs")
    public RespInfo<List<Long>> getUserOrgIds(@PathVariable Long userId) {
        List<Long> orgIds = userService.selectUserOrgIds(userId);
        return RespInfo.success(orgIds);
    }

    /**
     * 批量绑定用户组织
     */
    @PostMapping("/{userId}/orgs")
    public RespInfo<Void> bindOrgs(@PathVariable Long userId, @RequestBody UserOrgBindDTO dto) {
        boolean result = userService.bindUserOrgs(userId, dto.getOrgIds(), dto.getMainOrgId());
        return result ? RespInfo.success() : RespInfo.error("绑定组织失败");
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/resetPwd")
    public RespInfo<Void> resetPwd(@RequestParam Long id, @RequestParam String password) {
        boolean result = userService.resetPassword(id, password);
        return result ? RespInfo.success() : RespInfo.error("重置密码失败");
    }

    /**
     * 更新用户状态
     */
    @PostMapping("/updateStatus")
    public RespInfo<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        boolean result = userService.updateUserStatus(id, status);
        return result ? RespInfo.success() : RespInfo.error("操作失败");
    }

    /**
     * 更新用户资料
     */
    @PostMapping("/updateProfile")
    public RespInfo<Void> updateProfile(@RequestBody SysUserDTO dto) {
        boolean result = userService.updateUserProfile(dto);
        return result ? RespInfo.success() : RespInfo.error("更新资料失败");
    }

    /**
     * 获取当前登录用户的基本资料（直接查数据库，非缓存）
     */
    @GetMapping("/profile")
    public RespInfo<SysUser> profile() {
        Long userId = SessionHelper.getUserId();
        SysUser user = userService.getById(userId);
        user.setPassword(null);
        user.setSalt(null);
        return RespInfo.success(user);
    }
}
