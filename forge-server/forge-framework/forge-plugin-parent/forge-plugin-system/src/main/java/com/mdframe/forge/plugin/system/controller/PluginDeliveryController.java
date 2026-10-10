package com.mdframe.forge.plugin.system.controller;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryCreateDTO;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryReconcileDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginDeliveryService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import com.mdframe.forge.plugin.system.vo.*;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequiredArgsConstructor @ApiDecrypt @ApiEncrypt
@RequestMapping("/system/plugin-delivery")
public class PluginDeliveryController {
    private final PluginDeliveryService service;
    @GetMapping("/targets") @SaCheckPermission("system:plugin:delivery:list")
    public RespInfo<List<PluginDeliveryTargetVO>> targets() {
        return RespInfo.success(service.targets(PluginTaskActor.current()));
    }
    @GetMapping("/candidates") @SaCheckPermission("system:plugin:delivery:list")
    public RespInfo<List<PluginDeliveryCandidateVO>> candidates() {
        return RespInfo.success(service.candidates(PluginTaskActor.current()));
    }
    @GetMapping("/list") @SaCheckPermission("system:plugin:delivery:list")
    public RespInfo<List<PluginDeliveryVO>> list() {
        return RespInfo.success(service.recent(PluginTaskActor.current()));
    }
    @PostMapping("/add") @SaCheckPermission("system:plugin:delivery:execute")
    @OperationLog(module = "插件交付", type = OperationType.ADD, desc = "确认交付任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PluginDeliveryVO> add(@Valid @RequestBody PluginDeliveryCreateDTO dto) {
        return RespInfo.success(service.create(dto, PluginTaskActor.current()));
    }
    @PostMapping("/{id}/reconcile") @SaCheckPermission("system:plugin:delivery:reconcile")
    @OperationLog(module = "插件交付", type = OperationType.UPDATE, desc = "人工关闭交付占用",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<PluginDeliveryVO> reconcile(@PathVariable String id,
                                               @Valid @RequestBody PluginDeliveryReconcileDTO dto) {
        return RespInfo.success(service.reconcile(id, dto, PluginTaskActor.current()));
    }
}
