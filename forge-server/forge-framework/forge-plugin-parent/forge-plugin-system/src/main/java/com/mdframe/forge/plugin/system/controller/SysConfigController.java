package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysConfigDTO;
import com.mdframe.forge.plugin.system.dto.SysConfigQuery;
import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.service.ISysConfigService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import jakarta.validation.Valid;
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
public class SysConfigController {

    private final ISysConfigService configService;

    /**
     * 分页查询配置列表
     */
    @GetMapping("/page")
    @OperationLog(module = "系统配置管理", type = OperationType.QUERY, desc = "分页查询配置列表")
    public RespInfo<Page<SysConfig>> page(PageQuery pageQuery, SysConfigQuery query) {
        assertPlatformAdmin();
        Page<SysConfig> page = configService.selectConfigPage(pageQuery, query);
        maskConfigPage(page);
        return RespInfo.success(page);
    }

    /**
     * 查询配置列表
     */
    @GetMapping("/list")
    @OperationLog(module = "系统配置管理", type = OperationType.QUERY, desc = "查询配置列表")
    public RespInfo<List<SysConfig>> list(SysConfigQuery query) {
        assertPlatformAdmin();
        List<SysConfig> list = configService.selectConfigList(query);
        list.forEach(this::maskConfigValue);
        return RespInfo.success(list);
    }

    /**
     * 根据配置键名查询配置值
     */
    @GetMapping("/configKey/{configKey}")
    public RespInfo<String> getConfigByKey(@PathVariable String configKey) {
        assertPlatformAdmin();
        if (SensitiveDataUtil.isSensitiveKey(configKey)) {
            return RespInfo.error(403, "敏感配置不允许明文查询");
        }
        String configValue = configService.selectConfigByKey(configKey);
        return RespInfo.success(configValue);
    }

    /**
     * 根据ID查询配置详情
     */
    @PostMapping("/getById")
    public RespInfo<SysConfig> getById(@RequestParam Long configId) {
        assertPlatformAdmin();
        SysConfig config = configService.selectConfigById(configId);
        maskConfigValue(config);
        return RespInfo.success(config);
    }

    /**
     * 新增配置
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@Valid @RequestBody SysConfigDTO dto) {
        assertPlatformAdmin();
        boolean result = configService.insertConfig(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改配置
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@Valid @RequestBody SysConfigDTO dto) {
        assertPlatformAdmin();
        boolean result = configService.updateConfig(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除配置
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long configId) {
        assertPlatformAdmin();
        boolean result = configService.deleteConfigById(configId);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除配置
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] configIds) {
        assertPlatformAdmin();
        boolean result = configService.deleteConfigByIds(configIds);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    private void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以维护系统参数配置");
    }

    private void maskConfigPage(Page<SysConfig> page) {
        if (page == null || page.getRecords() == null) {
            return;
        }
        page.getRecords().forEach(this::maskConfigValue);
    }

    private void maskConfigValue(SysConfig config) {
        if (config == null) {
            return;
        }
        config.setConfigValue(SensitiveDataUtil.maskSensitiveValue(config.getConfigKey(), config.getConfigValue()));
    }
}
