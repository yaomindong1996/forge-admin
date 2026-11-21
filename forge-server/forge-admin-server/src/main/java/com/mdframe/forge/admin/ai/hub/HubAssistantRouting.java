package com.mdframe.forge.admin.ai.hub;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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
    private static final Set<String> GENERAL_CODES = Set.of(
            "general_assistant", "general", "default_assistant", "ai_assistant");
    /** 内部结构化生成专用智能体（只输出 JSON/Schema），不作为日常闲聊默认兜底。 */
    private static final Set<String> INTERNAL_GENERATOR_CODES = Set.of(
            "crud_config_builder", "dashboard_generator", "lowcode_app_designer",
            "page_schema_generator", "sql_generator");

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
        Agent byCode = candidates.stream()
                .filter(agent -> GENERAL_CODES.contains(agent.code().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
        if (byCode != null) {
            return byCode;
        }
        Agent byName = candidates.stream()
                .filter(agent -> GENERAL_NAME.matcher(agent.name().strip()).matches())
                .findFirst()
                .orElse(null);
        if (byName != null) {
            return byName;
        }
        // 不把内部结构化生成智能体（如 CRUD配置生成器 / 大屏生成助手）误当成日常聊天默认助手
        return candidates.stream()
                .filter(agent -> !isInternalGenerator(agent))
                .findFirst()
                .orElse(null);
    }

    private boolean isInternalGenerator(Agent agent) {
        String code = agent.code() == null ? "" : agent.code().trim().toLowerCase(Locale.ROOT);
        if (INTERNAL_GENERATOR_CODES.contains(code)) {
            return true;
        }
        String name = agent.name() == null ? "" : agent.name();
        return name.contains("生成器") || name.contains("大屏生成");
    }

    private Route chatOrClarify(Agent agent, Stage stage, String turnId) {
        if (agent == null) {
            return new Route(Kind.CLARIFY, null, null,
                    "当前未配置可用的通用对话智能体，请在上方下拉框选择智能体，或前往「AI能力 → 智能体」启用「通用助手」。",
                    stage, turnId);
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
