package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysTenantDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantQuery;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.service.ISysTenantService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理Controller
 */
@RestController
@RequestMapping("/system/tenant")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysTenantController {

    private final ISysTenantService tenantService;

    /**
     * 分页查询租户列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysTenant>> page(SysTenantQuery query) {
        IPage<SysTenant> page = tenantService.selectTenantPage(query);
        return RespInfo.success(page);
    }

    /**
     * 根据ID查询租户详情
     */
    @PostMapping("/getById")
    public RespInfo<SysTenant> getById(@RequestParam Long id) {
        SysTenant tenant = tenantService.selectTenantById(id);
        return RespInfo.success(tenant);
    }

    /**
     * 查询用户当前租户的配置
     */
    @PostMapping("/userTenantConfig")
    public RespInfo<SysTenant> selectUserTenantConfig(@RequestParam(required = false) Long id) {
        SysTenant tenant = tenantService.selectUserTenantConfig(id);
        return RespInfo.success(tenant);
    }

    /**
     * 新增租户
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysTenantDTO dto) {
        boolean result = tenantService.insertTenant(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改租户
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysTenantDTO dto) {
        boolean result = tenantService.updateTenant(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除租户
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = tenantService.deleteTenantById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除租户
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = tenantService.deleteTenantByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
