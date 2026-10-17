package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysFileDownloadLogCreateDTO;
import com.mdframe.forge.plugin.system.dto.SysFileDownloadLogQuery;
import com.mdframe.forge.plugin.system.entity.SysFileDownloadLog;
import com.mdframe.forge.plugin.system.service.impl.SysFileDownloadLogServiceImpl;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 下载记录。数据范围在服务端按超级管理员 / 租户管理员 / 本人收紧。
 */
@RestController
@RequestMapping("/system/file/download-log")
@RequiredArgsConstructor
public class SysFileDownloadLogController {

    private final SysFileDownloadLogServiceImpl downloadLogService;

    @SaCheckPermission("system:file:downloadLog:list")
    @GetMapping("/page")
    public RespInfo<Page<SysFileDownloadLog>> page(PageQuery pageQuery, SysFileDownloadLogQuery query) {
        return RespInfo.success(downloadLogService.page(pageQuery.toPage(), query));
    }

    /** 对象存储直链不经过文件下载过滤器，由前端在下载成功后补记。 */
    @SaCheckPermission("system:file:downloadLog:list")
    @PostMapping
    public RespInfo<Void> record(@Valid @RequestBody SysFileDownloadLogCreateDTO dto) {
        downloadLogService.recordExternal(dto);
        return RespInfo.success();
    }
}
