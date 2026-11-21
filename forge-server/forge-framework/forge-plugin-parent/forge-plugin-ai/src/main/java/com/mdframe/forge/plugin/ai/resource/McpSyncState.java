package com.mdframe.forge.plugin.ai.resource;

public enum McpSyncState {
    UNSYNCED, SYNCED, FAILED;

    public String getCode() {
        return name();
    }

    public boolean matches(String value) {
        return name().equals(value);
    }
}
