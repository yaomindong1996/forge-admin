package com.mdframe.forge.plugin.capability.secureaction.catalog;

import java.util.List;

public record SecureActionSearchResult(
        List<SecureActionDescriptor> items,
        boolean hasMore) {
}
