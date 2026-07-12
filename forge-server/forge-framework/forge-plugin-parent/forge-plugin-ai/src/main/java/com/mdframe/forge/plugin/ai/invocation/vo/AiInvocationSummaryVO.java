package com.mdframe.forge.plugin.ai.invocation.vo;
import lombok.Data;
@Data public class AiInvocationSummaryVO { private Long totalCount; private Long successCount; private Long failedCount; private Long usageUnavailableCount; private Long costUnavailableCount; private Long totalPromptTokens; private Long totalCompletionTokens; private Long estimatedCostCent; private Long p95LatencyMs; }
