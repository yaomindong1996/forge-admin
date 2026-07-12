package com.mdframe.forge.plugin.capability.model;

import java.util.List;

public record CapabilityPage(
        List<CapabilityDefinition> items,
        String nextCursor,
        String snapshotVersion) {

    public CapabilityPage {
        items = List.copyOf(items);
        if (snapshotVersion == null || snapshotVersion.isBlank()) {
            throw new IllegalArgumentException("snapshotVersion 不能为空");
        }
    }
}
