package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginReleaseCheckDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginReleaseCheckService;
import com.mdframe.forge.plugin.system.vo.PluginReleaseCheckVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 专用过滤器认证原构建机器；跳过用户会话不意味着匿名入口。 */
@RestController
@SaIgnore
@RequiredArgsConstructor
@RequestMapping(value = "/internal/plugin-build", consumes = MediaType.APPLICATION_JSON_VALUE)
public class PluginReleaseCheckController {
    private final PluginReleaseCheckService checks;

    @PostMapping("/{id}/approval-check")
    public RespInfo<PluginReleaseCheckVO> check(@PathVariable String id,
                                               @Valid @RequestBody PluginReleaseCheckDTO command,
                                               HttpServletRequest request) {
        var worker = PluginWorkerIdentity.required(request);
        return worker.inTenant(() -> RespInfo.success(checks.check(id, command, worker)));
    }
}
