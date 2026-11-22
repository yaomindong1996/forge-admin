package com.mdframe.forge.plugin.ai.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 超级管理员放行 MCP 出站源（写入平台出站白名单 AI_MCP 场景）。
 */
public record McpAllowOriginDTO(
        @NotBlank @Size(max = 500) String endpoint) {
}
