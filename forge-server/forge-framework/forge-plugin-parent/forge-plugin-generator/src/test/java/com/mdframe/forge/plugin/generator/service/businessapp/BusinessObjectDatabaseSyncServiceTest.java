package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
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
    @DisplayName("high risk modify statements alone remain preview-only")
    void highRiskModifyIsPreviewOnly() {
        StubDdlService ddlService = new StubDdlService(
                "ALTER TABLE crm_customer MODIFY COLUMN customer_name varchar(32)", true);
        ddlService.markAllManualReview();
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

    @Test
    @DisplayName("mixed safe add and high-risk modify still syncs the safe part")
    void mixedSafeAndHighRiskSyncsSafeOnly() {
        StubDdlService ddlService = new StubDdlService("ALTER TABLE crm_customer ADD COLUMN level int", true);
        ddlService.addDdl("ALTER TABLE crm_customer MODIFY COLUMN customer_name varchar(32)");
        TestableTableMappingService service = service(ddlService, true);

        service.syncDatabase(201L, 7, true);

        assertTrue(ddlService.safeDdlExecuted);
        assertTrue(ddlService.executed);
        assertFalse(ddlService.forceExecuted);
    }

    @Test
    @DisplayName("force execute runs all preview statements including high-risk modify")
    void forceExecuteRunsAllPreviewStatements() {
        StubDdlService ddlService = new StubDdlService(
                "ALTER TABLE crm_customer MODIFY COLUMN customer_name varchar(32)", true);
        ddlService.markAllManualReview();
        TestableTableMappingService service = service(ddlService, true);

        service.syncDatabase(201L, 7, true, true);

        assertTrue(ddlService.forceExecuted);
        assertTrue(ddlService.executed);
    }

    @Test
    @DisplayName("managed page forms use live DDL capability instead of a stale saved snapshot")
    void managedPageFormExecutesSafeDdl() {
        StubDdlService ddlService = new StubDdlService("CREATE TABLE crm_customer (...) ", true);
        BusinessObjectDesignerService.DesignerContext context = managedContext();
        context.getModelSchema().getRuntimeDatasource().setAllowDdl(false);
        TestableTableMappingService service = service(ddlService, false, context);

        service.syncManagedDatabase(201L, 10L, "form_customer");

        assertTrue(ddlService.executed);
    }

    @Test
    @DisplayName("automatic database sync rejects objects that are not managed page forms")
    void automaticSyncRejectsNonManagedObject() {
        StubDdlService ddlService = new StubDdlService("CREATE TABLE crm_customer (...) ", true);
        TestableTableMappingService service = service(ddlService, true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.syncManagedDatabase(201L, 10L, "form_customer"));

        assertTrue(error.getMessage().contains("不是页面表单自动管理的数据存储"));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("managed page forms respect the datasource automatic DDL switch")
    void managedPageFormRejectsDatasourceWithoutDdlCapability() {
        StubDdlService ddlService = new StubDdlService("CREATE TABLE crm_customer (...) ", false);
        BusinessObjectDesignerService.DesignerContext context = managedContext();
        TestableTableMappingService service = service(ddlService, false, context);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.syncManagedDatabase(201L, 10L, "form_customer"));

        assertTrue(error.getMessage().contains("当前数据存储未允许自动建表"));
        assertFalse(ddlService.executed);
    }

    @Test
    @DisplayName("managed page forms execute safe DDLs and silently skip leftover unsafe ones")
    void managedPageFormPartialSyncsUnsafeDdl() {
        StubDdlService ddlService = new StubDdlService(
                "ALTER TABLE crm_customer MODIFY COLUMN customer_name varchar(32)", true);
        TestableTableMappingService service = service(ddlService, false, managedContext());

        service.syncManagedDatabase(201L, 10L, "form_customer");

        assertFalse(ddlService.safeDdlExecuted);
    }

    @Test
    @DisplayName("managed page forms execute safe additive DDLs even when unsafe DDLs exist")
    void managedPageFormExecutesSafeDdlAlongsideUnsafe() {
        StubDdlService ddlService = new StubDdlService(
                "ALTER TABLE crm_customer ADD COLUMN new_field varchar(64)", true);
        ddlService.addDdl("ALTER TABLE crm_customer MODIFY COLUMN old_field varchar(128)");
        TestableTableMappingService service = service(ddlService, false, managedContext());

        service.syncManagedDatabase(201L, 10L, "form_customer");

        assertTrue(ddlService.safeDdlExecuted);
    }

    private static BusinessObjectDesignerService.DesignerContext managedContext() {
        BusinessObjectDesignerService.DesignerContext context = BusinessObjectTableMappingServiceTest.context();
        context.getObject().setOptions(JSON.toJSONString(new JSONObject()
                .fluentPut("managedBy", "PAGE_FORM")
                .fluentPut("sourceApplicationId", 10L)
                .fluentPut("sourceFormAssetId", "form_customer")));
        return context;
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

        private final List<String> ddls = new ArrayList<>();
        private final boolean executable;
        private boolean executed;
        private boolean safeDdlExecuted;
        private boolean forceExecuted;
        private boolean forceAllManualReview;

        StubDdlService(String ddl, boolean executable) {
            super(null, null, null, null, null);
            this.ddls.add(ddl);
            this.executable = executable;
        }

        void addDdl(String ddl) {
            this.ddls.add(ddl);
        }

        void markAllManualReview() {
            this.forceAllManualReview = true;
        }

        @Override
        public LowcodeDdlPreviewVO previewCreateTable(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema) {
            LowcodeDdlPreviewVO preview = new LowcodeDdlPreviewVO();
            preview.setTableName(modelSchema.getTableName());
            preview.setTableExists(true);
            preview.setExecutable(executable);
            preview.getDdlStatements().addAll(ddls);
            int safe = forceAllManualReview ? 0 : (int) ddls.stream()
                    .filter(ddl -> ddl.contains("ADD COLUMN") || ddl.contains("CREATE TABLE"))
                    .count();
            preview.setOnlineSafeDdlCount(safe);
            preview.setManualReviewDdlCount(ddls.size() - safe);
            return preview;
        }

        @Override
        public void executeCreateTable(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema) {
            executed = true;
        }

        @Override
        public int executeSafeDdlOnly(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema) {
            int safeCount = forceAllManualReview ? 0 : (int) ddls.stream()
                    .filter(ddl -> ddl.contains("ADD COLUMN") || ddl.contains("CREATE TABLE"))
                    .count();
            if (safeCount > 0) {
                safeDdlExecuted = true;
                executed = true;
            }
            return safeCount;
        }

        @Override
        public int countUnsafeDdl(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema) {
            if (forceAllManualReview) {
                return ddls.size();
            }
            return (int) ddls.stream()
                    .filter(ddl -> !(ddl.contains("ADD COLUMN") || ddl.contains("CREATE TABLE")))
                    .count();
        }

        @Override
        public int executePreviewDdl(com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema modelSchema,
                                     boolean force) {
            if (force) {
                forceExecuted = true;
                executed = true;
                return ddls.size();
            }
            return executeSafeDdlOnly(modelSchema);
        }
    }
}
