package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.skill.domain.AiSkill;
import com.mdframe.forge.plugin.ai.skill.domain.AiSkillFile;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillFileMapper;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillMapper;
import com.mdframe.forge.plugin.ai.skill.pkg.SkillRuntimeFiles;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 读取技能资料文件工具。
 * 优先从智能体已绑定的不可变 SkillPackage 只读版本读取，兼容历史 AiSkill 表。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReadSkillTool implements AgentTool {

    private final AiSkillMapper skillMapper;
    private final AiSkillFileMapper skillFileMapper;

    @Autowired(required = false)
    private ObjectProvider<SkillRuntimeFiles> skillRuntimeFiles;

    @Override
    public String getKey() {
        return "read_skill";
    }

    @Override
    public String getDescription() {
        return "读取技能资料包中的文件内容。当需要了解某个已绑定技能的说明文档或参考资料时使用此工具。";
    }

    @Override
    public String getParametersSchema() {
        return """
        {
          "type": "object",
          "properties": {
            "skill_code": {
              "type": "string",
              "description": "技能编码"
            },
            "file_path": {
              "type": "string",
              "description": "技能内文件路径（可选，不传则返回SKILL.md）"
            }
          },
          "required": ["skill_code"]
        }
        """;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        try {
            String skillCode = (String) args.get("skill_code");
            if (skillCode == null || skillCode.isBlank()) {
                return ToolResult.error("技能编码不能为空");
            }
            String targetPath = (String) args.get("file_path");

            SkillRuntimeFiles runtimeFiles = skillRuntimeFiles == null ? null : skillRuntimeFiles.getIfAvailable();
            if (runtimeFiles != null && context != null && context.getAgentId() != null) {
                try {
                    String packagedContent = runtimeFiles.read(context, skillCode, targetPath);
                    if (packagedContent != null) {
                        return ToolResult.text(packagedContent);
                    }
                } catch (BusinessException ex) {
                    return ToolResult.error(ex.getMessage());
                }
            }

            AiSkill skill = skillMapper.selectEnabledByCode(skillCode);
            if (skill == null) {
                return ToolResult.error("技能不存在或已停用: " + skillCode);
            }

            List<AiSkillFile> files = skillFileMapper.selectBySkillId(skill.getId());
            if (files.isEmpty()) {
                return ToolResult.text("技能包为空，无文件内容。");
            }

            if (targetPath != null && !targetPath.isBlank()) {
                AiSkillFile target = files.stream()
                        .filter(f -> targetPath.equals(f.getFilePath()))
                        .findFirst()
                        .orElse(null);
                if (target == null) {
                    return ToolResult.error("文件不存在: " + targetPath);
                }
                return ToolResult.text(target.getFileContent());
            }

            AiSkillFile skillMd = files.stream()
                    .filter(f -> "SKILL.md".equals(f.getFilePath()) || f.getFilePath().endsWith("/SKILL.md"))
                    .findFirst()
                    .orElse(null);

            if (skillMd != null) {
                return ToolResult.text(skillMd.getFileContent());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("技能: ").append(skill.getSkillName()).append(" (").append(skill.getSkillCode()).append(")\n");
            sb.append("描述: ").append(skill.getDescription() != null ? skill.getDescription() : "无").append("\n");
            sb.append("版本: ").append(skill.getVersion()).append("\n\n");
            sb.append("文件列表:\n");
            for (AiSkillFile f : files) {
                sb.append("- ").append(f.getFilePath()).append("\n");
            }
            return ToolResult.text(sb.toString());
        } catch (Exception e) {
            log.warn("[AgentSkill] 读取失败: exceptionType={}", e.getClass().getSimpleName());
            return ToolResult.error("读取技能文件失败，请稍后重试");
        }
    }
}
