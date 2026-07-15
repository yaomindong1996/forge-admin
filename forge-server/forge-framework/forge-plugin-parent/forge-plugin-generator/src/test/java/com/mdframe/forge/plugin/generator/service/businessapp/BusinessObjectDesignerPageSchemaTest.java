package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessObjectDesigner page schema")
class BusinessObjectDesignerPageSchemaTest {

    @Test
    @DisplayName("normalizes blank and alias page zones before validation")
    void normalizesBlankAndAliasPageZonesBeforeValidation() throws Exception {
        LowcodeModelSchema modelSchema = modelSchema();
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        pageSchema.setZones(new ArrayList<>());
        pageSchema.getZones().add(new LowcodePageZone());
        pageSchema.getZones().add(zone("data-table", List.of("name")));
        pageSchema.getZones().add(zone("list", List.of("name")));

        LowcodePageSchema normalized = ensurePageSchema(pageSchema, modelSchema);

        assertEquals(5, normalized.getZones().size());
        LowcodePageZone tableZone = normalized.getZones().stream()
                .filter(zone -> "table".equals(zone.getZoneKey()))
                .findFirst()
                .orElse(null);
        assertNotNull(tableZone);
        assertEquals(List.of("name"), tableZone.getFieldRefs());
        assertDoesNotThrow(() -> new LowcodeSchemaValidator().validatePage(normalized, modelSchema));
    }

    @Test
    @DisplayName("ignores toolbar field refs during page validation")
    void ignoresToolbarFieldRefsDuringValidation() {
        LowcodeModelSchema modelSchema = modelSchema();
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        pageSchema.setZones(List.of(
                zone("table", List.of("name")),
                zone("toolbar", List.of("pw_supplier_material__specModel", "missingActionRef"))
        ));

        assertDoesNotThrow(() -> new LowcodeSchemaValidator().validatePage(pageSchema, modelSchema));
    }

    @Test
    @DisplayName("accepts input number component alias")
    void acceptsInputNumberComponentAlias() {
        LowcodeModelSchema modelSchema = modelSchema();
        modelSchema.setFields(List.of(field("name"), field("amountCent", "amount_cent", "input-number", "bigint")));
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        pageSchema.setZones(List.of(zone("edit", List.of("name", "amountCent"))));

        assertDoesNotThrow(() -> new LowcodeSchemaValidator().validatePage(pageSchema, modelSchema));
    }

    @Test
    @DisplayName("applies input number component alias defaults")
    void appliesInputNumberComponentAliasDefaults() throws Exception {
        BusinessFieldDTO field = new BusinessFieldDTO();
        Method method = BusinessObjectDesignerService.class.getDeclaredMethod(
                "applyComponentDefaults", BusinessFieldDTO.class, String.class);
        method.setAccessible(true);

        method.invoke(designerService(), field, "input-number");

        assertEquals("NUMBER", field.getFieldType());
        assertEquals("int", field.getDataType());
        assertEquals("eq", field.getQueryType());
    }

    @Test
    @DisplayName("preserves imported decimal type when bound to number component")
    void preservesImportedDecimalTypeForNumberComponent() throws Exception {
        BusinessFieldDTO field = new BusinessFieldDTO();
        field.setFieldType("MONEY");
        field.setDataType("decimal");
        field.setLength(18);
        field.setPrecision(2);
        Method method = BusinessObjectDesignerService.class.getDeclaredMethod(
                "applyComponentDefaults", BusinessFieldDTO.class, String.class);
        method.setAccessible(true);

        method.invoke(designerService(), field, "number");

        assertEquals("MONEY", field.getFieldType());
        assertEquals("decimal", field.getDataType());
        assertEquals(18, field.getLength());
        assertEquals(2, field.getPrecision());
        assertEquals("eq", field.getQueryType());
    }

    @Test
    @DisplayName("reads legacy page zone key alias")
    void readsLegacyPageZoneKeyAlias() throws Exception {
        LowcodePageSchema schema = new ObjectMapper().readValue("""
                {"zones":[{"key":"table","type":"data-table","fieldRefs":["name"]}]}
                """, LowcodePageSchema.class);

        LowcodePageZone zone = schema.getZones().get(0);
        assertEquals("table", zone.getZoneKey());
        assertEquals("data-table", zone.getComponentKey());
    }

