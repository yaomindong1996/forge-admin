package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeChildFieldCompilerTest {

    @Test
    void explicitPanelFieldsOverrideEditZoneAndExcludeManagedFields() {
        LowcodePageModelRef ref = childRef();
        ref.setProps(Map.of("childFieldCodes", List.of("quantity", "material")));
        ref.setFields(List.of(
                source("material", Map.of("label", "物料")),
                source("orderId", Map.of()),
                source("id", Map.of()),
                source("quantity", Map.of("label", "数量")),
                source("locked", Map.of("readonly", true)),
                source("archived", Map.of("fieldStatus", "HIDDEN"))));

        List<Map<String, Object>> fields = RuntimeChildFieldCompiler.compile(
                ref, List.of("item__archived"), "orderId", RuntimeChildFieldCompilerTest::render);

        assertEquals(List.of("quantity", "material"), fields.stream().map(item -> item.get("sourceField")).toList());
        assertEquals(List.of("item__quantity", "item__material"),
                fields.stream().map(item -> item.get("fieldRef")).toList());
        assertEquals("item", fields.get(0).get("modelCode"));
        assertEquals("数量", fields.get(0).get("label"));
    }

    @Test
    void explicitPanelKeepsReadonlyFieldAndMarksReadonly() {
        LowcodePageModelRef ref = childRef();
        ref.setProps(Map.of("childFieldCodes", List.of("quantity", "locked")));
        ref.setFields(List.of(
                source("quantity", Map.of("label", "数量")),
                source("locked", Map.of("label", "锁定值", "readonly", true)),
                source("orderId", Map.of())));

        List<Map<String, Object>> fields = RuntimeChildFieldCompiler.compile(
                ref, List.of(), "orderId", RuntimeEditFieldCompiler::buildEditField);

        assertEquals(List.of("quantity", "locked"), fields.stream().map(item -> item.get("sourceField")).toList());
        Map<String, Object> locked = fields.get(1);
        assertEquals(true, locked.get("readonly"));
        assertEquals(true, locked.get("disabled"));
    }

    @Test
    void editZoneSelectionSortsByRefAndPreservesCustomRef() {
        LowcodePageModelRef ref = childRef();
        ref.setFields(List.of(
                source("material", Map.of("fieldRef", "item__material")),
                source("quantity", Map.of("fieldRef", "item__qty")),
                source("orderId", Map.of())));

        List<Map<String, Object>> fields = RuntimeChildFieldCompiler.compile(
                ref, List.of("item__qty", "item__material"), "orderId", RuntimeChildFieldCompilerTest::render);

        assertEquals(List.of("quantity", "material"), fields.stream().map(item -> item.get("sourceField")).toList());
        assertEquals(List.of("item__qty", "item__material"),
                fields.stream().map(item -> item.get("fieldRef")).toList());
    }

    @Test
    void noSelectedChildRefKeepsReadonlyBusinessFieldsInSourceOrder() {
        LowcodePageModelRef ref = childRef();
        ref.setFields(List.of(
                source("material", Map.of()),
                source("locked", Map.of("readonly", true)),
                source("quantity", Map.of()),
                source("orderId", Map.of()),
                source("hidden", Map.of("formVisible", false))));

        List<Map<String, Object>> fields = RuntimeChildFieldCompiler.compile(
                ref, List.of("mainName"), "orderId", RuntimeChildFieldCompilerTest::render);

        assertEquals(List.of("material", "locked", "quantity"),
                fields.stream().map(item -> item.get("sourceField")).toList());
        assertTrue(Boolean.TRUE.equals(fields.get(1).get("readonly")));
    }

    private static LowcodePageModelRef childRef() {
        LowcodePageModelRef ref = new LowcodePageModelRef();
        ref.setModelCode("item");
        ref.setModelName("明细");
        return ref;
    }

    private static Map<String, Object> source(String field, Map<String, Object> overrides) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("sourceField", field);
        source.put("columnName", field);
        source.putAll(overrides);
        return source;
    }

    private static Map<String, Object> render(LowcodeFieldSchema field) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("field", field.getField());
        item.put("label", field.getLabel());
        if (Boolean.TRUE.equals(field.getReadonly())) {
            item.put("readonly", true);
        }
        return item;
    }
}
