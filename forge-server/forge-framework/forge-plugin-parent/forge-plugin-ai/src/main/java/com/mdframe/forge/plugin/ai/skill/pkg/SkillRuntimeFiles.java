package com.mdframe.forge.plugin.ai.skill.pkg;

import com.mdframe.forge.plugin.ai.agent.engine.permission.AgentToolPermissionService;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 运行时技能资料包只读文件读取器（仅允许读取当前智能体已绑定的 READY 技能版本）。
 */
@Component
@RequiredArgsConstructor
public class SkillRuntimeFiles {

    private final SkillPackageManager packages;
    private final AgentToolPermissionService permissions;

    public String read(ToolContext ctx, String code, String requestedPath) {
        if (ctx == null || ctx.getTenantId() == null || ctx.getTenantId() <= 0
                || ctx.getAgentId() == null || ctx.getAgentId() <= 0) {
            throw new BusinessException("读取技能缺少可信执行身份");
        }
        if (!SkillPackageArchive.code(code)) {
            throw new BusinessException("请提供有效的技能编码");
        }
        String path = requestedPath == null || requestedPath.isBlank() ? "SKILL.md" : requestedPath;
        if (path.length() > 240 || path.startsWith("/") || path.contains("\\")
                || List.of(path.split("/", -1)).stream()
                .anyMatch(part -> part.isEmpty() || ".".equals(part) || "..".equals(part))) {
            throw new BusinessException("技能文件路径无效");
        }
        if (permissions.getDecision(ctx.getAgentId(), "read_skill") == PermissionDecision.DENY) {
            throw new BusinessException("此智能体已被禁止读取技能资料");
        }
        return packages.readRuntimeFile(ctx.getTenantId(), ctx.getAgentId(), code, path);
    }
}
