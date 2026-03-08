package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysFileStorageConfig;
import com.mdframe.forge.plugin.system.service.ISysFileStorageConfigService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 文件存储配置管理
 */
@RestController
@RequestMapping("/system/storage/config")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysFileStorageConfigController {
    
    private final ISysFileStorageConfigService storageConfigService;
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    public RespInfo<Page<SysFileStorageConfig>> page(PageQuery query, SysFileStorageConfig condition) {
        return RespInfo.success(storageConfigService.page(query, condition));
    }
    
    /**
     * 详情
     */
    @PostMapping("/detail")
    public RespInfo<SysFileStorageConfig> detail(@RequestParam Long id) {
        return RespInfo.success(storageConfigService.getById(id));
    }
    
    /**
     * 新增
     */
    @PostMapping
    public RespInfo<Void> add(@RequestBody SysFileStorageConfig config) {
        storageConfigService.save(config);
        return RespInfo.success();
    }
    
    /**
     * 修改
     */
    @PutMapping
    public RespInfo<Void> edit(@RequestBody SysFileStorageConfig config) {
        storageConfigService.updateById(config);
        return RespInfo.success();
    }
    
    /**
     * 删除
     */
    @DeleteMapping("/{ids}")
    public RespInfo<Void> remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            storageConfigService.removeById(id);
        }
        return RespInfo.success();
    }
    
    /**
     * 设置默认配置
     */
    @PutMapping("/default/{id}")
    public RespInfo<Void> setDefault(@PathVariable Long id) {
        storageConfigService.setDefault(id);
        return RespInfo.success();
    }
    
    /**
     * 启用/禁用
     */
    @PutMapping("/enabled/{id}/{enabled}")
    public RespInfo<Void> updateEnabled(@PathVariable Long id, @PathVariable Boolean enabled) {
        storageConfigService.updateEnabled(id, enabled);
        return RespInfo.success();
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/test/{id}")
    public RespInfo<Boolean> testConnection(@PathVariable Long id) {
        return RespInfo.success(storageConfigService.testConnection(id));
    }
}
