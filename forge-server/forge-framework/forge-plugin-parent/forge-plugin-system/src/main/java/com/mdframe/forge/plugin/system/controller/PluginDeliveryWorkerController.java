package com.mdframe.forge.plugin.system.controller;
import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.system.config.PluginDeliveryFilter;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryLeaseDTO;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryFinishDTO;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryRecoveryDTO;
import com.mdframe.forge.plugin.system.vo.PluginDeliveryRecoveryVO;
import com.mdframe.forge.plugin.system.service.plugin.PluginDeliveryService;
import com.mdframe.forge.plugin.system.vo.PluginDeliveryClaimVO;
import com.mdframe.forge.plugin.system.vo.PluginDeliveryVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @SaIgnore @RequiredArgsConstructor
@RequestMapping("/internal/plugin-delivery")
public class PluginDeliveryWorkerController {
    private final PluginDeliveryService service;
    @PostMapping("/{id}/recovery")
    public RespInfo<PluginDeliveryRecoveryVO> recovery(@PathVariable String id,
            @Valid @RequestBody PluginDeliveryRecoveryDTO dto, HttpServletRequest request) {
        var identity = PluginDeliveryFilter.identity(request);
        return identity.inTenant(() -> RespInfo.success(service.recovery(id, dto, identity)));
    }
    @PostMapping("/{id}/claim")
    public RespInfo<PluginDeliveryClaimVO> claim(@PathVariable String id,
                                                @Valid @RequestBody PluginDeliveryLeaseDTO dto,
                                                HttpServletRequest request) {
        var identity = PluginDeliveryFilter.identity(request);
        return identity.inTenant(() -> RespInfo.success(service.claim(id, dto, identity)));
    }
    @PostMapping({"/{id}/authorize", "/{id}/heartbeat"})
    public RespInfo<PluginDeliveryClaimVO> authorize(@PathVariable String id,
                                                    @Valid @RequestBody PluginDeliveryLeaseDTO dto,
                                                    HttpServletRequest request) {
        var identity = PluginDeliveryFilter.identity(request);
        return identity.inTenant(() -> RespInfo.success(service.authorize(id, dto, identity)));
    }
    @PostMapping("/{id}/finish")
    public RespInfo<PluginDeliveryVO> finish(@PathVariable String id,
                                            @Valid @RequestBody PluginDeliveryFinishDTO dto,
                                            HttpServletRequest request) {
        var identity = PluginDeliveryFilter.identity(request);
        return identity.inTenant(() -> RespInfo.success(service.finish(id, dto, identity)));
    }
}
