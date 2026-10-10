package com.mdframe.forge.plugin.system.controller;
import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.system.config.PluginRuntimeProbeFilter;
import com.mdframe.forge.plugin.system.service.plugin.PluginRuntimeProbeService;
import com.mdframe.forge.plugin.system.vo.PluginRuntimeProbeVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @SaIgnore @RequiredArgsConstructor
public class PluginRuntimeProbeController {
    private final PluginRuntimeProbeService service;
    @GetMapping("/internal/plugin-runtime/probe")
    public RespInfo<PluginRuntimeProbeVO> probe(HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(PluginRuntimeProbeFilter.VERIFIED))) {
            throw new BusinessException(403, "运行探针未认证");
        }
        return RespInfo.success(service.probe(request.getHeader("X-Forge-Probe-Nonce")));
    }
}
