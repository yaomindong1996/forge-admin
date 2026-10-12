package com.mdframe.forge.plugin.ai.agent.engine.event;

/**
 * Agent 事件类型枚举（含直接订阅错误与自定义扩展事件）。
 */
public enum AgentEventType {

    AGENT_START("AGENT_START"),
    AGENT_END("AGENT_END"),
    AGENT_RESULT("AGENT_RESULT"),
    /** 本次直接订阅失败提示；失去占用时不得发布给全局持久化监听器。 */
    ERROR("ERROR"),

    MODEL_CALL_START("MODEL_CALL_START"),
    MODEL_CALL_END("MODEL_CALL_END"),

    TEXT_BLOCK_START("TEXT_BLOCK_START"),
    TEXT_BLOCK_DELTA("TEXT_BLOCK_DELTA"),
    TEXT_BLOCK_END("TEXT_BLOCK_END"),

    THINKING_BLOCK_START("THINKING_BLOCK_START"),
    THINKING_BLOCK_DELTA("THINKING_BLOCK_DELTA"),
    THINKING_BLOCK_END("THINKING_BLOCK_END"),

    DATA_BLOCK_START("DATA_BLOCK_START"),
    DATA_BLOCK_DELTA("DATA_BLOCK_DELTA"),
    DATA_BLOCK_END("DATA_BLOCK_END"),

    TOOL_CALL_START("TOOL_CALL_START"),
    TOOL_CALL_DELTA("TOOL_CALL_DELTA"),
    TOOL_CALL_END("TOOL_CALL_END"),

    TOOL_RESULT_START("TOOL_RESULT_START"),
    TOOL_RESULT_TEXT_DELTA("TOOL_RESULT_TEXT_DELTA"),
    TOOL_RESULT_DATA_DELTA("TOOL_RESULT_DATA_DELTA"),
    TOOL_RESULT_END("TOOL_RESULT_END"),

    EXCEED_MAX_ITERS("EXCEED_MAX_ITERS"),
    REQUEST_STOP("REQUEST_STOP"),

    REQUIRE_USER_CONFIRM("REQUIRE_USER_CONFIRM"),
    USER_CONFIRM_RESULT("USER_CONFIRM_RESULT"),

    SUBAGENT_EXPOSED("SUBAGENT_EXPOSED"),
    HINT_BLOCK("HINT_BLOCK"),
    ALL_TOOLS_DENIED("ALL_TOOLS_DENIED"),

    /**
     * 持久化元信息（流首下发）：携带本轮落库的 user/assistant recordId，
     * 供前端在未重载会话时即可将 live 消息关联到 DB 行，支撑重新生成/编辑重发/单条删除。
     * 由 {@code AgentEngineService} 在请求线程 concat 进 SSE 流首，不经过 eventPublisher，
     * 因此持久化监听器不会收到它。
     */
    PERSIST_META("PERSIST_META"),

    CUSTOM("CUSTOM");

    private final String code;

    AgentEventType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 按 code 查找枚举值，不存在返回 null。
     */
    public static AgentEventType fromCode(String code) {
        if (code == null) return null;
        for (AgentEventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
