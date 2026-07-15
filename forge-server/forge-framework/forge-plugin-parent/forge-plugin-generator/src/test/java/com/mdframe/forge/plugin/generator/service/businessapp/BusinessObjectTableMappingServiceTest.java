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
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableMappingVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
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
    @DisplayName("unmapped database columns remain visible")
    void unmappedDatabaseColumnsRemainVisible() {
        StubDdlService ddlService = new StubDdlService();
        ddlService.tableExists = true;
        ddlService.columns.put("customer_name", new ColumnMetadata(
                "customer_name", "varchar(128)", "NO", null, "", "客户名称", ""));
        ddlService.columns.put("legacy_code", new ColumnMetadata(
                "legacy_code", "varchar(64)", "YES", null, "", "历史编码", ""));
        BusinessObjectTableMappingService service = service(ddlService, 1L);

        BusinessObjectTableMappingVO mapping = service.getTableMapping(201L);

        assertEquals("OUT_OF_SYNC", mapping.getSyncStatus());
        assertTrue(mapping.getFields().stream().anyMatch(field -> "UNMAPPED_DATABASE_COLUMN".equals(field.getSyncStatus())));
    }

    private static BusinessObjectTableMappingService service(StubDdlService ddlService, Long sharedCount) {
        BusinessObjectDesignContextProvider contextProvider = objectId -> context();
        BusinessApplicationObjectMapper applicationObjectMapper = proxy(BusinessApplicationObjectMapper.class,
                (method, args) -> "countByObjectId".equals(method.getName())
                        ? sharedCount : defaultValue(method.getReturnType()));
        BusinessObjectMapper objectMapper = proxy(BusinessObjectMapper.class,
                (method, args) -> defaultValue(method.getReturnType()));
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