    @Test
    @DisplayName("preserves field runtime metadata when rebuilding fields")
    void preservesFieldRuntimeMetadataWhenRebuildingFields() throws Exception {
        LowcodeModelSchema modelSchema = modelSchema();
        LowcodeFieldSchema status = field("status", "status", "select", "varchar");
        status.setBusinessFieldType("DICT");
        status.setDictType("pw_common_status");
        status.getBasicProps().put("dictType", "pw_common_status");
        LowcodeFieldSchema purchaseNo = field("purchaseNo", "purchase_no", "input", "varchar");
        purchaseNo.setReadonly(true);
        purchaseNo.getBasicProps().put("generation", Map.of("enabled", true, "ruleCode", "purchase_no"));
        LowcodeFieldSchema warehouseId = field("warehouseId", "warehouse_id", "recordSelector", "bigint");
        warehouseId.setBusinessFieldType("RECORD_SELECTOR");
        warehouseId.getBasicProps().put("recordSelector", Map.of("businessObjectCode", "PW_WAREHOUSE"));
        modelSchema.setFields(List.of(status, purchaseNo, warehouseId));

        LowcodeModelSchema rebuilt = rebuildModelFields(modelSchema, List.of(
                dto("状态", "status", "TEXT", "input"),
                dto("采购单号", "purchaseNo", "TEXT", "input"),
                dto("目标仓库", "warehouseId", "NUMBER", "number")
        ));

        LowcodeFieldSchema rebuiltStatus = findField(rebuilt, "status");
        assertEquals("select", rebuiltStatus.getComponentType());
        assertEquals("pw_common_status", rebuiltStatus.getDictType());
        LowcodeFieldSchema rebuiltPurchaseNo = findField(rebuilt, "purchaseNo");
        assertEquals("purchase_no", ((Map<?, ?>) rebuiltPurchaseNo.getBasicProps().get("generation")).get("ruleCode"));
        LowcodeFieldSchema rebuiltWarehouseId = findField(rebuilt, "warehouseId");
        assertEquals("recordSelector", rebuiltWarehouseId.getComponentType());
        assertEquals("PW_WAREHOUSE", ((Map<?, ?>) rebuiltWarehouseId.getBasicProps().get("recordSelector")).get("businessObjectCode"));
    }

    @Test
    @DisplayName("bridges legacy runtime schemas into page field settings")
    void bridgesLegacyRuntimeSchemasIntoPageFieldSettings() throws Exception {
        AiCrudConfig config = new AiCrudConfig();
        config.setEditSchema("""
                [
                  {"field":"warehouseId","label":"目标仓库","type":"recordSelector","props":{"recordSelector":{"businessObjectCode":"PW_WAREHOUSE"}}},
                  {"field":"orderStatus","label":"状态","type":"select","dictType":"pw_order_status","props":{"dictType":"pw_order_status"}}
                ]
                """);
        config.setSearchSchema("""
                [{"field":"orderStatus","label":"状态","type":"select","dictType":"pw_order_status"}]
                """);
        config.setColumnsSchema("""
                [{"prop":"orderStatus","label":"状态","render":{"type":"dictTag","dictType":"pw_order_status"}}]
                """);
        LowcodeModelSchema modelSchema = modelSchema();
        modelSchema.setFields(List.of(
                field("warehouseId", "warehouse_id", "number", "bigint"),
                field("orderStatus", "order_status", "input", "varchar")
        ));

        LowcodePageSchema pageSchema = resolvePageSchema(config, modelSchema);

        Map<String, Object> editSettings = fieldSettings(pageSchema, "edit");
        assertEquals("recordSelector", ((Map<?, ?>) editSettings.get("warehouseId")).get("componentType"));
        assertEquals("pw_order_status", ((Map<?, ?>) editSettings.get("orderStatus")).get("dictType"));
        Map<String, Object> searchSettings = fieldSettings(pageSchema, "search");
        assertEquals("pw_order_status", ((Map<?, ?>) searchSettings.get("orderStatus")).get("dictType"));
        Map<String, Object> tableSettings = fieldSettings(pageSchema, "table");
        assertEquals("dictTag", ((Map<?, ?>) tableSettings.get("orderStatus")).get("renderType"));
    }

