package com.mdframe.forge.plugin.ai.health;
import java.util.Optional;
public interface AiModelHealthRegistry {
    AiModelHealthSnapshot snapshot(AiModelHealthKey key);
    Optional<AiModelHealthLease> tryAcquire(AiModelHealthKey key);
    AiModelHealthLease acquireManualProbe(AiModelHealthKey key);
    void reset(AiModelHealthKey key);
    void resetProvider(Long tenantId, Long providerPk);
}
