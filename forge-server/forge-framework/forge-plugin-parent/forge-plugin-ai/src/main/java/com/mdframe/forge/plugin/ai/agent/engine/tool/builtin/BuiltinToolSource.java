package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolContributor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 社区版内置工具源：注册知识库检索、HTTP 接口调用与技能资料读取。
 */
@Component
@RequiredArgsConstructor
public class BuiltinToolSource implements AgentToolContributor {

    private final RagSearchTool ragSearchTool;
    private final HttpTool httpTool;
    private final ReadSkillTool readSkillTool;

    @Override
    public String getSource() {
        return "builtin";
    }

    @Override
    public List<AgentTool> contribute() {
        return List.of(ragSearchTool, httpTool, readSkillTool);
    }
}
