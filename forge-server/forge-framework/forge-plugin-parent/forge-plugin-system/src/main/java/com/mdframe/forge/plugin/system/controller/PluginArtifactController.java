package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.system.dto.PluginArtifactRegisterDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginArtifactRegistrationService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskQueryService;
import com.mdframe.forge.plugin.system.vo.SysPluginTaskVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/plugin-task")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class PluginArtifactController {
    private final PluginArtifactRegistrationService registrations;
    private final PluginTaskQueryService queries;

    @PostMapping("/{id}/artifact")
    @SaCheckPermission({"system:plugin:artifact:register", "system:plugin:task:detail"})
    @OperationLog(module = "插件中心", type = OperationType.ADD, desc = "登记候选制品",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<SysPluginTaskVO> register(@PathVariable String id,
                                             @Valid @RequestBody PluginArtifactRegisterDTO command) {
        var actor = PluginTaskActor.current();
        registrations.register(id, command, actor);
        return RespInfo.success(queries.detail(id, actor));
    }
}
