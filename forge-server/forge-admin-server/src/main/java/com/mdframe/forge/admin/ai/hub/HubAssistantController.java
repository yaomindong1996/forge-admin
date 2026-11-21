package com.mdframe.forge.admin.ai.hub;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社区版全局悬浮 AI 助手接口（支持通用/智能体对话路由、智能体目录查询、我的待办只读查询）。
 */
@RestController
@RequestMapping("/ai/hub")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class HubAssistantController {

    private final HubAccess access;
    private final HubAssistantRouting routing;
    private final HubFlowTodoGateway todoGateway;

    @PostMapping("/route")
    @SaCheckPermission(value = {
            HubAccess.PERM_HUB_USE,
            HubAccess.PERM_ENGINE_STREAM,
            HubAccess.PERM_AGENT_LIST,
            HubAccess.PERM_TODO_LIST
    }, mode = SaMode.OR)
    public RespInfo<HubAssistantRouting.Route> route(@Valid @RequestBody RouteRequest request) {
        var actor = access.current();
        return RespInfo.success(routing.route(actor, request.message().strip(), request.previousAgentCode()));
    }

    @GetMapping("/agents")
    @SaCheckPermission(value = {
            HubAccess.PERM_HUB_USE,
            HubAccess.PERM_ENGINE_STREAM,
            HubAccess.PERM_AGENT_LIST
    }, mode = SaMode.OR)
    public RespInfo<List<HubAssistantRouting.Agent>> agents() {
        return RespInfo.success(routing.catalog());
    }

    @GetMapping("/todos")
    @SaCheckPermission(value = {
            HubAccess.PERM_HUB_USE,
            HubAccess.PERM_TODO_LIST
    }, mode = SaMode.OR)
    public RespInfo<HubFlowTodoGateway.TodoPage> todos(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return RespInfo.success(todoGateway.list(access.current(), pageNum, pageSize));
    }

    public record RouteRequest(
            @NotBlank @Size(max = 8000) String message,
            @Size(max = 128) String previousAgentCode) {
    }
}
