package com.mdframe.forge.plugin.ai.routing.constant;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiModelSelectionModeTest {
    @Test void historicalValuesShouldRemainPinned() { assertEquals(AiModelSelectionMode.PINNED, AiModelSelectionMode.fromNullable(null)); assertEquals(AiModelSelectionMode.PINNED, AiModelSelectionMode.fromNullable("  ")); }
    @Test void unknownValueShouldFailClosed() { assertThrows(BusinessException.class, () -> AiModelSelectionMode.fromNullable("AUTO_MAGIC")); }
}
