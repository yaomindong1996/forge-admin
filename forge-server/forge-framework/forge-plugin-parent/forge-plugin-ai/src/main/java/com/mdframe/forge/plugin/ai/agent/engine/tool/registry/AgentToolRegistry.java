package com.mdframe.forge.plugin.ai.agent.engine.tool.registry;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolContributor;
import com.mdframe.forge.plugin.ai.resource.McpResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent 工具注册表。
 * 收集所有 AgentToolContributor 提供的内置工具，并支持动态解析已同步的 MCP 远程工具。
 */
@Slf4j
@Component
public class AgentToolRegistry {

    @Autowired(required = false)
    private ObjectProvider<McpResourceManager> mcpResources;

    private final Map<String, AgentTool> toolMap = new ConcurrentHashMap<>();
    private final Map<String, List<AgentTool>> toolsBySource = new ConcurrentHashMap<>();

    public AgentToolRegistry(List<AgentToolContributor> contributors) {
        for (AgentToolContributor contributor : contributors) {
            String source = contributor.getSource();
            List<AgentTool> tools = contributor.contribute();
            toolsBySource.computeIfAbsent(source, ignored -> new ArrayList<>()).addAll(tools);
            for (AgentTool tool : tools) {
                String key = source + ":" + tool.getKey();
                if (toolMap.putIfAbsent(key, tool) != null) {
                    throw new IllegalStateException("重复的平台工具标识: " + key);
                }
                log.info("[AgentToolRegistry] 注册工具: {} ({})", key, source);
            }
        }
        toolsBySource.replaceAll((source, values) -> List.copyOf(values));
    }

    /**
     * 按 source:key 获取工具（支持内置与 MCP 远程工具）。
     */
    public AgentTool getTool(String source, String key) {
        if ("mcp".equals(source) && mcpResources != null) {
            var manager = mcpResources.getIfAvailable();
            return manager == null ? null : manager.resolve(key);
        }
        return toolMap.get(source + ":" + key);
    }

    /**
     * 按显式租户与用户身份获取运行时工具（避免在后台线程依赖请求上下文）。
     */
    public AgentTool getToolForRuntime(String source, String key, Long tenant, Long actor) {
        if ("mcp".equals(source)) {
            var manager = mcpResources == null ? null : mcpResources.getIfAvailable();
            return manager == null ? null : manager.resolveForRuntime(key, tenant, actor);
        }
        return "builtin".equals(source) ? toolMap.get(source + ":" + key) : null;
    }

    public Map<String, List<AgentTool>> catalog() {
        return Map.copyOf(toolsBySource);
    }

    public List<AgentTool> getAllTools() {
        return new ArrayList<>(toolMap.values());
    }

    public List<AgentTool> getToolsBySource(String source) {
        return toolsBySource.getOrDefault(source, List.of());
    }

    public List<AgentTool> resolve(List<ToolBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return getAllTools();
        }
        return bindings.stream()
                .filter(ToolBinding::isEnabled)
                .map(b -> getTool(b.getToolSource(), b.getToolKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static class ToolBinding {
        private final String toolSource;
        private final String toolKey;
        private final String toolGroup;
        private final boolean enabled;

        public ToolBinding(String toolSource, String toolKey, String toolGroup, boolean enabled) {
            this.toolSource = toolSource;
            this.toolKey = toolKey;
            this.toolGroup = toolGroup;
            this.enabled = enabled;
        }

        public String getToolSource() {
            return toolSource;
        }

        public String getToolKey() {
            return toolKey;
        }

        public String getToolGroup() {
            return toolGroup;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
