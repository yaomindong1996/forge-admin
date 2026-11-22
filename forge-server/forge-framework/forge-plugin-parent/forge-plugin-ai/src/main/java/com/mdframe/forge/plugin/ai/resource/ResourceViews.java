package com.mdframe.forge.plugin.ai.resource;

import java.time.LocalDateTime;
import java.util.List;

public final class ResourceViews {

    private ResourceViews() {
    }

    public record SyncedTool(String toolKey, String title, String description) {
    }

    public record Connection(Long id, String connectionCode, String connectionName, String endpoint,
                             boolean secretConfigured, int timeoutSeconds, int enabled, int rowVersion,
                             String syncState, String errorCode, LocalDateTime syncedAt,
                             int toolCount, boolean destinationAllowed, List<SyncedTool> tools) {
    }

    public record Tool(String source, String sourceLabel, String toolKey, String title, String description,
                       String inputSchema, Long connectionId, String connectionName, String contractHash,
                       boolean available, int bindingCount) {
    }

    public record Catalog(List<Tool> tools) {
    }
}