    private LowcodePageSchema ensurePageSchema(LowcodePageSchema pageSchema,
                                               LowcodeModelSchema modelSchema) throws Exception {
        BusinessFieldSchemaService fieldSchemaService = new BusinessFieldSchemaService(
                new LowcodeModelSchemaNormalizer(),
                new BusinessNamingService()
        );
        BusinessObjectDesignerService service = designerService(fieldSchemaService);
        Method method = BusinessObjectDesignerService.class.getDeclaredMethod(
                "ensurePageSchema", LowcodePageSchema.class, LowcodeModelSchema.class);
        method.setAccessible(true);
        return (LowcodePageSchema) method.invoke(service, pageSchema, modelSchema);
    }

    private LowcodePageSchema resolvePageSchema(AiCrudConfig config, LowcodeModelSchema modelSchema) throws Exception {
        Method method = BusinessObjectDesignerService.class.getDeclaredMethod(
                "resolvePageSchema", AiCrudConfig.class, LowcodeModelSchema.class);
        method.setAccessible(true);
        return (LowcodePageSchema) method.invoke(designerService(), config, modelSchema);
    }

    private LowcodeModelSchema rebuildModelFields(LowcodeModelSchema modelSchema, List<BusinessFieldDTO> fields) throws Exception {
        Method method = BusinessObjectDesignerService.class.getDeclaredMethod(
                "rebuildModelFields", LowcodeModelSchema.class, List.class);
        method.setAccessible(true);
        return (LowcodeModelSchema) method.invoke(designerService(), modelSchema, fields);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fieldSettings(LowcodePageSchema pageSchema, String zoneKey) {
        LowcodePageZone zone = pageSchema.getZones().stream()
                .filter(item -> zoneKey.equals(item.getZoneKey()))
                .findFirst()
                .orElseThrow();
        return (Map<String, Object>) zone.getProps().get("fieldSettings");
    }

    private LowcodeFieldSchema findField(LowcodeModelSchema modelSchema, String fieldCode) {
        return modelSchema.getFields().stream()
                .filter(field -> fieldCode.equals(field.getField()))
                .findFirst()
                .orElseThrow();
    }

    private BusinessFieldDTO dto(String label, String fieldCode, String fieldType, String componentType) {
        BusinessFieldDTO dto = new BusinessFieldDTO();
        dto.setFieldName(label);
        dto.setFieldCode(fieldCode);
        dto.setColumnName(fieldCode);
        dto.setFieldType(fieldType);
        dto.setComponentType(componentType);
        dto.setDataType("varchar");
        dto.setListVisible(true);
        dto.setFormVisible(true);
        dto.setSearchable(true);
        return dto;
    }

    private BusinessObjectDesignerService designerService() {
        BusinessFieldSchemaService fieldSchemaService = new BusinessFieldSchemaService(
                new LowcodeModelSchemaNormalizer(),
                new BusinessNamingService()
        );
        return designerService(fieldSchemaService);
    }

    private BusinessObjectDesignerService designerService(BusinessFieldSchemaService fieldSchemaService) {
        return new BusinessObjectDesignerService(
                new ObjectMapper(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new LowcodeModelSchemaNormalizer(),
                new LowcodeSchemaValidator(),
                null,
                null,
                fieldSchemaService,
                null,
                null
        );
    }

    private LowcodePageZone zone(String zoneKey, List<String> fieldRefs) {
        LowcodePageZone zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setFieldRefs(new ArrayList<>(fieldRefs));
        return zone;
    }

    private LowcodeModelSchema modelSchema() {
        LowcodeModelSchema modelSchema = new LowcodeModelSchema();
        modelSchema.setTableMode("EXISTING");
        modelSchema.setAppType("SINGLE");
        modelSchema.setTableName("pw_purchase_order");
        modelSchema.setFields(List.of(field("name")));
        return modelSchema;
    }

    private LowcodeFieldSchema field(String fieldCode) {
        return field(fieldCode, fieldCode, "input", "varchar");
    }

    private LowcodeFieldSchema field(String fieldCode, String columnName, String componentType, String dataType) {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField(fieldCode);
        field.setColumnName(columnName);
        field.setLabel("名称");
        field.setDataType(dataType);
        field.setComponentType(componentType);
        field.setQueryType("like");
        field.setSensitiveType("NONE");
        field.setReadonly(false);
        field.setSystemField(false);
        return field;
    }
}
