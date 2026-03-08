package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.entity.SysExcelColumnConfig;
import com.mdframe.forge.plugin.system.service.ISysExcelColumnConfigService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Excel列配置管理
 */
@RestController
@RequestMapping("/system/excel/column-config")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysExcelColumnConfigController {
    
    private final ISysExcelColumnConfigService columnConfigService;
    
    /**
     * 根据配置键查询列配置
     */
    @GetMapping("/list")
    public RespInfo<List<SysExcelColumnConfig>> list(@RequestParam String configKey) {
        return RespInfo.success(columnConfigService.listByConfigKey(configKey));
    }
    
    /**
     * 详情
     */
    @PostMapping("/detail")
    public RespInfo<SysExcelColumnConfig> detail(@RequestParam Long id) {
        return RespInfo.success(columnConfigService.getById(id));
    }
    
    /**
     * 新增
     */
    @PostMapping
    public RespInfo<Void> add(@RequestBody SysExcelColumnConfig config) {
        columnConfigService.save(config);
        return RespInfo.success();
    }
    
    /**
     * 修改
     */
    @PutMapping
    public RespInfo<Void> edit(@RequestBody SysExcelColumnConfig config) {
        columnConfigService.updateById(config);
        return RespInfo.success();
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/{ids}")
    public RespInfo<Void> remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            columnConfigService.removeById(id);
        }
        return RespInfo.success();
    }
    
    /**
     * 批量保存列配置
     */
    @PostMapping("/batch")
    public RespInfo<Void> saveBatch(@RequestParam String configKey, @RequestBody List<SysExcelColumnConfig> columns) {
        columnConfigService.saveBatch(configKey, columns);
        return RespInfo.success();
    }
}
