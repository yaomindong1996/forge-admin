package com.mdframe.forge.plugin.ai.resource;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import com.mdframe.forge.plugin.ai.resource.domain.McpConnection;
import com.mdframe.forge.plugin.ai.resource.mapper.McpResourceMapper;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/ai/resource")
@RequiredArgsConstructor
public class ResourceCatalogController {

    private final AgentToolRegistry registry;
    private final McpResourceMapper mapper;

    @GetMapping("/tools")
    @SaCheckPermission("ai:agent:tool:list")
    public RespInfo<ResourceViews.Catalog> tools() {
        Long tenant = McpResourceManager.tenant();
        List<ResourceViews.Tool> result = new ArrayList<>();
        registry.catalog().forEach((source, tools) -> tools.forEach(tool -> result.add(new ResourceViews.Tool(
                source, sourceLabel(source), tool.getKey(), title(tool.getKey()), tool.getDescription(),
                tool.getParametersSchema(), null, "平台内置", null, true,
                mapper.bindingCount(tenant, source, tool.getKey())))));
        // 智能体装配与 MCP 目录共用本接口；具备工具列表或 MCP 查询权限即可看到已同步工具
        if (SessionHelper.hasPermission("ai:mcp:connection:list")
                || SessionHelper.hasPermission("ai:agent:tool:list")) {
            var connections = new HashMap<Long, McpConnection>();
            mapper.connections(tenant).forEach(row -> connections.put(row.getId(), row));
            for (var tool : mapper.tools(tenant, null)) {
                var row = connections.get(tool.getConnectionId());
                if (row == null) {
                    continue;
                }
                String label = row.getConnectionName() == null || row.getConnectionName().isBlank()
                        ? "MCP" : row.getConnectionName();
                result.add(new ResourceViews.Tool(
                        "mcp", label, tool.getToolKey(),
                        tool.getTitle() == null || tool.getTitle().isBlank() ? tool.getRemoteName() : tool.getTitle(),
                        tool.getDescription(), tool.getInputSchema(),
                        row.getId(), row.getConnectionName(), tool.getContractHash(),
                        McpResourceManager.available(row, tool),
                        mapper.bindingCount(tenant, "mcp", tool.getToolKey())));
            }
        }
        result.sort(Comparator.comparing(ResourceViews.Tool::source)
                .thenComparing(t -> t.title() == null ? "" : t.title()));
        return RespInfo.success(new ResourceViews.Catalog(result));
    }

    private String sourceLabel(String source) {
        return switch (source) {
            case "builtin" -> "内置工具";
            case "capability" -> "能力平台";
            case "mcp" -> "MCP";
            default -> source;
        };
    }

    private String title(String key) {
        return switch (key) {
            case "rag_search" -> "知识库检索";
            case "read_skill" -> "读取技能资料";
            case "http_request" -> "HTTP接口调用";
            default -> key;
        };
    }
}
