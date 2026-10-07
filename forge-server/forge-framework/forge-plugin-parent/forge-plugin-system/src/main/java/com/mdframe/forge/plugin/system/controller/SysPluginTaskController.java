package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskCommandDTO;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskQuery;
import com.mdframe.forge.plugin.system.dto.SysPluginUploadDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskCommandService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskQueryService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskUploadService;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskPageVO;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 只有任务管理协议，没有下载 ZIP 或执行构建/部署命令的端点。 */
@RestController
@RequestMapping("/system/plugin-task")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysPluginTaskController {
    private final PluginTaskUploadService uploads;
    private final PluginTaskQueryService queries;
    private final PluginTaskCommandService commands;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @SaCheckPermission("system:plugin:upload")
    @OperationLog(module = "插件中心", type = OperationType.ADD, desc = "上传插件预检",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SysPluginTaskVO> upload(@Valid @ModelAttribute SysPluginUploadDTO dto) {
        return RespInfo.success(uploads.upload(dto, PluginTaskActor.current()));
    }

    @GetMapping("/page")
    @SaCheckPermission("system:plugin:task:list")
    public RespInfo<SysPluginTaskPageVO> page(@Valid SysPluginTaskQuery query) {
        return RespInfo.success(queries.page(query, PluginTaskActor.current()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:plugin:task:detail")
    public RespInfo<SysPluginTaskVO> detail(@PathVariable String id) {
        return RespInfo.success(queries.detail(id, PluginTaskActor.current()));
    }

    @PostMapping("/{id}/confirm")
    @SaCheckPermission("system:plugin:confirm")
    @OperationLog(module = "插件中心", type = OperationType.UPDATE, desc = "确认待构建",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SysPluginTaskVO> confirm(@PathVariable String id, @Valid @RequestBody SysPluginTaskCommandDTO dto) {
        return RespInfo.success(commands.confirm(id, dto, PluginTaskActor.current()));
    }

    @PostMapping("/{id}/cancel")
    @SaCheckPermission("system:plugin:cancel")
    @OperationLog(module = "插件中心", type = OperationType.UPDATE, desc = "取消插件任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SysPluginTaskVO> cancel(@PathVariable String id, @Valid @RequestBody SysPluginTaskCommandDTO dto) {
        return RespInfo.success(commands.cancel(id, dto, PluginTaskActor.current()));
    }
}
