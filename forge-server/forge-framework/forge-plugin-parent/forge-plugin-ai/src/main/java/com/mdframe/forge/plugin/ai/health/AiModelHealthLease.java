package com.mdframe.forge.plugin.ai.health;
public interface AiModelHealthLease extends AutoCloseable {
    AiModelHealthKey key();
    void success();
    void failure(AiModelFailureCategory category);
    void cancel();
    void abort();
    @Override default void close() { abort(); }
}
