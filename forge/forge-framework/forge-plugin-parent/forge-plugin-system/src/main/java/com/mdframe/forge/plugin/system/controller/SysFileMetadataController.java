package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileGroupMapper;
import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文件元数据管理
 */
@RestController
@RequestMapping("/system/file/metadata")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysFileMetadataController {
    
    private final ISysFileMetadataService fileMetadataService;
    private final SysFileGroupMapper fileGroupMapper;
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    public RespInfo<Page<SysFileMetadata>> page(PageQuery query, SysFileMetadata condition) {
        return RespInfo.success(fileMetadataService.page(query, condition));
    }
    
    /**
     * 详情
     */
    @GetMapping("/{id}")
    public RespInfo<SysFileMetadata> detail(@PathVariable Long id) {
        return RespInfo.success(fileMetadataService.getById(id));
    }
    
    /**
     * 根据业务类型和业务ID查询
     */
    @GetMapping("/business/{businessType}/{businessId}")
    public RespInfo<List<SysFileMetadata>> listByBusiness(
            @PathVariable String businessType,
            @PathVariable String businessId) {
        return RespInfo.success(fileMetadataService.listByBusiness(businessType, businessId));
    }
    
    /**
     * 获取文件统计数据
     */
    @GetMapping("/statistics")
    public RespInfo<Map<String, Object>> statistics() {
        return RespInfo.success(fileGroupMapper.selectFileStatistics());
    }
    
    /**
     * 批量删除
     */
    @DeleteMapping("/{fileIds}")
    public RespInfo<Void> remove(@PathVariable String[] fileIds) {
        fileMetadataService.removeBatch(fileIds);
        return RespInfo.success();
    }
    
    /**
     * 更新文件元数据（如移动分组）
     */
    @PutMapping
    public RespInfo<Boolean> update(@RequestBody SysFileMetadata metadata) {
        return RespInfo.success(fileMetadataService.updateById(metadata));
    }
}
