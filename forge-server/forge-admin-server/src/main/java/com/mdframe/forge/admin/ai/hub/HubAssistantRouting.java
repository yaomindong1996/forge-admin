package com.mdframe.forge.admin.ai.hub;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 社区版全局 AI 助手路由：在通用助手、已启用业务智能体与我的待办只读查询之间路由。
 */
@Component
@RequiredArgsConstructor
class HubAssistantRouting {

    private static final Pattern GENERAL_NAME = Pattern.compile(
            "^(通用|默认|全能|综合)(AI|智能)?(助手|助理|智能体)$|^(general|default) (ai )?assistant$",
            Pattern.CASE_INSENSITIVE);

    private final HubAccess access;
    private final AiAgentMapper agentMapper;
    private final HubBindingLookup bindings;

    Route route(HubAccess.Actor actor, String message, String previousAgentCode) {
        String turnId = UUID.randomUUID().toString();
        if (HubIntent.route(message) == HubIntent.MY_TODOS) {
            return new Route(Kind.TODO, HubIntent.MY_TODOS, null, null, Stage.RULE, turnId);
        }
        if (!actor.can(HubAccess.PERM_ENGINE_STREAM) && !actor.can(HubAccess.PERM_HUB_USE)) {
            return new Route(Kind.CLARIFY, null, null,
                    "当前账号尚未开通智能体对话权限，请联系管理员授权。", Stage.FALLBACK, turnId);
        }
        List<Agent> candidates = list(actor);
        return routeChat(actor, message, previousAgentCode, candidates, turnId);
    }

    List<Agent> catalog() {
        return list(access.current());
    }

    private List<Agent> list(HubAccess.Actor actor) {
        List<AiAgent> rows = agentMapper.selectEnabledAgents();
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(a -> Objects.equals(a.getTenantId(), actor.tenantId()))
                .filter(a -> a.getId() != null && StringUtils.hasText(a.getAgentCode()))
                .map(this::describe)
                .sorted(Comparator.comparing(Agent::name).thenComparing(Agent::code))
                .toList();
    }

    private Agent describe(AiAgent agent) {
        String name = StringUtils.hasText(agent.getAgentName()) ? agent.getAgentName() : agent.getAgentCode();
        return new Agent(agent.getId().toString(), agent.getAgentCode(), name, agent.getDescription());
    }

    private Route routeChat(HubAccess.Actor actor, String message, String previousCode,
                            List<Agent> candidates, String turnId) {
        Agent general = resolveGeneral(actor.tenantId(), candidates);
        if (HubIntent.greeting(message)) {
            return chatOrClarify(general, Stage.RULE, turnId);
        }
        List<Agent> specialists = candidates.stream()
                .filter(candidate -> !candidate.equals(general))
                .toList();
        Agent matched = HubAgentMatch.best(message, specialists);
        if (matched != null) {
            return new Route(Kind.CHAT, HubIntent.CHAT, matched, null, Stage.AGENT_MATCH, turnId);
        }
        if (StringUtils.hasText(previousCode)) {
            Agent previous = candidates.stream()
                    .filter(agent -> agent.code().equals(previousCode))
                    .findFirst()
                    .orElse(general);
            return chatOrClarify(previous, Stage.FALLBACK, turnId);
        }
        return chatOrClarify(general, Stage.FALLBACK, turnId);
    }

    private Agent resolveGeneral(Long tenantId, List<Agent> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        String configured = bindings.findGeneralAgentCode(tenantId);
        if (StringUtils.hasText(configured)) {
            Agent byConfig = candidates.stream()
                    .filter(agent -> agent.code().equals(configured))
                    .findFirst()
                    .orElse(null);
            if (byConfig != null) {
                return byConfig;
            }
        }
        return candidates.stream()
                .filter(agent -> GENERAL_NAME.matcher(agent.name().strip()).matches())
                .findFirst()
                .orElse(candidates.get(0));
    }

    private Route chatOrClarify(Agent agent, Stage stage, String turnId) {
        if (agent == null) {
            return new Route(Kind.CLARIFY, null, null,
                    "当前租户尚未启用任何智能体，请先在「AI能力 → 智能体」中创建并启用。", stage, turnId);
        }
        return new Route(Kind.CHAT, HubIntent.CHAT, agent, null, stage, turnId);
    }

    enum Kind { CHAT, TODO, CLARIFY }

    enum Stage { RULE, AGENT_MATCH, FALLBACK }

    public record Agent(String id, String code, String name, String description) {
    }

    public record Route(Kind kind, HubIntent intent, Agent agent, String notice, Stage stage, String turnId) {
    }
}
