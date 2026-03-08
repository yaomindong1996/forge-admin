package com.mdframe.forge.starter.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.config.service.ISysConfigGroupService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 系统配置分组控制器
 * 对应数据库表: sys_config_group
 */
@RestController
@RequestMapping("/api/config/group")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
@ApiPermissionIgnore
@IgnoreTenant
public class SysConfigGroupController {

    private final ISysConfigGroupService sysConfigGroupService;

    /**
     * 分页查询配置分组列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysConfigGroup>> page(PageQuery pageQuery, SysConfigGroup sysConfigGroup) {
        Page<SysConfigGroup> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        IPage<SysConfigGroup> result = sysConfigGroupService.page(page, null);
        return RespInfo.success(result);
    }

    /**
     * 查询所有配置分组列表
     */
    @GetMapping("/list")
    public RespInfo<List<SysConfigGroup>> list(SysConfigGroup sysConfigGroup) {
        List<SysConfigGroup> list = sysConfigGroupService.list();
        return RespInfo.success(list);
    }

    /**
     * 根据ID获取配置分组详情
     */
    @GetMapping("/{id}")
    public RespInfo<SysConfigGroup> getInfo(@PathVariable Long id) {
        SysConfigGroup sysConfigGroup = sysConfigGroupService.getById(id);
        return RespInfo.success(sysConfigGroup);
    }

    /**
     * 新增配置分组
     */
    @PostMapping
    public RespInfo<Void> add(@Valid @RequestBody SysConfigGroup sysConfigGroup) {
        // 检查分组编码是否已存在
        SysConfigGroup existing = sysConfigGroupService.selectByGroupCode(sysConfigGroup.getGroupCode());
        if (existing != null) {
            return RespInfo.error("分组编码已存在：" + sysConfigGroup.getGroupCode());
        }
        sysConfigGroupService.save(sysConfigGroup);
        return RespInfo.success();
    }

    /**
     * 修改配置分组
     */
    @PutMapping
    public RespInfo<Void> edit(@Valid @RequestBody SysConfigGroup sysConfigGroup) {
        // 检查分组编码是否被其他记录占用
        SysConfigGroup existing = sysConfigGroupService.selectByGroupCode(sysConfigGroup.getGroupCode());
        if (existing != null && !existing.getId().equals(sysConfigGroup.getId())) {
            return RespInfo.error("分组编码已存在：" + sysConfigGroup.getGroupCode());
        }
        sysConfigGroupService.updateById(sysConfigGroup);
        return RespInfo.success();
    }

    /**
     * 删除配置分组
     */
    @DeleteMapping("/{ids}")
    public RespInfo<Void> delete(@PathVariable Long[] ids) {
        sysConfigGroupService.removeByIds(Arrays.asList(ids));
        return RespInfo.success();
    }

    /**
     * 启用/禁用配置分组
     */
    @PutMapping("/changeStatus")
    public RespInfo<Void> changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        SysConfigGroup sysConfigGroup = new SysConfigGroup();
        sysConfigGroup.setId(id);
        sysConfigGroup.setStatus(status);
        sysConfigGroupService.updateById(sysConfigGroup);
        String msg = status == 1 ? "启用" : "禁用";
        return RespInfo.success();
    }

    /**
     * 根据分组编码获取配置分组
     */
    @GetMapping("/byCode/{groupCode}")
    public RespInfo<SysConfigGroup> getByGroupCode(@PathVariable String groupCode) {
        SysConfigGroup sysConfigGroup = sysConfigGroupService.selectByGroupCode(groupCode);
        return RespInfo.success(sysConfigGroup);
    }

    /**
     * 查询所有启用的配置分组
     */
    @GetMapping("/enabled")
    public RespInfo<List<SysConfigGroup>> getEnabledGroups() {
        List<SysConfigGroup> list = sysConfigGroupService.selectEnabledGroups();
        return RespInfo.success(list);
    }
}
