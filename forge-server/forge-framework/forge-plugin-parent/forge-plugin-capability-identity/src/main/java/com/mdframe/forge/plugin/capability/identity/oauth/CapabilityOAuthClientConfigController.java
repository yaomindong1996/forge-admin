package com.mdframe.forge.plugin.capability.identity.oauth;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/capability/oauth/client")
@RequiredArgsConstructor
public class CapabilityOAuthClientConfigController {

    private final CapabilityOAuthClientConfigService configService;

    @PostMapping("/{id}/config")
    @SaCheckPermission("ai:capability:client:rotate")
    @OperationLog(
            module = "AI中枢客户端",
            type = OperationType.UPDATE,
            desc = "配置MCP OAuth客户端")
    public RespInfo<Void> configure(
            @PathVariable Long id,
            @Valid @RequestBody CapabilityOAuthClientConfigRequest request) {
        configService.configure(SessionHelper.getTenantId(), id, request);
        return RespInfo.success();
    }

    @GetMapping("/{id}/redirect-uris")
    @SaCheckPermission("ai:capability:client:query")
    @OperationLog(
            module = "AI中枢客户端",
            type = OperationType.QUERY,
            desc = "查询MCP OAuth回调地址")
    public RespInfo<List<String>> redirectUris(@PathVariable Long id) {
        return RespInfo.success(configService.listRedirectUris(SessionHelper.getTenantId(), id));
    }
}
