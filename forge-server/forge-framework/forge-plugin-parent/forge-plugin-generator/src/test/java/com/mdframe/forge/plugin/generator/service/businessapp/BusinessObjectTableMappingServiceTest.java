package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeDatasourceSnapshot;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlRepository.ColumnMetadata;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableFieldMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableMappingVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessObjectTableMappingService")
class BusinessObjectTableMappingServiceTest {

    @Test
    @DisplayName("table mapping exposes datasource table and three-way field mapping")
    void tableMappingExposesDatabaseAnchor() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        BusinessObjectTableMappingService service = service(ddlService, 3L);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        assertEquals("runtime_main", mapping.getDatasourceCode());
        assertEquals("crm_customer", mapping.getTableName());
        assertEquals(7, mapping.getDesignVersion());
        assertEquals(3L, mapping.getSharedApplicationCount());
        assertEquals("IN_SYNC", mapping.getSyncStatus());
        assertEquals("customerName", mapping.getFields().get(0).getFieldCode());
        assertEquals("customer_name", mapping.getFields().get(0).getColumnName());
        assertEquals("IN_SYNC", mapping.getFields().get(0).getSyncStatus());
    }

    @Test
    @DisplayName("live IN_SYNC reconciles stale PARTIAL databaseSync marker")
    void liveInSyncReconcilesStalePartialMarker() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        BusinessObjectDesignerService.DesignerContext context = context();
        context.getObject().setDesignerOptions(
                "{\"databaseSync\":{\"status\":\"PARTIAL\",\"designVersion\":5,"
                        + "\"message\":\"已跳过高风险差异\",\"ddlCount\":0}}");
        AtomicBoolean updated = new AtomicBoolean(false);
        BusinessObjectTableMappingService service = service(ddlService, 1L, context, updated);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        assertEquals("IN_SYNC", mapping.getSyncStatus());
        assertEquals("IN_SYNC", mapping.getLastSyncStatus());
        assertTrue(updated.get());
        assertTrue(context.getObject().getDesignerOptions().contains("\"status\":\"IN_SYNC\""));
    }

    @Test
    @DisplayName("nullable unmapped database columns remain visible without blocking")
    void unmappedDatabaseColumnsRemainVisible() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        ddlService.columns.put("legacy_code", new ColumnMetadata(
                "legacy_code", "varchar(64)", "YES", null, "", "历史编码", ""));
        BusinessObjectTableMappingService service = service(ddlService, 1L);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        assertEquals("IN_SYNC", mapping.getSyncStatus());
        assertTrue(mapping.getFields().stream()
                .anyMatch(field -> "UNMAPPED_DATABASE_COLUMN".equals(field.getSyncStatus())
                        && !Boolean.TRUE.equals(field.getBlockingDifference())));
    }

    @Test
    @DisplayName("forge system columns remain visible without causing out of sync")
    void forgeSystemColumnsDoNotCauseOutOfSync() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        Map<String, String> systemColumns = Map.of(
                "id", "bigint",
                "tenant_id", "bigint",
                "del_flag", "char(1)",
                "create_by", "bigint",
                "create_time", "datetime",
                "create_dept", "bigint",
                "update_by", "bigint",
                "update_time", "datetime");
        for (Map.Entry<String, String> entry : systemColumns.entrySet()) {
            String column = entry.getKey();
            ddlService.columns.put(column, new ColumnMetadata(
                    column, entry.getValue(), "YES", null, "", column, ""));
        }
        BusinessObjectTableMappingService service = service(ddlService, 1L);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        assertEquals("IN_SYNC", mapping.getSyncStatus());
        assertEquals(0, mapping.getUnsyncedChangeCount());
        assertEquals(0, mapping.getPendingDdlCount());
        assertTrue(mapping.getFields().stream()
                .filter(field -> Boolean.TRUE.equals(field.getSystemField()))
                .allMatch(field -> "IN_SYNC".equals(field.getSyncStatus())));
    }

    @Test
    @DisplayName("missing and mismatched business columns remain out of sync")
    void missingAndMismatchedBusinessColumnsRemainOutOfSync() {
        StubDdlService missingColumnDdl = new StubDdlService();
        missingColumnDdl.tableExists = true;
        BusinessObjectTableMappingVO missingMapping = service(missingColumnDdl, 1L).getTableMapping(201L);

        StubDdlService mismatchedTypeDdl = new StubDdlService();
        mismatchedTypeDdl.tableExists = true;
        mismatchedTypeDdl.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(64)", "NO", null, "", "客户名称", ""));
        BusinessObjectTableMappingVO mismatchedMapping = service(mismatchedTypeDdl, 1L).getTableMapping(201L);

        assertEquals("OUT_OF_SYNC", missingMapping.getSyncStatus());
        assertEquals("MISSING_DATABASE_COLUMN", missingMapping.getFields().get(0).getSyncStatus());
        assertEquals(1, missingMapping.getPendingDdlCount());
        assertEquals("OUT_OF_SYNC", mismatchedMapping.getSyncStatus());
        assertEquals("TYPE_MISMATCH", mismatchedMapping.getFields().get(0).getSyncStatus());
    }

    @Test
    @DisplayName("logic delete character and integer storage types are compatible")
    void logicDeleteCharacterAndIntegerStorageTypesAreCompatible() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        ddlService.columns.put("del_flag", new ColumnMetadata(
                "del_flag", "tinyint", "NO", 0, "", "删除标志", ""));
        BusinessObjectDesignerService.DesignerContext context = context();
        LowcodeFieldSchema delFlag = new LowcodeFieldSchema();
        delFlag.setField("delFlag");
        delFlag.setColumnName("del_flag");
        delFlag.setLabel("删除标志");
        delFlag.setDataType("char");
        delFlag.setLength(1);
        delFlag.setSystemField(true);
        delFlag.setReadonly(true);
        context.getModelSchema().setFields(List.of(context.getModelSchema().getFields().get(0), delFlag));

        BusinessObjectTableMappingVO mapping = service(ddlService, 1L, context).getTableMapping(201L);

        BusinessObjectTableFieldMappingVO delFlagMapping = mapping.getFields().stream()
                .filter(field -> "del_flag".equals(field.getColumnName()))
                .findFirst()
                .orElseThrow();
        assertEquals("IN_SYNC", mapping.getSyncStatus());
        assertEquals("IN_SYNC", delFlagMapping.getSyncStatus());
        assertFalse(Boolean.TRUE.equals(delFlagMapping.getBlockingDifference()));
    }

    @Test
    @DisplayName("safe unmapped database columns remain visible without blocking publication")
    void safeUnmappedDatabaseColumnsDoNotBlockPublication() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        ddlService.columns.put("department_name", new ColumnMetadata(
                "department_name", "varchar(128)", "YES", null, "", "部门名称", ""));
        ddlService.columns.put("handover_owner_name", new ColumnMetadata(
                "handover_owner_name", "varchar(128)", "NO", "", "", "交接人姓名", ""));
        ddlService.columns.put("generated_label", new ColumnMetadata(
                "generated_label", "varchar(255)", "NO", null, "VIRTUAL GENERATED", "生成标签", "concat(`customer_name`,'')"));
        ddlService.columns.put("legacy_sequence", new ColumnMetadata(
                "legacy_sequence", "bigint", "NO", null, "auto_increment", "历史序号", ""));
        BusinessObjectTableMappingService service = service(ddlService, 1L);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        List<BusinessObjectTableFieldMappingVO> unmappedFields = mapping.getFields().stream()
                .filter(field -> "UNMAPPED_DATABASE_COLUMN".equals(field.getSyncStatus()))
                .toList();
        assertEquals("IN_SYNC", mapping.getSyncStatus());
        assertEquals(0, mapping.getUnsyncedChangeCount());
        assertEquals(4, unmappedFields.size());
        assertTrue(unmappedFields.stream()
                .noneMatch(field -> Boolean.TRUE.equals(field.getBlockingDifference())));
    }

    @Test
    @DisplayName("required unmapped database column without default remains blocking")
    void requiredUnmappedDatabaseColumnWithoutDefaultRemainsBlocking() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        ddlService.columns.put("approval_code", new ColumnMetadata(
                "approval_code", "varchar(64)", "NO", null, "", "审批编码", ""));
        BusinessObjectTableMappingService service = service(ddlService, 1L);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        BusinessObjectTableFieldMappingVO approvalCode = mapping.getFields().stream()
                .filter(field -> "approval_code".equals(field.getColumnName()))
                .findFirst()
                .orElseThrow();
        assertEquals("OUT_OF_SYNC", mapping.getSyncStatus());
        assertEquals(1, mapping.getUnsyncedChangeCount());
        assertTrue(Boolean.TRUE.equals(approvalCode.getBlockingDifference()));
    }

    private static BusinessObjectTableMappingService service(StubDdlService ddlService, Long sharedCount) {
        return service(ddlService, sharedCount, context(), null);
    }

    private static BusinessObjectTableMappingService service(
            StubDdlService ddlService,
            Long sharedCount,
            BusinessObjectDesignerService.DesignerContext context) {
        return service(ddlService, sharedCount, context, null);
    }

    private static BusinessObjectTableMappingService service(
            StubDdlService ddlService,
            Long sharedCount,
            BusinessObjectDesignerService.DesignerContext context,
            AtomicBoolean updated) {
        BusinessObjectDesignContextProvider contextProvider = objectId -> context;
        BusinessApplicationObjectMapper applicationObjectMapper = proxy(BusinessApplicationObjectMapper.class,
                (method, args) -> "countByObjectId".equals(method.getName())
                        ? sharedCount : defaultValue(method.getReturnType()));
        BusinessObjectMapper objectMapper = proxy(BusinessObjectMapper.class,
                (method, args) -> {
                    if ("updateById".equals(method.getName())) {
                        if (updated != null) {
                            updated.set(true);
                        }
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });
        return new BusinessObjectTableMappingService(contextProvider, ddlService,
                applicationObjectMapper, objectMapper);
    }

    static BusinessObjectDesignerService.DesignerContext context() {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(201L);
        object.setObjectCode("customer");
        object.setObjectName("客户");
        object.setDesignerOptions("{}");

        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("customerName");
        field.setColumnName("customer_name");
        field.setLabel("客户名称");
        field.setDataType("varchar");
        field.setLength(128);
        field.setRequired(true);
        field.setComponentType("input");

        LowcodeRuntimeDatasourceSnapshot datasource = new LowcodeRuntimeDatasourceSnapshot();
        datasource.setDatasourceId(9L);
        datasource.setDatasourceCode("runtime_main");
        datasource.setDatasourceName("低代码运行库");
        datasource.setDbType("MySQL");
        datasource.setTableName("crm_customer");
        datasource.setTableMode("CREATE");
        datasource.setAllowDdl(true);

        LowcodeModelSchema modelSchema = new LowcodeModelSchema();
        modelSchema.setTableName("crm_customer");
        modelSchema.setTableMode("CREATE");
        modelSchema.setRuntimeDatasource(datasource);
        modelSchema.setFields(java.util.List.of(field));

        AiCrudConfig config = new AiCrudConfig();
        config.setDraftVersion(7);

        BusinessObjectDesignerService.DesignerContext context = new BusinessObjectDesignerService.DesignerContext();
        context.setObject(object);
        context.setConfig(config);
        context.setModelSchema(modelSchema);
        return context;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method, args));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }

    @FunctionalInterface
    private interface Handler {
        Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }

    static class StubDdlService extends LowcodeDdlService {

        boolean tableExists;
        boolean executable = true;
        boolean executed;
        final Map<String, ColumnMetadata> columns = new LinkedHashMap<>();
        Set<String> indexes = Set.of();

        StubDdlService() {
            super(null, null, null, null, null);
        }

        @Override
        public LowcodeDdlPreviewVO previewCreateTable(LowcodeModelSchema modelSchema) {
            LowcodeDdlPreviewVO preview = new LowcodeDdlPreviewVO();
            preview.setTableName(modelSchema.getTableName());
            preview.setTableExists(tableExists);
            preview.setExecutable(executable);
            if (!tableExists) {
                preview.getDdlStatements().add("CREATE TABLE crm_customer (...)");
            }
            else if (columns.get("customer_name") == null) {
                preview.getDdlStatements().add("ALTER TABLE crm_customer ADD COLUMN customer_name varchar(128)");
            }
            return preview;
        }

        @Override
        public Map<String, ColumnMetadata> listColumnMetadata(LowcodeModelSchema modelSchema) {
            return columns;
        }

        @Override
        public Set<String> listIndexes(LowcodeModelSchema modelSchema) {
            return indexes;
        }

        @Override
        public void executeCreateTable(LowcodeModelSchema modelSchema) {
            executed = true;
        }
    }
}
