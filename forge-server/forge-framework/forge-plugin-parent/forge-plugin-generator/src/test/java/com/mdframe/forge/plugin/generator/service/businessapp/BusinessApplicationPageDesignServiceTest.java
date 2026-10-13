package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPageDesignDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPageDesignVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BusinessApplicationPageDesignServiceTest {

    private static final Long APPLICATION_ID = 11L;
    private static final Long OBJECT_ID = 21L;

    @Mock
    private BusinessApplicationService applicationService;
    @Mock
    private BusinessApplicationObjectService applicationObjectService;
    @Mock
    private BusinessObjectService objectService;
    @Mock
    private BusinessObjectCreateService objectCreateService;
    @Mock
    private BusinessObjectDesignerService designerService;
    @Mock
    private BusinessNamingService namingService;
    @Mock
    private IGenDatasourceService datasourceService;
    @Mock
    private BusinessObjectTableMappingService tableMappingService;
    @Mock
    private LowcodeDdlService ddlService;
    @Mock
    private LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    @Mock
    private DynamicCrudRepository dynamicCrudRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private BusinessApplicationPageDesignService service;

    @BeforeEach
    void setUp() {
        service = new BusinessApplicationPageDesignService(
                new ObjectMapper(),
                applicationService,
                applicationObjectService,
                objectService,
                objectCreateService,
                designerService,
                namingService,
                datasourceService,
                tableMappingService,
                ddlService,
                runtimeDataSourceResolver,
                dynamicCrudRepository,
                transactionManager);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(namingService.normalizeObjectCode(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(namingService.normalizeFieldCode(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(namingService.camelToSnake(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("new page metadata commits before managed database synchronization")
    void newPageCommitsMetadataBeforeDatabaseSync() {
        BusinessApplicationPageDesignDTO request = designRequest();
        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of());
        when(datasourceService.selectEnabledDatasources("LOWCODE_RUNTIME")).thenReturn(List.of(runtimeDatasource()));
        when(objectCreateService.create(any())).thenReturn(OBJECT_ID);
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        BusinessApplicationPageDesignVO result = service.save(APPLICATION_ID, request);

        assertEquals(OBJECT_ID, result.getObjectId());
        assertTrue(result.getObjectCreated());
        assertFalse(result.getHasBusinessData());
        assertEquals("customer", objectRef(result.getBuilder()).get("objectCode"));
        assertEquals("cfg_customer", objectRef(result.getBuilder()).get("configKey"));

        ArgumentCaptor<List<BusinessApplicationObjectDTO>> associations = ArgumentCaptor.forClass(List.class);
        verify(applicationObjectService).replace(eq(APPLICATION_ID), associations.capture());
        Map<String, Object> associationOptions = JSON.parseObject(
                associations.getValue().get(0).getOptions(), Map.class);
        assertEquals(Boolean.FALSE, associationOptions.get("hiddenFromPrimaryFlow"));

        InOrder order = inOrder(transactionManager, tableMappingService);
        order.verify(transactionManager).commit(any());
        order.verify(tableMappingService).syncManagedDatabase(OBJECT_ID, APPLICATION_ID, "form-1");
    }

    @Test
    @DisplayName("new page uses the selected runtime datasource instead of the default")
    void newPageUsesSelectedRuntimeDatasource() {
        BusinessApplicationPageDesignDTO request = designRequest();
        request.setRuntimeDatasourceId(32L);
        GenDatasource defaultDatasource = runtimeDatasource();
        GenDatasource selected = runtimeDatasource();
        selected.setDatasourceId(32L);
        selected.setDatasourceCode("runtime_selected");
        selected.setDatasourceName("业务库");
        selected.setIsDefault(0);
        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of());
        when(datasourceService.selectEnabledDatasources("LOWCODE_RUNTIME"))
                .thenReturn(List.of(defaultDatasource, selected));
        when(objectCreateService.create(any())).thenReturn(OBJECT_ID);
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        service.save(APPLICATION_ID, request);

        ArgumentCaptor<BusinessObjectDTO> created = ArgumentCaptor.forClass(BusinessObjectDTO.class);
        verify(objectCreateService).create(created.capture());
        assertEquals(32L, created.getValue().getRuntimeDatasourceId());
        assertEquals("BLANK", created.getValue().getCreateMode());
    }

    @Test
    @DisplayName("importing an existing table binds DB_IMPORT and skips automatic create-table")
    void importExistingTableSkipsCreateTableSync() {
        BusinessApplicationPageDesignDTO request = designRequest();
        request.setRuntimeDatasourceId(31L);
        request.setCreateMode("DB_IMPORT");
        request.setImportDatasourceId(31L);
        request.setImportTableName("crm_customer");
        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of());
        when(datasourceService.selectEnabledDatasources("LOWCODE_RUNTIME")).thenReturn(List.of(runtimeDatasource()));
        when(objectCreateService.create(any())).thenReturn(OBJECT_ID);
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        service.save(APPLICATION_ID, request);

        ArgumentCaptor<BusinessObjectDTO> created = ArgumentCaptor.forClass(BusinessObjectDTO.class);
        verify(objectCreateService).create(created.capture());
        assertEquals("DB_IMPORT", created.getValue().getCreateMode());
        assertEquals(31L, created.getValue().getImportDatasourceId());
        assertEquals("crm_customer", created.getValue().getImportTableName());
        verify(tableMappingService, never()).syncManagedDatabase(eq(OBJECT_ID), eq(APPLICATION_ID), eq("form-1"));
    }

    @Test
    @DisplayName("a newly created object with only system fields can still save page fields")
    void newObjectWithSystemFieldsOnlySkipsCreateTableProbe() {
        BusinessApplicationPageDesignDTO request = designRequest();
        LowcodeModelSchema blankModel = new LowcodeModelSchema();
        blankModel.setTableName("biz_business_object");
        LowcodeFieldSchema idField = new LowcodeFieldSchema();
        idField.setField("id");
        idField.setColumnName("id");
        idField.setSystemField(true);
        blankModel.setFields(List.of(idField));
        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of());
        when(datasourceService.selectEnabledDatasources("LOWCODE_RUNTIME")).thenReturn(List.of(runtimeDatasource()));
        when(objectCreateService.create(any())).thenReturn(OBJECT_ID);
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(blankModel));
        lenient().when(ddlService.previewCreateTable(any())).thenThrow(
                new BusinessException("数据模型至少需要一个业务字段"));

        BusinessApplicationPageDesignVO result = service.save(APPLICATION_ID, request);

        assertEquals(OBJECT_ID, result.getObjectId());
        assertTrue(result.getObjectCreated());
        assertFalse(result.getHasBusinessData());
        verify(ddlService, never()).previewCreateTable(any());
        verify(designerService).saveDesigner(eq(OBJECT_ID), any());
        verify(tableMappingService).syncManagedDatabase(OBJECT_ID, APPLICATION_ID, "form-1");
    }

    @Test
    @DisplayName("existing data locks form fields and preserves application object options")
    void existingDataLocksFieldsAndPreservesAssociationOptions() {
        BusinessApplicationPageDesignDTO request = designRequest();
        request.setObjectId(OBJECT_ID);
        request.getBuilder().put("formAssets", List.of(Map.of(
                "id", "form-1",
                "formDesignerSchema", Map.of("schemaVersion", "stale"))));
        BusinessApplicationObjectVO association = new BusinessApplicationObjectVO();
        association.setObjectId(OBJECT_ID);
        association.setObjectRole("PRIMARY");
        association.setSortOrder(0);
        association.setOptions("{\"customPermission\":\"keep\"}");
        LowcodeModelSchema modelSchema = new LowcodeModelSchema();
        modelSchema.setTableName("biz_customer");
        modelSchema.setFields(List.of(existingTextField()));
        LowcodeDdlPreviewVO preview = new LowcodeDdlPreviewVO();
        preview.setTableExists(true);
        LowcodeRuntimeDataSourceContext runtimeContext = LowcodeRuntimeDataSourceContext.master("biz_customer");

        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of(association));
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(modelSchema));
        when(ddlService.previewCreateTable(modelSchema)).thenReturn(preview);
        when(runtimeDataSourceResolver.resolve(modelSchema)).thenReturn(runtimeContext);
        when(dynamicCrudRepository.countList(eq("biz_customer"), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        BusinessApplicationPageDesignVO result = service.save(APPLICATION_ID, request);

        assertTrue(result.getHasBusinessData());
        assertEquals(Boolean.TRUE, resultFieldBinding(result.getBuilder()).get("locked"));
        assertEquals("form-first-v1", resultFormSchema(result.getBuilder()).get("schemaVersion"));

        ArgumentCaptor<BusinessObjectDesignerDTO> designer = ArgumentCaptor.forClass(BusinessObjectDesignerDTO.class);
        verify(designerService).saveDesigner(eq(OBJECT_ID), designer.capture());
        assertEquals(Boolean.TRUE, componentFieldBinding(designer.getValue().getFormDesignerSchema()).get("locked"));

        ArgumentCaptor<List<BusinessApplicationObjectDTO>> associations = ArgumentCaptor.forClass(List.class);
        verify(applicationObjectService).replace(eq(APPLICATION_ID), associations.capture());
        Map<String, Object> associationOptions = JSON.parseObject(
                associations.getValue().get(0).getOptions(), Map.class);
        assertEquals("keep", associationOptions.get("customPermission"));
        assertEquals(Boolean.FALSE, associationOptions.get("hiddenFromPrimaryFlow"));
    }

    @Test
    @DisplayName("snake_case and camelCase field twins are merged after normalizeFieldCode")
    void mergesSnakeAndCamelFieldCodeTwins() {
        BusinessApplicationPageDesignDTO request = designRequest();
        BusinessFieldDTO camel = textField();
        camel.setFieldCode("entName");
        camel.setColumnName("ent_name");
        camel.setFieldName("企业全称");
        BusinessFieldDTO snake = textField();
        snake.setFieldCode("ent_name");
        snake.setColumnName("ent_name");
        snake.setFieldName("企业全称");
        BusinessFieldDTO creditCamel = textField();
        creditCamel.setFieldCode("unifiedCreditCode");
        creditCamel.setColumnName("unified_credit_code");
        creditCamel.setFieldName("统一社会信用代码");
        BusinessFieldDTO creditSnake = textField();
        creditSnake.setFieldCode("unified_credit_code");
        creditSnake.setColumnName("unified_credit_code");
        creditSnake.setFieldName("统一社会信用代码");
        request.setFields(List.of(camel, snake, creditCamel, creditSnake));

        when(namingService.normalizeFieldCode(any(), any())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            if ("ent_name".equals(value) || "entName".equals(value)) {
                return "entName";
            }
            if ("unified_credit_code".equals(value) || "unifiedCreditCode".equals(value)) {
                return "unifiedCreditCode";
            }
            return value;
        });
        when(namingService.camelToSnake(any())).thenAnswer(invocation -> {
            Object argument = invocation.getArgument(0);
            String value = argument == null ? "" : String.valueOf(argument);
            if ("entName".equals(value)) {
                return "ent_name";
            }
            if ("unifiedCreditCode".equals(value)) {
                return "unified_credit_code";
            }
            return value;
        });
        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of());
        when(datasourceService.selectEnabledDatasources("LOWCODE_RUNTIME")).thenReturn(List.of(runtimeDatasource()));
        when(objectCreateService.create(any())).thenReturn(OBJECT_ID);
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        service.save(APPLICATION_ID, request);

        ArgumentCaptor<BusinessObjectDesignerDTO> designer = ArgumentCaptor.forClass(BusinessObjectDesignerDTO.class);
        verify(designerService).saveDesigner(eq(OBJECT_ID), designer.capture());
        List<BusinessFieldDTO> savedFields = designer.getValue().getFields();
        assertEquals(List.of("entName", "unifiedCreditCode"),
                savedFields.stream().map(BusinessFieldDTO::getFieldCode).toList());
    }

    @Test
    @DisplayName("a data page must contain the submitted form asset and page binding")
    void rejectsBuilderWithoutBoundFormAsset() {
        BusinessApplicationPageDesignDTO request = designRequest();
        request.getBuilder().put("formAssets", List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.save(APPLICATION_ID, request));

        assertEquals("页面草稿中未找到当前表单资产", error.getMessage());
        verify(transactionManager, never()).getTransaction(any());
        verify(tableMappingService, never()).syncManagedDatabase(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("a new page does not reuse an object when only the recycled page id matches")
    void newPageDoesNotReuseObjectWhenOnlyPageIdMatches() {
        BusinessApplicationPageDesignDTO request = designRequest();
        BusinessApplicationObjectVO stale = new BusinessApplicationObjectVO();
        stale.setObjectId(99L);
        stale.setObjectRole("PRIMARY");
        stale.setSortOrder(0);
        stale.setOptions("{\"managedBy\":\"PAGE_FORM\",\"sourceApplicationId\":\"11\","
                + "\"sourcePageId\":\"page-1\",\"sourceFormAssetId\":\"form-approval\"}");

        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of(stale));
        when(datasourceService.selectEnabledDatasources("LOWCODE_RUNTIME")).thenReturn(List.of(runtimeDatasource()));
        when(objectCreateService.create(any())).thenReturn(OBJECT_ID);
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        BusinessApplicationPageDesignVO result = service.save(APPLICATION_ID, request);

        assertTrue(result.getObjectCreated());
        assertEquals(OBJECT_ID, result.getObjectId());
        verify(objectService, never()).requireEntity(99L);
    }

    @Test
    @DisplayName("saving the same page and form asset still reuses the managed object")
    void repeatedSaveReusesObjectWhenPageAndFormAssetBothMatch() {
        BusinessApplicationPageDesignDTO request = designRequest();
        BusinessApplicationObjectVO association = new BusinessApplicationObjectVO();
        association.setObjectId(OBJECT_ID);
        association.setObjectRole("PRIMARY");
        association.setSortOrder(0);
        association.setOptions("{\"managedBy\":\"PAGE_FORM\",\"sourceApplicationId\":\"11\","
                + "\"sourcePageId\":\"page-1\",\"sourceFormAssetId\":\"form-1\"}");

        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of(association));
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        BusinessApplicationPageDesignVO result = service.save(APPLICATION_ID, request);

        assertFalse(result.getObjectCreated());
        assertEquals(OBJECT_ID, result.getObjectId());
        verify(objectCreateService, never()).create(any());
    }

    @Test
    @DisplayName("tree-list page save enables model treeConfig and list-form layout")
    void treeListPageSaveEnablesEmbeddedTree() {
        BusinessApplicationPageDesignDTO request = designRequest();
        request.setPageType("tree-list");
        request.setObjectId(OBJECT_ID);
        BusinessApplicationObjectVO association = new BusinessApplicationObjectVO();
        association.setObjectId(OBJECT_ID);
        association.setObjectRole("PRIMARY");
        association.setSortOrder(0);
        association.setOptions("{\"managedBy\":\"PAGE_FORM\",\"sourceApplicationId\":\"11\","
                + "\"sourcePageId\":\"page-1\",\"sourceFormAssetId\":\"form-1\"}");

        when(applicationService.requireEntity(APPLICATION_ID)).thenReturn(application());
        when(applicationObjectService.list(APPLICATION_ID)).thenReturn(List.of(association));
        when(objectService.requireEntity(OBJECT_ID)).thenReturn(savedObject());
        when(designerService.loadContext(OBJECT_ID)).thenReturn(designerContext(new LowcodeModelSchema()));

        service.save(APPLICATION_ID, request);

        ArgumentCaptor<BusinessObjectDesignerDTO> designer = ArgumentCaptor.forClass(BusinessObjectDesignerDTO.class);
        verify(designerService).saveDesigner(eq(OBJECT_ID), designer.capture());
        assertEquals("TREE", designer.getValue().getModelSchema().getAppType());
        assertEquals(Boolean.TRUE, designer.getValue().getModelSchema().getTreeConfig().getEnabled());
        assertEquals("parentId", designer.getValue().getModelSchema().getTreeConfig().getParentField());
        assertEquals("list-form", designer.getValue().getPageSchema().getLayoutType());
        assertEquals(Boolean.TRUE, designer.getValue().getPageSchema().getZones().stream()
                .filter(zone -> "table".equals(zone.getZoneKey()))
                .findFirst()
                .map(zone -> zone.getProps().get("enableTreeAddChild"))
                .orElse(null));
    }

    private BusinessApplicationPageDesignDTO designRequest() {
        BusinessApplicationPageDesignDTO request = new BusinessApplicationPageDesignDTO();
        request.setPageId("page-1");
        request.setPageType("form");
        request.setFormAssetId("form-1");
        request.setObjectCode("customer");
        request.setObjectName("客户");
        request.setFields(List.of(textField()));
        request.setFormDesignerSchema(formSchema());
        request.setBuilder(builder());
        return request;
    }

    private Map<String, Object> builder() {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", "block-1");
        block.put("blockType", "AiCrudPage");
        block.put("props", new LinkedHashMap<>(Map.of("formAssetId", "form-1")));
        Map<String, Object> gridLayout = new LinkedHashMap<>();
        gridLayout.put("items", new ArrayList<>(List.of(block)));
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("gridLayout", gridLayout);
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("layout", layout);

        Map<String, Object> builder = new LinkedHashMap<>();
        builder.put("nodes", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of(
                "id", "page-1",
                "type", "page")))));
        builder.put("pages", new LinkedHashMap<>(Map.of("page-1", page)));
        builder.put("formAssets", new ArrayList<>(List.of(new LinkedHashMap<>(Map.of(
                "id", "form-1",
                "formDesignerSchema", formSchemaMap(formSchema()))))));
        return builder;
    }

    private FormDesignerSchemaDTO formSchema() {
        FormDesignerSchemaDTO schema = new FormDesignerSchemaDTO();
        Map<String, Object> component = new LinkedHashMap<>();
        component.put("id", "field-1");
        component.put("componentKey", "input");
        component.put("fieldBinding", new LinkedHashMap<>(Map.of(
                "mode", "field",
                "fieldCode", "customerName")));
        schema.setComponents(new ArrayList<>(List.of(component)));
        return schema;
    }

    private BusinessFieldDTO textField() {
        BusinessFieldDTO field = new BusinessFieldDTO();
        field.setFieldCode("customerName");
        field.setColumnName("customer_name");
        field.setFieldName("客户名称");
        field.setFieldType("TEXT");
        field.setDataType("varchar");
        field.setLength(128);
        field.setPrecision(0);
        field.setComponentType("input");
        return field;
    }

    private LowcodeFieldSchema existingTextField() {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("customerName");
        field.setColumnName("customer_name");
        field.setLabel("客户名称");
        field.setBusinessFieldType("TEXT");
        field.setDataType("varchar");
        field.setLength(128);
        field.setPrecision(0);
        field.setComponentType("input");
        field.setSystemField(false);
        return field;
    }

    private BusinessObjectDesignerService.DesignerContext designerContext(LowcodeModelSchema modelSchema) {
        BusinessObjectDesignerService.DesignerContext context = new BusinessObjectDesignerService.DesignerContext();
        context.setModelSchema(modelSchema);
        return context;
    }

    private AiBusinessApplication application() {
        AiBusinessApplication application = new AiBusinessApplication();
        application.setId(APPLICATION_ID);
        application.setApplicationCode("crm");
        application.setApplicationName("客户管理");
        application.setSuiteCode("sales");
        application.setStatus(0);
        application.setOptions("{}");
        return application;
    }

    private AiBusinessObject savedObject() {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(OBJECT_ID);
        object.setSuiteCode("sales");
        object.setObjectCode("customer");
        object.setObjectName("客户");
        object.setObjectType("MASTER");
        object.setModelCode("sales_customer");
        object.setConfigKey("cfg_customer");
        object.setStatus(1);
        object.setOptions("{}");
        return object;
    }

    private GenDatasource runtimeDatasource() {
        GenDatasource datasource = new GenDatasource();
        datasource.setDatasourceId(31L);
        datasource.setDatasourceCode("runtime");
        datasource.setDatasourceName("运行库");
        datasource.setDbType("MySQL");
        datasource.setUsageScope("LOWCODE_RUNTIME");
        datasource.setAllowRuntimeWrite(1);
        datasource.setAllowRuntimeDdl(1);
        datasource.setReadonly(0);
        datasource.setIsDefault(1);
        datasource.setIsEnabled(1);
        datasource.setSort(0);
        return datasource;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectRef(Map<String, Object> builder) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) builder.get("nodes");
        return (Map<String, Object>) nodes.get(0).get("objectRef");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resultFormSchema(Map<String, Object> builder) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) builder.get("formAssets");
        return (Map<String, Object>) assets.get(0).get("formDesignerSchema");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resultFieldBinding(Map<String, Object> builder) {
        List<Map<String, Object>> components = (List<Map<String, Object>>) resultFormSchema(builder).get("components");
        return (Map<String, Object>) components.get(0).get("fieldBinding");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> componentFieldBinding(FormDesignerSchemaDTO schema) {
        return (Map<String, Object>) schema.getComponents().get(0).get("fieldBinding");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> formSchemaMap(FormDesignerSchemaDTO schema) {
        return new ObjectMapper().convertValue(schema, Map.class);
    }
}
