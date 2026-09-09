package com.mdframe.forge.plugin.system.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mdframe.forge.plugin.system.dto.DataScopeConfigDTO;
import com.mdframe.forge.plugin.system.dto.DataScopeConfigStatusDTO;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.plugin.system.service.ISysDataScopeConfigService;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据权限配置Controller
 */
@RestController
@RequestMapping("/system/dataScopeConfig")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysDataScopeConfigController {

    private final ISysDataScopeConfigService dataScopeConfigService;
    private final IDataScopeService dataScopeService;

    /**
     * 分页查询数据权限配置列表
     */
    @GetMapping("/page")
    @OperationLog(module = "数据权限配置管理", type = OperationType.QUERY, desc = "分页查询配置列表")
    public RespInfo<Page<SysDataScopeConfig>> page(PageQuery pageQuery, SysDataScopeConfig query) {
        assertPlatformAdmin();
        Page<SysDataScopeConfig> page = dataScopeConfigService.selectConfigPage(pageQuery, query);
        return RespInfo.success(page);
    }

    /**
     * 查询数据权限配置列表
     */
    @GetMapping("/list")
    @OperationLog(module = "数据权限配置管理", type = OperationType.QUERY, desc = "查询配置列表")
    public RespInfo<List<SysDataScopeConfig>> list(SysDataScopeConfig query) {
        assertPlatformAdmin();
        List<SysDataScopeConfig> list = dataScopeConfigService.selectConfigList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询配置详情
     */
    @PostMapping("/getById")
    public RespInfo<SysDataScopeConfig> getById(@RequestParam Long id) {
        assertPlatformAdmin();
        SysDataScopeConfig config = dataScopeConfigService.selectConfigById(id);
        return RespInfo.success(config);
    }

    /**
     * 新增数据权限配置
     */
    @PostMapping("/add")
    @OperationLog(module = "数据权限配置管理", type = OperationType.ADD, desc = "新增数据权限规则")
    public RespInfo<Void> add(@Valid @RequestBody DataScopeConfigDTO dto) {
        assertPlatformAdmin();
        SysDataScopeConfig config = BeanUtil.copyProperties(dto, SysDataScopeConfig.class);
        boolean result = dataScopeConfigService.insertConfig(config);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改数据权限配置
     */
    @PostMapping("/edit")
    @OperationLog(module = "数据权限配置管理", type = OperationType.UPDATE, desc = "修改数据权限规则")
    public RespInfo<Void> edit(@Valid @RequestBody DataScopeConfigDTO dto) {
        assertPlatformAdmin();
        SysDataScopeConfig config = BeanUtil.copyProperties(dto, SysDataScopeConfig.class);
        boolean result = dataScopeConfigService.updateConfig(config);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除数据权限配置
     */
    @PostMapping("/remove")
    @OperationLog(module = "数据权限配置管理", type = OperationType.DELETE, desc = "删除数据权限规则")
    public RespInfo<Void> remove(@RequestParam Long id) {
        assertPlatformAdmin();
        boolean result = dataScopeConfigService.deleteConfigById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除数据权限配置
     */
    @PostMapping("/removeBatch")
    @OperationLog(module = "数据权限配置管理", type = OperationType.DELETE, desc = "批量删除数据权限规则")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        assertPlatformAdmin();
        boolean result = dataScopeConfigService.deleteConfigByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    @PostMapping("/status")
    @OperationLog(module = "数据权限配置管理", type = OperationType.UPDATE, desc = "启用或禁用数据权限规则")
    public RespInfo<Void> updateStatus(@Valid @RequestBody DataScopeConfigStatusDTO dto) {
        assertPlatformAdmin();
        dataScopeConfigService.updateConfigStatus(dto);
        return RespInfo.success();
    }

    @PostMapping("/refreshCache")
    @OperationLog(module = "数据权限配置管理", type = OperationType.UPDATE, desc = "刷新数据权限")
    public RespInfo<Void> refreshCache() {
        assertPlatformAdmin();
        dataScopeService.refreshDataScopeCache();
        return RespInfo.success();
    }

    private void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以维护数据权限配置");
    }
}
