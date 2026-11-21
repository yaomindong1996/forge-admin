package com.mdframe.forge.plugin.ai.resource.dto;

import jakarta.validation.constraints.Min;

public record McpConnectionActionDTO(@Min(1) int expectedRowVersion) {
}
