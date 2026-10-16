package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.LinkageSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.ViewSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeComponentCatalog;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessObjectDesigner page schema")
class BusinessObjectDesignerPageSchemaTest {

    @Test
    @DisplayName("bridges legacy linkage rules through form governance")
    @SuppressWarnings("unchecked")
    void bridgesLegacyLinkageRulesThroughFormGovernance() throws Exception {
        BusinessObjectLinkagePolicy policy = new BusinessObjectLinkagePolicy(new ObjectMapper());
        FormDesignerSchemaDTO formSchema = new FormDesignerSchemaDTO();
        LinkageSchemaDTO legacy = new LinkageSchemaDTO();
        legacy.setSettings(Map.of("strict", true));
        legacy.setRules(List.of(Map.of(
                "ruleId", "legacy_rule",
                "type", "linkedDict",
                "sourceField", "province",
                "targetField", "city"
        )));

        FormDesignerSchemaDTO hydrated = policy.hydrateFormFieldLinkages(formSchema, legacy);
        Map<String, Object> governance = (Map<String, Object>) hydrated.getSettings().get("governance");
        assertEquals(legacy.getRules(), governance.get("fieldLinkages"));

        Map<String, Object> configuredRule = new LinkedHashMap<>();
        configuredRule.put("ruleId", "application_rule");
        configuredRule.put("type", "clear");
        configuredRule.put("sourceField", "province");
        configuredRule.put("targetField", "city");
        governance.put("fieldLinkages", List.of(configuredRule));

        LinkageSchemaDTO unified = policy.resolveUnifiedLinkageSchema(hydrated, legacy);

        assertEquals("application_rule", unified.getRules().get(0).get("ruleId"));
        assertEquals(Map.of("strict", true), unified.getSettings());
        assertEquals("legacy_rule", legacy.getRules().get(0).get("ruleId"));
    }

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
    @DisplayName("accepts every form designer field component")
    void acceptsEveryFormDesignerFieldComponent() {
        LowcodeSchemaValidator validator = new LowcodeSchemaValidator();

        for (String componentType : LowcodeComponentCatalog.FIELD_COMPONENT_KEYS) {
            LowcodeModelSchema modelSchema = modelSchema();
            modelSchema.setFields(List.of(
                    field("name"),
                    field("componentValue", "component_value", componentType, "varchar")
            ));

            assertDoesNotThrow(() -> validator.validateModel(modelSchema), componentType);
        }
    }

    @Test
    @DisplayName("compiles extended fields and page widgets into runtime form layout")
    @SuppressWarnings("unchecked")
    void compilesExtendedFieldsAndPageWidgetsIntoRuntimeFormLayout() throws Exception {
        List<String> componentTypes = List.of(
                "slider", "rate", "color", "radioButton", "transfer", "customSelect", "daterange",
                "datetimerange", "month", "year", "timerange", "treeSelect", "text"
        );
        List<Map<String, Object>> components = new ArrayList<>();
        Set<String> modelFields = new java.util.LinkedHashSet<>();
        int index = 0;
        for (String componentType : componentTypes) {
            String fieldCode = "field" + index++;
            modelFields.add(fieldCode);
            components.add(Map.of(
                    "id", "cmp_" + fieldCode,
                    "componentKey", componentType,
                    "fieldBinding", Map.of("mode", "field", "fieldCode", fieldCode),
                    "layout", Map.of("span", 1)
            ));
        }
        for (String componentType : LowcodeComponentCatalog.PAGE_WIDGET_COMPONENT_KEYS) {
            components.add(Map.of(
                    "id", "widget_" + componentType,
                    "componentKey", componentType,
                    "fieldBinding", Map.of("mode", "virtual", "fieldCode", ""),
                    "props", Map.of("title", componentType),
                    "layout", Map.of("span", 2)
            ));
        }

        List<Map<String, Object>> layout = new BusinessObjectRuntimeFormProjector(value -> false)
                .buildRuntimeFormLayout(components, modelFields, 2);

        assertEquals(componentTypes.size() + LowcodeComponentCatalog.PAGE_WIDGET_COMPONENT_KEYS.size(), layout.size());
        assertEquals(componentTypes.size(), layout.stream()
                .filter(node -> "field".equals(node.get("nodeType")))
                .count());
        assertEquals(LowcodeComponentCatalog.PAGE_WIDGET_COMPONENT_KEYS.size(), layout.stream()
                .filter(node -> "widget".equals(node.get("nodeType")))
                .filter(node -> LowcodeComponentCatalog.PAGE_WIDGET_COMPONENT_KEYS.contains(node.get("componentKey")))
                .count());
    }

