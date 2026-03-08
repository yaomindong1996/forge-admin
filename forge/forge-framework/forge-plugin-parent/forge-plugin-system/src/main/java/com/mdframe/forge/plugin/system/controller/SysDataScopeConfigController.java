package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.plugin.system.service.ISysDataScopeConfigService;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
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
@ApiPermissionIgnore
public class SysDataScopeConfigController {

    private final ISysDataScopeConfigService dataScopeConfigService;

    /**
     * 分页查询数据权限配置列表
     */
    @GetMapping("/page")
    @OperationLog(module = "数据权限配置管理", type = OperationType.QUERY, desc = "分页查询配置列表")
    public RespInfo<Page<SysDataScopeConfig>> page(PageQuery pageQuery, SysDataScopeConfig query) {
        Page<SysDataScopeConfig> page = dataScopeConfigService.selectConfigPage(pageQuery, query);
        return RespInfo.success(page);
    }

    /**
     * 查询数据权限配置列表
     */
    @GetMapping("/list")
    @OperationLog(module = "数据权限配置管理", type = OperationType.QUERY, desc = "查询配置列表")
    public RespInfo<List<SysDataScopeConfig>> list(SysDataScopeConfig query) {
        List<SysDataScopeConfig> list = dataScopeConfigService.selectConfigList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询配置详情
     */
    @PostMapping("/getById")
    public RespInfo<SysDataScopeConfig> getById(@RequestParam Long id) {
        SysDataScopeConfig config = dataScopeConfigService.selectConfigById(id);
        return RespInfo.success(config);
    }

    /**
     * 新增数据权限配置
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysDataScopeConfig config) {
        boolean result = dataScopeConfigService.insertConfig(config);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改数据权限配置
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysDataScopeConfig config) {
        boolean result = dataScopeConfigService.updateConfig(config);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除数据权限配置
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = dataScopeConfigService.deleteConfigById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除数据权限配置
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        boolean result = dataScopeConfigService.deleteConfigByIds(ids);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
