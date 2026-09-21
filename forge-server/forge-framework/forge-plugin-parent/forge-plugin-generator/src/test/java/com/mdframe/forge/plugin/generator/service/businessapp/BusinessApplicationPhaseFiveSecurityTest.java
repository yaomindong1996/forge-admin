package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStep;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessSnapshot;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
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
                new ObjectMapper(), null, null, null, null, null, null, null, null, null);

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
        assertEquals(List.of("PRECHECK", "SNAPSHOT", "PROCESSES", "OBJECTS", "ENTRIES",
                        "PAGE_MENUS", "EXTENSIONS", "COMMIT"),
                BusinessApplicationPublishStep.ORDERED_STEPS);
    }

    @Test
    @DisplayName("application snapshot stores structured immutable process versions")
    void snapshotStoresPublishedProcessVersions() {
        BusinessApplicationSnapshotService service = new BusinessApplicationSnapshotService(
                new ObjectMapper(), null, null, null, null, null, null, null, null, null);
        BusinessProcessSnapshot process = new BusinessProcessSnapshot(
                "1900000000000001001",
                "2900000000000001001",
                "leave_submit",
                2,
                5,
                "1.0",
                "a".repeat(64),
                Map.of("schemaVersion", "1.0", "processCode", "leave_submit"),
                Map.of("flowModels", List.of(Map.of(
                        "modelKey", "leave_approval", "deploymentId", "deployment-2"))));

        BusinessApplicationSnapshotService.SnapshotBundle bundle
                = service.finalizeProcesses("{}", List.of(process));

        assertTrue(bundle.json().contains("publishedProcessVersions"));
        assertTrue(bundle.json().contains("2900000000000001001"));
        assertTrue(bundle.json().contains("deployment-2"));
        assertTrue(bundle.snapshot().containsKey("runtimeActions"));
    }

    @Test
    @DisplayName("published snapshot exposes the final business object status")
    @SuppressWarnings("unchecked")
    void publishedSnapshotMarksSelectedObjectsPublished() {
        BusinessApplicationSnapshotService service = new BusinessApplicationSnapshotService(
                new ObjectMapper(), null, null, null, null, null, null, null, null, null);
        String candidate = """
                {
                  "application":{"designStatus":"CHANGED"},
                  "objects":[
                    {"objectId":"101","objectCode":"ORDER","designStatus":"CHANGED"},
                    {"objectId":"102","objectCode":"UNSELECTED","designStatus":"DRAFT"}
                  ]
                }
                """;

        BusinessApplicationSnapshotService.SnapshotBundle bundle = service.finalizePublished(
                candidate, Map.of(101L, 9001L), new BusinessApplicationAssetSelectionVO(),
                3, "PUBLISH");
        List<Map<String, Object>> objects = (List<Map<String, Object>>) bundle.snapshot().get("objects");

        assertEquals("PUBLISHED", objects.get(0).get("designStatus"));
        assertEquals("9001", objects.get(0).get("publishedDesignVersionId"));
        assertEquals("DRAFT", objects.get(1).get("designStatus"));
    }
}
