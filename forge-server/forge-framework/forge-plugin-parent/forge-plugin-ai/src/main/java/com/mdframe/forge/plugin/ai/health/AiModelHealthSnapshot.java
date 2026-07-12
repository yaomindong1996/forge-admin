package com.mdframe.forge.plugin.ai.health;
import java.time.Instant;
public record AiModelHealthSnapshot(AiModelHealthStatus status, int failureCount, Instant openedAt, boolean probeInProgress) { }
