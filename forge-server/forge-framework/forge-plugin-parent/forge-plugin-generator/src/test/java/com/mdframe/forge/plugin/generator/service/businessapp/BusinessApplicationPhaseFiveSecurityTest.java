package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication Phase 5 security contract")
class BusinessApplicationPhaseFiveSecurityTest {

    @Test
    @DisplayName("snapshot recursively removes sensitive keys before hashing")
    void snapshotRemovesSensitiveKeys() {
        BusinessApplicationSnapshotService service = new BusinessApplicationSnapshotService(
                new ObjectMapper(), null, null, null, null, null, null, null);

        BusinessApplicationSnapshotService.SnapshotBundle bundle = service.bundle(Map.of(
                "application", Map.of("name", "demo", "clientSecret", "must-not-persist"),
                "bindings", List.of(Map.of("bindingConfig", Map.of(
                        "api_key", "must-not-persist", "configId", "safe-config")))
        ));

        assertFalse(bundle.json().contains("must-not-persist"));
        assertFalse(bundle.json().contains("clientSecret"));
        assertFalse(bundle.json().contains("api_key"));
        assertTrue(bundle.json().contains("safe-config"));
        assertEquals(64, bundle.hash().length());
    }

    @Test
    @DisplayName("publish steps have one deterministic order")
    void deterministicPublishSteps() {
        assertEquals(List.of("PRECHECK", "SNAPSHOT", "OBJECTS", "ENTRIES", "EXTENSIONS", "COMMIT"),
                BusinessApplicationPublishStep.ORDERED_STEPS);
    }
}
