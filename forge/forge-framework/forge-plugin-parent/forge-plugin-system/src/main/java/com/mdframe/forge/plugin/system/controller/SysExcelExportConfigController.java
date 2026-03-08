package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import com.mdframe.forge.plugin.system.service.ISysExcelColumnConfigService;
import com.mdframe.forge.plugin.system.service.ISysExcelExportConfigService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.excel.core.DynamicExportEngine;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Excel导出配置管理
 */
@RestController
@RequestMapping("/system/excel/export-config")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysExcelExportConfigController {
    
    private final ISysExcelExportConfigService exportConfigService;
    private final ISysExcelColumnConfigService columnConfigService;
    private final DynamicExportEngine dynamicExportEngine;
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    public RespInfo<Page<SysExcelExportConfig>> page(PageQuery query, SysExcelExportConfig condition) {
        return RespInfo.success(exportConfigService.page(query, condition));
    }
    
    /**
     * 详情
     */
    @PostMapping("/detail")
    public RespInfo<SysExcelExportConfig> detail(@RequestParam Long id) {
        return RespInfo.success(exportConfigService.getById(id));
    }
    
    /**
     * 新增
     */
    @PostMapping
    public RespInfo<Void> add(@RequestBody SysExcelExportConfig config) {
        exportConfigService.save(config);
        return RespInfo.success();
    }
    
    /**
     * 修改
     */
    @PutMapping
    public RespInfo<Void> edit(@RequestBody SysExcelExportConfig config) {
        exportConfigService.updateById(config);
        return RespInfo.success();
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/{ids}")
    public RespInfo<Void> remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            SysExcelExportConfig config = exportConfigService.getById(id);
            if (config != null) {
                // 删除主配置
                exportConfigService.removeById(id);
                // 删除关联的列配置
                columnConfigService.deleteByConfigKey(config.getConfigKey());
            }
        }
        return RespInfo.success();
    }
    
    /**
     * 更新状态
     */
    @PutMapping("/status")
    public RespInfo<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        exportConfigService.updateStatus(id, status);
        return RespInfo.success();
    }
    
    /**
     * 复制配置
     */
    @PostMapping("/copy")
    public RespInfo<SysExcelExportConfig> copy(@RequestParam Long id, @RequestParam String newConfigKey) {
        SysExcelExportConfig newConfig = exportConfigService.copyConfig(id, newConfigKey);
        return RespInfo.success(newConfig);
    }
    
    /**
     * 导出测试（生成示例Excel）
     */
    @GetMapping("/test/{id}")
    public void testExport(@PathVariable Long id, HttpServletResponse response) {
        SysExcelExportConfig config = exportConfigService.getById(id);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }
        
        // 使用空参数进行测试导出
        Map<String, Object> params = new HashMap<>();
        dynamicExportEngine.export(response, config.getConfigKey(), params);
    }
}
