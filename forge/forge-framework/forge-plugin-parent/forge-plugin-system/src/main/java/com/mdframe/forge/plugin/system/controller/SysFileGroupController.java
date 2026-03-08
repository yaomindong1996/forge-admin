package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.entity.SysFileGroup;
import com.mdframe.forge.plugin.system.service.ISysFileGroupService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文件分组管理
 */
@RestController
@RequestMapping("/system/file/group")
@RequiredArgsConstructor
@ApiPermissionIgnore
@ApiDecrypt
@ApiEncrypt
public class SysFileGroupController {

    private final ISysFileGroupService fileGroupService;

    /**
     * 获取分组列表（带文件数量）
     */
    @GetMapping("/list")
    public RespInfo<List<SysFileGroup>> list() {
        return RespInfo.success(fileGroupService.listGroupWithFileCount());
    }

    /**
     * 获取文件统计数据
     */
    @GetMapping("/statistics")
    public RespInfo<Map<String, Object>> statistics() {
        return RespInfo.success(fileGroupService.getFileStatistics());
    }

    /**
     * 获取分组详情
     */
    @GetMapping("/{id}")
    public RespInfo<SysFileGroup> detail(@PathVariable Long id) {
        return RespInfo.success(fileGroupService.getById(id));
    }

    /**
     * 创建分组
     */
    @PostMapping
    public RespInfo<Boolean> create(@RequestBody SysFileGroup group) {
        return RespInfo.success(fileGroupService.createGroup(group));
    }

    /**
     * 更新分组
     */
    @PutMapping
    public RespInfo<Boolean> update(@RequestBody SysFileGroup group) {
        return RespInfo.success(fileGroupService.updateGroup(group));
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/{id}")
    public RespInfo<Boolean> delete(@PathVariable Long id) {
        return RespInfo.success(fileGroupService.deleteGroup(id));
    }
}
