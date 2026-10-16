package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.skill.domain.AiSkill;
import com.mdframe.forge.plugin.ai.skill.domain.AiSkillFile;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillFileMapper;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 读取技能文件工具。
 * Agent 可通过此工具读取技能包中的文件内容，获取技能知识。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReadSkillTool implements AgentTool {

    private final AiSkillMapper skillMapper;
    private final AiSkillFileMapper skillFileMapper;

    @Override
    public String getKey() {
        return "read_skill";
    }

    @Override
    public String getDescription() {
        return "读取技能包文件内容。当需要了解某个技能的详细说明、使用指南或脚本内容时使用此工具。";
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

            AiSkill skill = skillMapper.selectEnabledByCode(skillCode);
            if (skill == null) {
                return ToolResult.error("技能不存在或已停用: " + skillCode);
            }

            List<AiSkillFile> files = skillFileMapper.selectBySkillId(skill.getId());
            if (files.isEmpty()) {
                return ToolResult.text("技能包为空，无文件内容。");
            }

            String targetPath = (String) args.get("file_path");

            if (targetPath != null && !targetPath.isBlank()) {
                // 读取指定文件
                AiSkillFile target = files.stream()
                        .filter(f -> targetPath.equals(f.getFilePath()))
                        .findFirst()
                        .orElse(null);
                if (target == null) {
                    return ToolResult.error("文件不存在: " + targetPath);
                }
                return ToolResult.text(target.getFileContent());
            }

            // 默认返回 SKILL.md
            AiSkillFile skillMd = files.stream()
                    .filter(f -> "SKILL.md".equals(f.getFilePath()) || f.getFilePath().endsWith("/SKILL.md"))
                    .findFirst()
                    .orElse(null);

            if (skillMd != null) {
                return ToolResult.text(skillMd.getFileContent());
            }

            // 无 SKILL.md，返回文件列表
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
