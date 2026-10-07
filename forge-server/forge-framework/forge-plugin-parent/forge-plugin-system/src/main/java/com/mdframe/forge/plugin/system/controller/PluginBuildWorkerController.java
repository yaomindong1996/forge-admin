package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginBuildClaimDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildFinishDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildHeartbeatDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildLeaseDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginBuildService;
import com.mdframe.forge.plugin.system.vo.PluginBuildClaimVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 专用机器认证过滤器始终先执行；这里不接受用户会话或 X-Inner-Call。 */
@RestController
@SaIgnore
@RequiredArgsConstructor
@RequestMapping(value = "/internal/plugin-build", consumes = MediaType.APPLICATION_JSON_VALUE)
public class PluginBuildWorkerController {
    private final PluginBuildService builds;

    @PostMapping("/{id}/claim")
    public RespInfo<PluginBuildClaimVO> claim(@PathVariable String id, @Valid @RequestBody PluginBuildClaimDTO dto,
                                            HttpServletRequest request) {
        var worker = PluginWorkerIdentity.required(request);
        return worker.inTenant(() -> RespInfo.success(builds.claim(id, dto, worker)));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<byte[]> archive(@PathVariable String id, @Valid @RequestBody PluginBuildLeaseDTO dto,
                                          HttpServletRequest request) {
        var worker = PluginWorkerIdentity.required(request);
        return worker.inTenant(() -> ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Cache-Control", "no-store").body(builds.archive(id, dto, worker)));
    }

    @PostMapping("/{id}/heartbeat")
    public RespInfo<PluginBuildClaimVO> heartbeat(@PathVariable String id,
                                                 @Valid @RequestBody PluginBuildHeartbeatDTO dto,
                                                 HttpServletRequest request) {
        var worker = PluginWorkerIdentity.required(request);
        return worker.inTenant(() -> RespInfo.success(builds.heartbeat(id, dto, worker)));
    }

    @PostMapping("/{id}/finish")
    public RespInfo<PluginBuildClaimVO> finish(@PathVariable String id, @Valid @RequestBody PluginBuildFinishDTO dto,
                                             HttpServletRequest request) {
        var worker = PluginWorkerIdentity.required(request);
        return worker.inTenant(() -> RespInfo.success(builds.finish(id, dto, worker)));
    }
}
