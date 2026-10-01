package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication debug bundle sanitize/remap")
class BusinessApplicationBundleSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("export sanitize strips nested sensitive keys")
    @SuppressWarnings("unchecked")
    void sanitizeRemovesSensitiveKeys() throws Exception {
        BusinessApplicationBundleExportService service = new BusinessApplicationBundleExportService(
                objectMapper, null, null, null, null);
        Method sanitize = BusinessApplicationBundleExportService.class
                .getDeclaredMethod("sanitize", Object.class);
        sanitize.setAccessible(true);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("applicationName", "demo");
        input.put("clientSecret", "secret-value");
        input.put("options", Map.of(
                "api_key", "must-not-export",
                "safeFlag", true,
                "nested", Map.of("access_token", "tok", "pageTitle", "ok")
        ));

        Map<String, Object> cleaned = (Map<String, Object>) sanitize.invoke(service, input);
        String json = objectMapper.writeValueAsString(cleaned);

        assertFalse(json.contains("secret-value"));
        assertFalse(json.contains("must-not-export"));
        assertFalse(json.contains("tok"));
        assertTrue(json.contains("demo"));
        assertTrue(json.contains("pageTitle"));
        assertEquals(true, ((Map<?, ?>) cleaned.get("options")).get("safeFlag"));
    }

    @Test
    @DisplayName("import validate rejects unknown format")
    void validateRejectsUnknownFormat() throws Exception {
        BusinessApplicationBundleImportService service = new BusinessApplicationBundleImportService(
                objectMapper, null, null, null, null, null, null, null, null);
        Method validate = BusinessApplicationBundleImportService.class
                .getDeclaredMethod("validateBundle", Map.class);
        validate.setAccessible(true);

        Exception error = assertThrows(Exception.class, () -> {
            try {
                validate.invoke(service, Map.of("format", "other", "schemaVersion", 1));
            }
            catch (java.lang.reflect.InvocationTargetException e) {
                throw (Exception) e.getCause();
            }
        });
        assertTrue(error.getMessage().contains("forge-application-bundle")
                || error.getMessage().contains("不是有效"));
    }

    @Test
    @DisplayName("import remaps object codes inside nested JSON trees")
    @SuppressWarnings("unchecked")
    void remapJsonTreeRewritesObjectCodes() throws Exception {
        BusinessApplicationBundleImportService service = new BusinessApplicationBundleImportService(
                objectMapper, null, null, null, null, null, null, null, null);
        Method remap = BusinessApplicationBundleImportService.class
                .getDeclaredMethod("remapJsonTree", Object.class, Map.class);
        remap.setAccessible(true);

        Map<String, String> codeMap = Map.of("order", "order_dbg_ab12", "order_item", "order_item_dbg_ab12");
        Map<String, Object> tree = Map.of(
                "objectCode", "order",
                "nodes", List.of(Map.of("objectRef", "order_item", "title", "明细"))
        );

        Map<String, Object> remapped = (Map<String, Object>) remap.invoke(service, tree, codeMap);
        assertEquals("order_dbg_ab12", remapped.get("objectCode"));
        Map<String, Object> node = (Map<String, Object>) ((List<?>) remapped.get("nodes")).get(0);
        assertEquals("order_item_dbg_ab12", node.get("objectRef"));
        assertEquals("明细", node.get("title"));
    }
}
