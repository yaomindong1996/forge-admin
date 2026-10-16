package com.mdframe.forge.plugin.ai.agent.engine.tool;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 仅声明当前 Agent 的工具；复制具体选项类型，不污染缓存模型，也不安装自动执行 Advisor。 */
public final class AgentToolPromptFactory {

    private AgentToolPromptFactory() {
    }

    public static Prompt create(ReactContext context) {
        List<AgentTool> bound = context.getBoundTools();
        if (!(context.getChatModel().getOptions() instanceof ToolCallingChatOptions defaults)) {
            if (bound == null || bound.isEmpty()) {
                return new Prompt(context.buildMessages());
            }
            throw new BusinessException("当前模型不支持已绑定工具，请检查模型配置");
        }
        List<ToolCallback> callbacks = bound == null || bound.isEmpty() ? List.of() : declarations(bound);
        var options = defaults.mutate().toolCallbacks(callbacks).toolContext(Map.of()).build();
        return new Prompt(context.buildMessages(), options);
    }

    private static List<ToolCallback> declarations(List<AgentTool> bound) {
        List<ToolCallback> callbacks = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (AgentTool tool : bound) {
            if (tool == null || tool.getKey() == null || tool.getKey().isBlank() || !names.add(tool.getKey())) {
                throw new BusinessException("Agent 工具名称无效或重复，请检查绑定配置");
            }
            callbacks.add(new Declaration(tool));
        }
        return callbacks;
    }

    private static final class Declaration implements ToolCallback {
        private final ToolDefinition definition;

        private Declaration(AgentTool tool) {
            definition = ToolDefinition.builder().name(tool.getKey()).description(tool.getDescription())
                    .inputSchema(tool.getParametersSchema()).build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public String call(String input) {
            throw new UnsupportedOperationException("工具只能由 Forge ReAct 权限执行链调用");
        }
    }
}
