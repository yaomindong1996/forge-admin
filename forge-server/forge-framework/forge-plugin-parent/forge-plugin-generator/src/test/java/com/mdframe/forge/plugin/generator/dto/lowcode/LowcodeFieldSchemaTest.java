package com.mdframe.forge.plugin.generator.dto.lowcode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LowcodeFieldSchemaTest {

    @Test
    @DisplayName("schema JSON round trip keeps configured permissions without serializing helper flags")
    void jsonRoundTripKeepsProtocolPropertiesOnly() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var field = new LowcodeFieldSchema();
        field.setField("ownerUserId");
        field.setComponentType("userSelect");
        field.setBasicProps(Map.of("multiple", true));
        field.setReadonly(true);
        String encoded = mapper.writeValueAsString(field);
        var node = mapper.readTree(encoded);
        for (String helper : java.util.List.of("referenceField", "selectionLabelField", "multipleSelection")) {
            assertFalse(node.has(helper));
        }
        var restored = mapper.readValue(encoded, LowcodeFieldSchema.class);
        assertEquals("ownerUserId", restored.getField());
        assertEquals(Boolean.TRUE, restored.getReadonly());
        assertTrue(restored.isMultipleSelection());
    }

    @Test
    @DisplayName("legacy derived flags cannot override configured field permissions or selection type")
    void ignoresLegacyDerivedHelperFlags() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var restored = mapper.readValue("""
                {"field":"name","componentType":"input","readonly":true,
                 "referenceField":true,"selectionLabelField":true,"multipleSelection":true}
                """, LowcodeFieldSchema.class);
        assertEquals(Boolean.TRUE, restored.getReadonly());
        assertFalse(restored.isReferenceField());
        assertFalse(restored.isSelectionLabelField());
        assertFalse(restored.isMultipleSelection());
    }

    @Test
    @DisplayName("personnel multi-select switches bigint storage to comma-separated varchar")
    void applyMultipleSelectionStorageConvertsNumericIdsToVarchar() {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("ownerUserId");
        field.setColumnName("owner_user_id");
        field.setComponentType("userSelect");
        field.setDataType("bigint");
        field.setBasicProps(Map.of("multiple", true));

        field.applyMultipleSelectionStorage();

        assertTrue(field.isMultipleSelection());
        assertTrue(field.isSelectionLabelField());
        assertEquals("varchar", field.getDataType());
        assertEquals(LowcodeFieldSchema.MULTI_SELECT_VARCHAR_LENGTH, field.getLength());
    }

    @Test
    @DisplayName("dynamic optionSource select needs companion label column like reference fields")
    void dynamicOptionSourceSelectIsSelectionLabelField() {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("customerId");
        field.setColumnName("customer_id");
        field.setComponentType("select");
        field.setBasicProps(Map.of(
                "optionSource", Map.of(
                        "type", "QUERY_SOURCE",
                        "sourceType", "BUSINESS_OBJECT",
                        "sourceKey", "crm_customer",
                        "valueField", "id",
                        "labelField", "customerName"
                )
        ));

        assertTrue(field.hasDynamicOptionSource());
        assertTrue(field.isSelectionLabelField());
        assertEquals("customer_id_name", field.referenceDisplayColumnName());
        assertEquals("customerIdName", field.referenceDisplayFieldName());
    }

    @Test
    @DisplayName("static option select does not create companion label column")
    void staticOptionSelectIsNotSelectionLabelField() {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("status");
        field.setColumnName("status");
        field.setComponentType("select");
        field.setBasicProps(Map.of("optionSource", Map.of("type", "STATIC")));

        assertTrue(!field.hasDynamicOptionSource());
        assertTrue(!field.isSelectionLabelField());
    }
}