    @Test
    @DisplayName("applies input number component alias defaults")
    void appliesInputNumberComponentAliasDefaults() throws Exception {
        BusinessFieldDTO field = new BusinessFieldDTO();

        fieldDesignPolicy().applyComponentDefaults(field, "input-number");

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
        fieldDesignPolicy().applyComponentDefaults(field, "number");

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
    @DisplayName("preserves designer-authored master-detail options")
    void preservesDesignerAuthoredMasterDetailOptions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        LowcodePageSchema schema = mapper.readValue("""
                {
                  "options": {
                    "masterDetailConfig": {
                      "children": [{"relationKey":"order_items","saveMode":"CASCADE"}]
                    }
                  }
                }
                """, LowcodePageSchema.class);

        Map<?, ?> masterDetailConfig = (Map<?, ?>) schema.getOptions().get("masterDetailConfig");
        List<?> children = (List<?>) masterDetailConfig.get("children");
        assertEquals("order_items", ((Map<?, ?>) children.get(0)).get("relationKey"));
        assertTrue(mapper.writeValueAsString(schema).contains("masterDetailConfig"));
    }

    @Test
    @DisplayName("preserves H5 page sections and bottom bar in form designer payload")
    void preservesH5PageSectionsAndBottomBar() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FormDesignerSchemaDTO schema = mapper.readValue("""
                {
                  "components": [],
                  "pageSections": [{"sectionId":"member","sectionType":"card","fields":["memberPhone"]}],
                  "bottomBar": {"actions":[{"type":"save","label":"保存"}]}
                }
                """, FormDesignerSchemaDTO.class);

