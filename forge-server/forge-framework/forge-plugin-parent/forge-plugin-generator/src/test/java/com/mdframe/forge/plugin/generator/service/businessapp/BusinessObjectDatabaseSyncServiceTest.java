package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessObjectDatabaseSyncService")
class BusinessObjectDatabaseSyncServiceTest {

    @Test
    @DisplayName("preview never executes database DDL")
    void previewNeverExecutesDdl() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        TestableTableMappingService service = service(ddlService, true);

        service.previewDatabaseDiff(201L, 7);

        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("sync requires explicit confirmation")
    void syncRequiresConfirmation() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        TestableTableMappingService service = service(ddlService, true);

        assertThrows(BusinessException.class, () -> service.syncDatabase(201L, 7, false));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("sync rejects stale design version")
    void syncRejectsStaleDesignVersion() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        TestableTableMappingService service = service(ddlService, true);

        assertThrows(BusinessException.class, () -> service.syncDatabase(201L, 6, true));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("sync rejects missing DDL permission")
    void syncRejectsMissingPermission() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        TestableTableMappingService service = service(ddlService, false);

        assertThrows(BusinessException.class, () -> service.syncDatabase(201L, 7, true));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("sync rejects datasource that disables online DDL")
    void syncRejectsDatasourceWithoutDdlCapability() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        BusinessObjectDesignerService.DesignerContext context = BusinessObjectTableMappingServiceTest.context();
        context.getModelSchema().getRuntimeDatasource().setAllowDdl(false);
        TestableTableMappingService service = service(ddlService, true, context);

        assertThrows(BusinessException.class, () -> service.syncDatabase(201L, 7, true));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("high risk modify statements are preview-only")
    void highRiskModifyIsPreviewOnly() {
        StubDdlService ddlService = new StubDdlService(
                "ALTER TABLE crm_customer MODIFY COLUMN customer_name varchar(32)", true);
        TestableTableMappingService service = service(ddlService, true);

        assertThrows(BusinessException.class, () -> service.syncDatabase(201L, 7, true));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("safe additive DDL executes after all guards")
    void safeAdditiveDdlExecutes() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        TestableTableMappingService service = service(ddlService, true);

        service.syncDatabase(201L, 7, true);

        assertTrue(ddlService.executed);
    }

    private static TestableTableMappingService service(StubDdlService ddlService, boolean permission) {
        return service(ddlService, permission, BusinessObjectTableMappingServiceTest.context());
    }

    private static TestableTableMappingService service(StubDdlService ddlService, boolean permission,
                                                       BusinessObjectDesignerService.DesignerContext context) {
        BusinessObjectDesignContextProvider contextProvider = objectId -> context;
        BusinessApplicationObjectMapper applicationObjectMapper = proxy(BusinessApplicationObjectMapper.class);
        AtomicBoolean updated = new AtomicBoolean();
        BusinessObjectMapper objectMapper = (BusinessObjectMapper) Proxy.newProxyInstance(
                BusinessObjectMapper.class.getClassLoader(), new Class<?>[]{BusinessObjectMapper.class},
                (proxy, method, args) -> {
                    if ("updateById".equals(method.getName())) {
                        updated.set(true);
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });
        return new TestableTableMappingService(contextProvider, ddlService,
                applicationObjectMapper, objectMapper, permission);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
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

    private static class TestableTableMappingService extends BusinessObjectTableMappingService {

        private final boolean permission;

        TestableTableMappingService(BusinessObjectDesignContextProvider contextProvider,
                                    LowcodeDdlService ddlService,
                                    BusinessApplicationObjectMapper applicationObjectMapper,
                                    BusinessObjectMapper objectMapper,
                                    boolean permission) {
            super(contextProvider, ddlService, applicationObjectMapper, objectMapper);
            this.permission = permission;
        }

        @Override
        protected boolean hasDdlPermission() {
            return permission;
        }
    }

    private static class StubDdlService extends LowcodeDdlService {

        private final String ddl;
        private final boolean executable;
        private boolean executed;

        StubDdlService(String ddl, boolean executable) {
            super(null, null, null, null, null);
            this.ddl = ddl;
            this.executable = executable;
        }

        @Override
        public LowcodeDdlPreviewVO previewCreateTable(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema) {
            LowcodeDdlPreviewVO preview = new LowcodeDdlPreviewVO();
            preview.setTableName(modelSchema.getTableName());
            preview.setTableExists(true);
            preview.setExecutable(executable);
            preview.getDdlStatements().add(ddl);
            return preview;
        }

        @Override
        public void executeCreateTable(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema) {
            executed = true;
        }
    }
}
