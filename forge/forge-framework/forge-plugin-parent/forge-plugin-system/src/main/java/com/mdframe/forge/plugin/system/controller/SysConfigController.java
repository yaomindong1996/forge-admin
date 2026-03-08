package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysConfigDTO;
import com.mdframe.forge.plugin.system.dto.SysConfigQuery;
import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.service.ISysConfigService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置Controller
 */
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysConfigController {

    private final ISysConfigService configService;

    /**
     * 分页查询配置列表
     */
    @GetMapping("/page")
    @OperationLog(module = "系统配置管理", type = OperationType.QUERY, desc = "分页查询配置列表")
    public RespInfo<Page<SysConfig>> page(PageQuery pageQuery, SysConfigQuery query) {
        Page<SysConfig> page = configService.selectConfigPage(pageQuery, query);
        return RespInfo.success(page);
    }

    /**
     * 查询配置列表
     */
    @GetMapping("/list")
    @OperationLog(module = "系统配置管理", type = OperationType.QUERY, desc = "查询配置列表")
    public RespInfo<List<SysConfig>> list(SysConfigQuery query) {
        List<SysConfig> list = configService.selectConfigList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据配置键名查询配置值
     */
    @GetMapping("/configKey/{configKey}")
    public RespInfo<String> getConfigByKey(@PathVariable String configKey) {
        String configValue = configService.selectConfigByKey(configKey);
        return RespInfo.success(configValue);
    }

    /**
     * 根据ID查询配置详情
     */
    @PostMapping("/getById")
    public RespInfo<SysConfig> getById(@RequestParam Long configId) {
        SysConfig config = configService.selectConfigById(configId);
        return RespInfo.success(config);
    }

    /**
     * 新增配置
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysConfigDTO dto) {
        boolean result = configService.insertConfig(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改配置
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysConfigDTO dto) {
        boolean result = configService.updateConfig(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除配置
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long configId) {
        boolean result = configService.deleteConfigById(configId);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除配置
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] configIds) {
        boolean result = configService.deleteConfigByIds(configIds);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
