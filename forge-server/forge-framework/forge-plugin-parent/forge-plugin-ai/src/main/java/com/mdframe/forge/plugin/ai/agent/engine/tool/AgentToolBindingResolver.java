package com.mdframe.forge.plugin.ai.agent.engine.tool;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.tool.registry.AgentToolRegistry;
import com.mdframe.forge.plugin.ai.agenttool.domain.AiAgentToolConfig;
import com.mdframe.forge.plugin.ai.agenttool.service.AgentToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 复用原绑定表与知识库兜底；无绑定不代表允许全部 registry 工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentToolBindingResolver {

    private final AgentToolService toolService;
    private final AgentToolRegistry registry;

    public List<AgentTool> resolve(AiAgent agent) {
        Map<String, AgentTool> bound = new LinkedHashMap<>();
        List<AiAgentToolConfig> configs = toolService.listEnabledByAgentId(agent.getId());
        if (configs != null) {
            for (AiAgentToolConfig config : configs) {
                AgentTool tool = registry.getTool(config.getToolSource(), config.getToolKey());
                if (tool != null) {
                    bound.put(config.getToolSource() + ":" + config.getToolKey(), tool);
                } else {
                    log.warn("[AgentBinding] 已跳过未注册工具: agentId={}, source={}, key={}",
                            agent.getId(), config.getToolSource(), config.getToolKey());
                }
            }
        }
        if (!knowledgeIds(agent.getKnowledgeIds()).isEmpty() && !"none".equalsIgnoreCase(agent.getRagMode())) {
            AgentTool rag = registry.getTool("builtin", "rag_search");
            if (rag != null) {
                bound.putIfAbsent("builtin:rag_search", rag);
            }
        }
        return new ArrayList<>(bound.values());
    }

    public static List<Long> knowledgeIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<Long> ids = JSON.parseArray(raw, Long.class);
            return ids == null ? List.of() : List.copyOf(ids);
        } catch (RuntimeException ex) {
            log.warn("[AgentBinding] 知识库绑定无效: exceptionType={}", ex.getClass().getSimpleName());
            return List.of();
        }
    }
}
