package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeEditFieldCompilerTest {

    @Test
    void requiredInputKeepsValidationAndFiltersUnknownBasicProps() {
        LowcodeFieldSchema field = field("name", "input");
        field.setLength(40);
        field.setBasicProps(Map.of("clearable", true, "untrustedProperty", "ignored"));
        Map<String, Object> item = RuntimeEditFieldCompiler.buildEditField(field,
                Map.of("required", true, "requiredMessage", "请输入名称"));

        assertEquals(true, item.get("required"));
        assertEquals("请输入名称", item.get("requiredMessage"));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) item.get("props");
        assertEquals(40, props.get("maxlength"));
        assertEquals(true, props.get("clearable"));
        assertFalse(props.containsKey("untrustedProperty"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>) item.get("rules");
        assertEquals("请输入名称", rules.get(0).get("message"));
    }

    @Test
    void formulaFieldStaysReadonlyAndCannotRemainRequired() {
        LowcodeFieldSchema field = field("total", "number");
        field.setRequired(true);
        field.setFormulaConfig(Map.of("expression", "price * count"));
        Map<String, Object> item = RuntimeEditFieldCompiler.buildEditField(field,
                Map.of("rules", List.of(Map.of("required", true, "message", "required"))));

        assertEquals(false, item.get("required"));
        assertEquals(true, item.get("readonly"));
        assertEquals(true, item.get("disabled"));
        assertFalse(item.containsKey("rules"));
    }

    @Test
    void referenceAndDynamicSelectionWriteDisplayCompanionField() {
        LowcodeFieldSchema reference = field("customerId", "objectReference");
        reference.setColumnName("customer_id");
        reference.setReferenceObjectCode("customer");
        Map<String, Object> referenceItem = RuntimeEditFieldCompiler.buildEditField(reference);
        @SuppressWarnings("unchecked")
        Map<String, Object> referenceProps = (Map<String, Object>) referenceItem.get("props");
        assertEquals("customerIdName", referenceProps.get("labelValueField"));

        LowcodeFieldSchema selection = field("ownerId", "select");
        selection.setColumnName("owner_id");
        selection.setBasicProps(Map.of("optionSource", Map.of("type", "REMOTE", "api", "/users")));
        Map<String, Object> selectionItem = RuntimeEditFieldCompiler.buildEditField(selection);
        @SuppressWarnings("unchecked")
        Map<String, Object> selectionProps = (Map<String, Object>) selectionItem.get("props");
        assertEquals("ownerIdName", selectionProps.get("labelValueField"));
        assertTrue(selectionProps.containsKey("optionSource"));
    }

    @Test
    void staticOptionSourceKeepsOptionsForLabelMapping() {
        LowcodeFieldSchema field = field("cooperationStatus", "radioButton");
        field.setBasicProps(Map.of(
                "optionSource", Map.of("type", "STATIC"),
                "options", List.of(
                        Map.of("label", "正常", "value", "1"),
                        Map.of("label", "终止", "value", "2")
                )
        ));
        Map<String, Object> item = RuntimeEditFieldCompiler.buildEditField(field, Map.of(
                "props", Map.of(
                        "optionSource", Map.of("type", "STATIC"),
                        "options", List.of(
                                Map.of("label", "正常", "value", "1"),
                                Map.of("label", "终止", "value", "2")
                        )
                )
        ));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) item.get("props");
        assertTrue(props.containsKey("options"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) props.get("options");
        assertEquals(2, options.size());
        assertEquals("正常", options.get(0).get("label"));
        assertFalse(props.containsKey("labelValueField"));
    }

    @Test
    void dynamicOptionSourceStillDropsStaleStaticOptions() {
        LowcodeFieldSchema field = field("ownerId", "select");
        Map<String, Object> item = RuntimeEditFieldCompiler.buildEditField(field, Map.of(
                "props", Map.of(
                        "optionSource", Map.of("type", "REMOTE", "api", "/users"),
                        "options", List.of(Map.of("label", "选项1", "value", "1"))
                )
        ));
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) item.get("props");
        assertFalse(props.containsKey("options"));
        assertEquals("ownerIdName", props.get("labelValueField"));
    }

    @Test
    void systemFieldCannotBeEditedEvenWhenDesignerMarksRequired() {
        LowcodeFieldSchema field = field("tenantId", "number");
        field.setSystemField(true);
        Map<String, Object> item = RuntimeEditFieldCompiler.buildEditField(field, Map.of("required", true));

        assertEquals(false, item.get("required"));
        assertEquals(true, item.get("readonly"));
        assertEquals(true, item.get("disabled"));
    }

    private static LowcodeFieldSchema field(String name, String componentType) {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(name);
        field.setLabel(name);
        field.setComponentType(componentType);
        return field;
    }
}
