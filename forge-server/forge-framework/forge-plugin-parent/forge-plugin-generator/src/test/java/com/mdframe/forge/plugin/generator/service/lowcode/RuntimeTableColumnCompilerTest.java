package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuntimeTableColumnCompilerTest {

    private final RuntimeTableColumnCompiler compiler = new RuntimeTableColumnCompiler(new RuntimeFieldMetadataCompiler());

    @Test
    void dictionaryAndReferenceFieldsKeepTheirRenderProtocol() {
        LowcodeFieldSchema dictionary = field("status", "select");
        dictionary.setDictType("order_status");
        Map<String, Object> dictColumn = compiler.buildTableColumn(dictionary, Map.of(), null, null);
        assertEquals(Map.of("type", "dictTag", "dictType", "order_status"), dictColumn.get("render"));

        LowcodeFieldSchema reference = field("customerId", "objectReference");
        reference.setColumnName("customer_id");
        reference.setReferenceObjectCode("customer");
        Map<String, Object> referenceColumn = compiler.buildTableColumn(reference, Map.of(), null, null);
        assertEquals(Map.of("type", "relationName", "targetField", "customerIdName"), referenceColumn.get("render"));
    }

    @Test
    void switchColumnKeepsValueOverridesAndDefaultPresentation() {
        LowcodeFieldSchema field = field("enabled", "switch");
        field.setBasicProps(Map.of("checkedValue", 1, "uncheckedValue", 0));
        Map<String, Object> column = compiler.buildTableColumn(field,
                Map.of("checkedValue", "Y", "uncheckedValue", "N"), null, null);

        assertEquals(Map.of("type", "switch", "checkedValue", "Y", "uncheckedValue", "N"), column.get("render"));
        assertEquals(100, column.get("width"));
        assertEquals("center", column.get("align"));
    }

    @Test
    void explicitWidthFixedSideAndClickSettingsWin() {
        LowcodeFieldSchema field = field("name", "input");
        field.setWidth(180);
        Map<String, Object> column = compiler.buildTableColumn(field,
                Map.of("width", 260, "fixed", "LEFT", "clickAction", "openPage", "targetPageKey", "detail"),
                null, null);

        assertEquals(260, column.get("width"));
        assertEquals("left", column.get("fixed"));
        assertEquals("openPage", column.get("clickAction"));
        assertEquals("detail", column.get("targetPageKey"));
        assertFalse(column.containsKey("render"));
    }

    @Test
    void staticOptionFieldEmitsStaticOptionsRender() {
        LowcodeFieldSchema field = field("cooperationStatus", "radioButton");
        Map<String, Object> column = compiler.buildTableColumn(field, Map.of(
                "props", Map.of(
                        "optionSource", Map.of("type", "STATIC"),
                        "options", List.of(
                                Map.of("label", "正常", "value", "1"),
                                Map.of("label", "终止", "value", "2")
                        )
                )
        ), null, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> render = (Map<String, Object>) column.get("render");
        assertEquals("staticOptions", render.get("type"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) render.get("options");
        assertEquals(2, options.size());
        assertEquals("正常", options.get(0).get("label"));
    }

    @Test
    void childColumnTitleDropsDuplicatedModelPrefix() {
        LowcodeFieldSchema field = field("child__qty", "number");
        LowcodePageModelRef child = new LowcodePageModelRef();
        child.setModelCode("child");
        child.setModelName("Details");
        child.setPrimary(false);
        child.setFields(List.of(Map.of("sourceField", "qty", "fieldRef", "child__qty")));
        LowcodePageSchema page = new LowcodePageSchema();
        page.setModelRefs(List.of(child));

        Map<String, Object> column = compiler.buildTableColumn(field,
                Map.of("title", "Details · Quantity"), null, page);

        assertEquals("Quantity", column.get("title"));
    }

    private static LowcodeFieldSchema field(String name, String componentType) {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(name);
        field.setLabel(name);
        field.setComponentType(componentType);
        return field;
    }
}
