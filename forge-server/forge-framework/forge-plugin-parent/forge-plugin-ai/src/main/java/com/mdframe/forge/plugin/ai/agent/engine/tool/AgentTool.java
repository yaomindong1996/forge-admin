package com.mdframe.forge.plugin.ai.agent.engine.tool;

import java.util.Map;

/**
 * Agent 工具接口。
 * 所有可被 Agent 调用的工具都实现此接口。
 */
public interface AgentTool {

    /**
     * 工具唯一标识（如 rag_search, http_request, read_skill）
     */
    String getKey();

    /**
     * 工具契约摘要（MCP 工具用于检测远端契约变化）
     */
    default String getExecutionContract() {
        return "";
    }

    /**
     * 工具来源标识（builtin / mcp）
     */
    default String getSource() {
        return "builtin";
    }

    /**
     * 工具来源可读标签
     */
    default String getSourceLabel() {
        return "平台内置";
    }

    /**
     * 工具描述（供 LLM 理解工具用途）
     */
    String getDescription();

    /**
     * 工具参数 JSON Schema（OpenAI Function Calling 格式）
     */
    String getParametersSchema();

    /**
     * 执行工具
     *
     * @param args   工具参数
     * @param context 工具上下文
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> args, ToolContext context);
}