        assertEquals("member", schema.getPageSections().get(0).get("sectionId"));
        assertEquals("save", ((Map<?, ?>) ((List<?>) schema.getBottomBar().get("actions")).get(0)).get("type"));
        assertTrue(mapper.writeValueAsString(schema).contains("pageSections"));
    }

    @Test
    @DisplayName("finds child tables inside root, nested and multi-form schemas")
    @SuppressWarnings("unchecked")
    void findsChildTablesAcrossAllFormSchemaShapes() throws Exception {
        Map<String, Object> rootSubTable = Map.of(
                "id", "root_child", "componentKey", "subTable", "props", Map.of("modelCode", "ROOT_ITEM"));
        Map<String, Object> nestedSubTable = Map.of(
                "id", "nested_child", "componentKey", "forgeSubTable", "props", Map.of("modelCode", "NESTED_ITEM"));
        Map<String, Object> multiFormSubTable = Map.of(
                "id", "multi_child", "componentKey", "subTable", "props", Map.of("modelCode", "MULTI_ITEM"));
        List<Map<String, Object>> nodes = List.of(
                rootSubTable,
                Map.of("id", "card", "componentKey", "card", "children", List.of(nestedSubTable)),
                Map.of("formKey", "detail", "schema", Map.of("components", List.of(multiFormSubTable))));

        List<Map<String, Object>> result = new ArrayList<>();
        relationCoordinator().collectSubTableComponents(nodes, result);

        assertEquals(List.of("root_child", "nested_child", "multi_child"),
                result.stream().map(item -> String.valueOf(item.get("id"))).toList());
    }

    @Test
    @DisplayName("loads seed form schema from edit zone and merges it into runtime options")
    void loadsAndMergesSeedFormSchema() throws Exception {
        BusinessObjectDesignerService service = designerService();
        AiBusinessObject object = new AiBusinessObject();
        object.setObjectCode("PS_PRESALE_ORDER");
        object.setObjectName("预售单");
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        LowcodePageZone editZone = zone("edit", List.of("memberPhone"));
        editZone.setProps(new java.util.LinkedHashMap<>(Map.of(
                "formDesignerSchema", Map.of(
                        "components", List.of(),
                        "pageSections", List.of(Map.of("sectionId", "member", "sectionType", "card")),
                        "bottomBar", Map.of("actions", List.of(Map.of("type", "save")))
                )
        )));
        pageSchema.setZones(List.of(editZone));

        FormDesignerSchemaDTO migrated = new BusinessObjectFormSchemaAssembler(new ObjectMapper())
                .resolveFormDesignerSchema(object, modelSchema(), pageSchema, Map.of());

        Method merge = BusinessObjectDesignerService.class.getDeclaredMethod(
                "mergeFormDesignerSchemaIntoRuntimeOptions", String.class, LowcodePageSchema.class);
        merge.setAccessible(true);
        String options = (String) merge.invoke(service,
                "{\"actions\":[{\"actionCode\":\"submit\"}],\"masterDetailConfig\":{\"children\":[]}}",
                pageSchema);
        Map<?, ?> merged = new ObjectMapper().readValue(options, Map.class);

        assertEquals("member", migrated.getPageSections().get(0).get("sectionId"));
        assertTrue(merged.containsKey("actions"));
        assertTrue(merged.containsKey("masterDetailConfig"));
        assertTrue(merged.containsKey("formDesignerSchema"));
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

    @Test
    @DisplayName("keeps existing chinese child tab titles when merging refs")
    void keepsExistingChineseChildTabTitlesWhenMergingRefs() throws Exception {
        LowcodePageModelRef target = new LowcodePageModelRef();
        target.setModelCode("ps_presale_order_item");
        target.setModelName("预售商品明细");
        target.setProps(new java.util.LinkedHashMap<>(Map.of(
                "relationKey", "presale_items",
                "tabTitle", "presale_items",
                "relationName", "presale_items"
        )));

        LowcodePageModelRef existing = new LowcodePageModelRef();
        existing.setModelCode("ps_presale_order_item");
        existing.setModelName("预售商品");
        existing.setProps(new java.util.LinkedHashMap<>(Map.of(
                "relationKey", "presale_items",
                "tabTitle", "预售商品",
                "relationName", "预售商品"
        )));

        relationProjector().mergeExistingPageModelRef(target, existing);

        assertEquals("预售商品", target.getModelName());
        assertEquals("预售商品", target.getProps().get("tabTitle"));
        assertEquals("预售商品", target.getProps().get("relationName"));
        assertEquals("presale_items", target.getProps().get("relationKey"));
    }

    @Test
    @DisplayName("preserves custom search labels when projecting viewSchema onto AiCrudPage")
    void preservesSearchLabelsWhenApplyingViewSchema() throws Exception {
        LowcodeModelSchema modelSchema = modelSchema();
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        Map<String, Object> crud = new LinkedHashMap<>();
        crud.put("id", "block_crud");
        crud.put("blockType", "AiCrudPage");
        crud.put("fieldRefs", new ArrayList<>(List.of("name")));
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("searchFieldRefs", new ArrayList<>(List.of("name")));
        props.put("searchFieldSettings", new LinkedHashMap<>(Map.of(
                "name", new LinkedHashMap<>(Map.of("queryType", "like", "componentType", "input"))
        )));
        crud.put("props", props);
        Map<String, Object> grid = new LinkedHashMap<>();
        grid.put("items", new ArrayList<>(List.of(crud)));
        pageSchema.setListGridLayout(grid);
        pageSchema.setZones(new ArrayList<>(List.of(zone("search", List.of("name")))));

        ViewSchemaDTO viewSchema = new ViewSchemaDTO();
        viewSchema.getSearch().put("fields", List.of(
                Map.of("fieldCode", "name", "label", "主键ID_测", "componentKey", "input",
                        "visible", true, "order", 0, "matchMode", "eq")
        ));

        BusinessObjectViewSchemaProjector projector = new BusinessObjectViewSchemaProjector(
                new ObjectMapper(), field -> "input");
        projector.applyViewSchemaToPageZones(pageSchema, modelSchema, viewSchema);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) pageSchema.getListGridLayout().get("items");
        @SuppressWarnings("unchecked")
        Map<String, Object> nextProps = (Map<String, Object>) items.get(0).get("props");
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) nextProps.get("searchFieldSettings");
        @SuppressWarnings("unchecked")
        Map<String, Object> nameSetting = (Map<String, Object>) settings.get("name");
        assertEquals("主键ID_测", nameSetting.get("label"));
        assertEquals("eq", nameSetting.get("queryType"));
    }

    @Test
    @DisplayName("keeps child list fields while compiling the view schema into page zones")
    void keepsChildListFieldsWhenApplyingViewSchema() throws Exception {
        LowcodeModelSchema modelSchema = modelSchema();
        LowcodePageSchema pageSchema = new LowcodePageSchema();
        LowcodePageModelRef childRef = new LowcodePageModelRef();
        childRef.setModelCode("pw_purchase_order_item");
        childRef.setModelName("采购明细");
        childRef.setPrimary(false);
        childRef.setFields(List.of(Map.of(
                "field", "materialName",
                "sourceField", "materialName",
                "fieldRef", "pw_purchase_order_item__materialName",
                "label", "物料名称"
        )));
        pageSchema.setModelRefs(List.of(childRef));
        pageSchema.setZones(new ArrayList<>(List.of(zone("table", List.of("name")))));

        ViewSchemaDTO viewSchema = new ViewSchemaDTO();
        viewSchema.getList().put("columns", List.of(
                Map.of("fieldCode", "name", "label", "名称", "visible", true, "order", 0),
                Map.of("fieldCode", "pw_purchase_order_item__materialName", "label", "物料名称", "visible", true, "order", 1)
        ));

        BusinessObjectViewSchemaProjector projector = new BusinessObjectViewSchemaProjector(
                new ObjectMapper(), field -> "input");
        ViewSchemaDTO sanitized = projector.sanitizeViewSchemaFieldRefs(viewSchema, modelSchema, pageSchema);
        projector.applyViewSchemaToPageZones(pageSchema, modelSchema, sanitized);

        LowcodePageZone table = pageSchema.getZones().stream()
                .filter(item -> "table".equals(item.getZoneKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("name", "pw_purchase_order_item__materialName"), table.getFieldRefs());
    }

    private LowcodePageSchema ensurePageSchema(LowcodePageSchema pageSchema,
                                               LowcodeModelSchema modelSchema) throws Exception {
        BusinessFieldSchemaService fieldSchemaService = new BusinessFieldSchemaService(
                new LowcodeModelSchemaNormalizer(),
                new BusinessNamingService()
        );
        return new BusinessObjectLegacyPageSchemaAdapter(new ObjectMapper(), fieldSchemaService)
                .ensurePageSchema(pageSchema, modelSchema);
    }

    private LowcodePageSchema resolvePageSchema(AiCrudConfig config, LowcodeModelSchema modelSchema) throws Exception {
        Method method = BusinessObjectDesignerService.class.getDeclaredMethod(
                "resolvePageSchema", AiCrudConfig.class, LowcodeModelSchema.class);
        method.setAccessible(true);
        return (LowcodePageSchema) method.invoke(designerService(), config, modelSchema);
    }

    private LowcodeModelSchema rebuildModelFields(LowcodeModelSchema modelSchema, List<BusinessFieldDTO> fields) throws Exception {
        return fieldDesignPolicy().rebuildModelFields(modelSchema, fields);
    }

    private BusinessObjectFieldDesignPolicy fieldDesignPolicy() {
        BusinessFieldSchemaService fieldSchemaService = new BusinessFieldSchemaService(
                new LowcodeModelSchemaNormalizer(), new BusinessNamingService());
        return new BusinessObjectFieldDesignPolicy(fieldSchemaService, new LowcodeModelSchemaNormalizer());
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
                null,
                null,
                null
        );
    }

    @Test
    @DisplayName("syncs form designer generation onto model fields including snake_case bindings")
    @SuppressWarnings("unchecked")
    void syncsFormDesignerGenerationOntoModelFields() {
        LowcodeModelSchema modelSchema = new LowcodeModelSchema();
        modelSchema.setTableName("biz_customer");
        LowcodeFieldSchema customerCode = field("customerCode", "customer_code", "input", "varchar");
        customerCode.setLabel("客户编码");
        modelSchema.setFields(new ArrayList<>(List.of(customerCode)));

        FormDesignerSchemaDTO formSchema = new FormDesignerSchemaDTO();
        formSchema.setComponents(List.of(Map.of(
                "id", "cmp_code",
                "componentKey", "input",
                "label", "客户编码",
                "fieldBinding", Map.of("mode", "field", "fieldCode", "customer_code"),
                "props", Map.of(
                        "generation", Map.of(
                                "enabled", true,
                                "ruleCode", "customer_code",
                                "trigger", "ON_CREATE",
                                "fillPolicy", "EMPTY_ONLY"
                        )
                ),
                "visibility", Map.of("readonly", true, "hidden", true),
                "layout", Map.of("span", 1)
        )));

        LowcodePageSchema pageSchema = new LowcodePageSchema();
        pageSchema.setZones(new ArrayList<>());
        new BusinessObjectRuntimeFormProjector(value -> false)
                .applyFormDesignerSchemaToEditZone(pageSchema, modelSchema, formSchema);

        LowcodeFieldSchema synced = modelSchema.getFields().stream()
                .filter(item -> "customerCode".equals(item.getField()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> generation = (Map<String, Object>) synced.getBasicProps().get("generation");
        assertEquals(true, generation.get("enabled"));
        assertEquals("customer_code", generation.get("ruleCode"));
        assertTrue(Boolean.TRUE.equals(synced.getReadonly()));

        LowcodePageZone editZone = pageSchema.getZones().stream()
                .filter(zone -> "edit".equals(zone.getZoneKey()))
                .findFirst()
                .orElseThrow();
        Map<String, Object> fieldSettings = (Map<String, Object>) editZone.getProps().get("fieldSettings");
        assertNotNull(fieldSettings.get("customerCode"));
        Map<String, Object> setting = (Map<String, Object>) fieldSettings.get("customerCode");
        Map<String, Object> props = (Map<String, Object>) setting.get("props");
        assertEquals("customer_code", ((Map<?, ?>) props.get("generation")).get("ruleCode"));
    }

    private BusinessObjectRelationCoordinator relationCoordinator() {
        return new BusinessObjectRelationCoordinator(
                new ObjectMapper(), null, null, () -> 1L, ignored -> null, (context, status) -> { });
    }

    private BusinessObjectRelationProjector relationProjector() {
        return new BusinessObjectRelationProjector(
                new ObjectMapper(), null, null, () -> 1L, ignored -> null);
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
