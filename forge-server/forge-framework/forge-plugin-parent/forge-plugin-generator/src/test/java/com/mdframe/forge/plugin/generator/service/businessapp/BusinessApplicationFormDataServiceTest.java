package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationFormDataProvisionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.service.IGenDatasourceService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationFormDataVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplicationFormDataService")
class BusinessApplicationFormDataServiceTest {

    private final BusinessNamingService namingService = new BusinessNamingService();
    private StubTableMappingService lastTableMappingService;

    @Test
    @DisplayName("first save chooses the default writable low-code datasource and creates one managed object")
    void firstSaveCreatesManagedObjectWithDefaultWritableDatasource() {
        StubApplicationService applicationService = new StubApplicationService(application());
        StubApplicationObjectService applicationObjectService = new StubApplicationObjectService(List.of());
        StubObjectService objectService = new StubObjectService(Map.of(
                900000000000000001L,
                object(900000000000000001L, "crm_customer_form", "runtime_customer")
        ), List.of());
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000001L);
        StubDesignerService designerService = new StubDesignerService();
        AtomicInteger datasourceCalls = new AtomicInteger();
        IGenDatasourceService datasourceService = datasourceService(List.of(
                datasource(1L, 0, 1, 0),
                datasource(2L, 1, 1, 0)
        ), datasourceCalls);
        BusinessApplicationFormDataService service = service(
                applicationService,
                applicationObjectService,
                objectService,
                objectCreateService,
                designerService,
                datasourceService);

        BusinessApplicationFormDataVO result = service.provision(10L, request());

        assertEquals(1, objectCreateService.calls);
        assertEquals(2L, objectCreateService.created.getRuntimeDatasourceId());
        JSONObject options = JSON.parseObject(objectCreateService.created.getOptions());
        assertEquals("PAGE_FORM", options.getString("managedBy"));
        assertEquals("form_customer", options.getString("sourceFormAssetId"));
        assertEquals(2L, options.getJSONObject("runtimeDatasource").getLong("datasourceId"));

        assertEquals(1, applicationObjectService.replaceCalls);
        assertEquals(1, applicationObjectService.replaced.size());
        assertEquals(900000000000000001L, applicationObjectService.replaced.get(0).getObjectId());
        assertEquals("PRIMARY", applicationObjectService.replaced.get(0).getObjectRole());
        assertEquals(1, designerService.calls);
        assertEquals(900000000000000001L, designerService.objectId);
        assertEquals("客户登记表", designerService.designer.getObjectName());
        assertEquals("customerName", designerService.designer.getFields().get(0).getFieldCode());
        assertEquals(1, datasourceCalls.get());
        assertEquals(1, lastTableMappingService.calls);
        assertEquals(900000000000000001L, lastTableMappingService.objectId);
        assertTrue(lastTableMappingService.metadataCommittedBeforeSync);
        assertTrue(result.getCreated());
        assertEquals(900000000000000001L, result.getObjectId());
    }

    @Test
    @DisplayName("first save uses the requested runtime datasource")
    void firstSaveUsesRequestedRuntimeDatasource() {
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000001L);
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application()),
                new StubApplicationObjectService(List.of()),
                new StubObjectService(Map.of(
                        900000000000000001L,
                        object(900000000000000001L, "crm_customer_form", "runtime_customer")
                ), List.of()),
                objectCreateService,
                new StubDesignerService(),
                datasourceService(List.of(
                        datasource(2L, 1, 1, 0),
                        datasource(8L, 0, 1, 0)
                ), new AtomicInteger()));
        BusinessApplicationFormDataProvisionDTO request = request();
        request.setRuntimeDatasourceId(8L);

        service.provision(10L, request);

        assertEquals(8L, objectCreateService.created.getRuntimeDatasourceId());
        assertEquals("BLANK", objectCreateService.created.getCreateMode());
    }

    @Test
    @DisplayName("managed object model code stays within the database length for long application identities")
    void managedObjectModelCodeStaysWithinDatabaseLength() {
        AiBusinessApplication application = application();
        application.setSuiteCode("procurement_warehouse_management");
        application.setApplicationCode("procurement_warehouse_crm_test1_1_data_d7b09108");
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000001L);
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application),
                new StubApplicationObjectService(List.of()),
                new StubObjectService(Map.of(
                        900000000000000001L,
                        object(900000000000000001L, "managed_form", "runtime_form")
                ), List.of()),
                objectCreateService,
                new StubDesignerService(),
                datasourceService(List.of(datasource(2L, 1, 1, 0)), new AtomicInteger()));

        service.provision(10L, request());

        assertTrue(objectCreateService.created.getModelCode().length() <= 48);
    }

    @Test
    @DisplayName("repeated saves reuse the managed association and only synchronize the designer")
    void repeatedSaveReusesManagedObject() {
        BusinessApplicationObjectVO association = new BusinessApplicationObjectVO();
        association.setObjectId(900000000000000001L);
        association.setOptions(JSON.toJSONString(new JSONObject()
                .fluentPut("managedBy", "PAGE_FORM")
                .fluentPut("sourceFormAssetId", "form_customer")));
        StubApplicationService applicationService = new StubApplicationService(application());
        StubApplicationObjectService applicationObjectService = new StubApplicationObjectService(List.of(association));
        StubObjectService objectService = new StubObjectService(Map.of(
                900000000000000001L,
                object(900000000000000001L, "crm_customer_form", "runtime_customer")
        ), List.of());
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000002L);
        StubDesignerService designerService = new StubDesignerService();
        AtomicInteger datasourceCalls = new AtomicInteger();
        BusinessApplicationFormDataService service = service(
                applicationService,
                applicationObjectService,
                objectService,
                objectCreateService,
                designerService,
                datasourceService(List.of(), datasourceCalls));

        BusinessApplicationFormDataVO result = service.provision(10L, request());

        assertEquals(1, designerService.calls);
        assertEquals(900000000000000001L, designerService.objectId);
        assertEquals(0, objectCreateService.calls);
        assertEquals(0, datasourceCalls.get());
        assertEquals(0, applicationObjectService.replaceCalls);
        assertEquals(1, lastTableMappingService.calls);
        assertFalse(result.getCreated());
        assertEquals("runtime_customer", result.getConfigKey());
    }

    @Test
    @DisplayName("publish preparation skips unrelated objects and current in-sync managed storage")
    void synchronizeManagedDatabasesSkipsUnrelatedAndInSyncObjects() {
        BusinessApplicationObjectVO managed = new BusinessApplicationObjectVO();
        managed.setObjectId(900000000000000001L);
        managed.setOptions(JSON.toJSONString(new JSONObject()
                .fluentPut("managedBy", "PAGE_FORM")
                .fluentPut("sourceApplicationId", 10L)
                .fluentPut("sourceFormAssetId", "form_customer")));
        BusinessApplicationObjectVO manual = new BusinessApplicationObjectVO();
        manual.setObjectId(800000000000000001L);
        manual.setOptions("{}");
        BusinessApplicationObjectVO foreignManaged = new BusinessApplicationObjectVO();
        foreignManaged.setObjectId(700000000000000001L);
        foreignManaged.setOptions(JSON.toJSONString(new JSONObject()
                .fluentPut("managedBy", "PAGE_FORM")
                .fluentPut("sourceApplicationId", 99L)
                .fluentPut("sourceFormAssetId", "form_foreign")));
        BusinessApplicationObjectVO alreadySynchronized = new BusinessApplicationObjectVO();
        alreadySynchronized.setObjectId(600000000000000001L);
        alreadySynchronized.setSyncStatus("IN_SYNC");
        alreadySynchronized.setOptions(JSON.toJSONString(new JSONObject()
                .fluentPut("managedBy", "PAGE_FORM")
                .fluentPut("sourceApplicationId", 10L)
                .fluentPut("sourceFormAssetId", "form_synchronized")));
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        StubTableMappingService tableMappingService = new StubTableMappingService(() -> true, null);
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application()),
                new StubApplicationObjectService(List.of(
                        managed, manual, foreignManaged, alreadySynchronized)),
                new StubObjectService(Map.of(), List.of()),
                new StubObjectCreateService(900000000000000002L),
                new StubDesignerService(),
                datasourceService(List.of(), new AtomicInteger()),
                transactionManager,
                tableMappingService);

        int synchronizedCount = service.synchronizeManagedDatabases(10L);

        assertEquals(1, synchronizedCount);
        assertEquals(1, tableMappingService.calls);
        assertEquals(900000000000000001L, tableMappingService.objectId);
        assertEquals(10L, tableMappingService.applicationId);
        assertEquals("form_customer", tableMappingService.formAssetId);
    }

    @Test
    @DisplayName("a lost association is restored from the stable form ownership marker")
    void lostAssociationReusesManagedObject() {
        long objectId = 900000000000000001L;
        BusinessObjectVO candidate = new BusinessObjectVO();
        candidate.setId(objectId);
        candidate.setOptions(JSON.toJSONString(new JSONObject()
                .fluentPut("managedBy", "PAGE_FORM")
                .fluentPut("sourceApplicationId", 10L)
                .fluentPut("sourceFormAssetId", "form_customer")));
        StubApplicationObjectService applicationObjectService = new StubApplicationObjectService(List.of());
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000002L);
        StubDesignerService designerService = new StubDesignerService();
        AtomicInteger datasourceCalls = new AtomicInteger();
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application()),
                applicationObjectService,
                new StubObjectService(Map.of(
                        objectId, object(objectId, "crm_customer_form", "runtime_customer")
                ), List.of(candidate)),
                objectCreateService,
                designerService,
                datasourceService(List.of(), datasourceCalls));

        BusinessApplicationFormDataVO result = service.provision(10L, request());

        assertFalse(result.getCreated());
        assertEquals(objectId, result.getObjectId());
        assertEquals(0, objectCreateService.calls);
        assertEquals(0, datasourceCalls.get());
        assertEquals(1, designerService.calls);
        assertEquals(1, applicationObjectService.replaceCalls);
        assertEquals(objectId, applicationObjectService.replaced.get(0).getObjectId());
    }

    @Test
    @DisplayName("storage is not created when no writable low-code datasource exists")
    void missingWritableDatasourceIsRejected() {
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000001L);
        StubDesignerService designerService = new StubDesignerService();
        StubApplicationObjectService applicationObjectService = new StubApplicationObjectService(List.of());
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application()),
                applicationObjectService,
                new StubObjectService(Map.of(), List.of()),
                objectCreateService,
                designerService,
                datasourceService(List.of(
                        datasource(1L, 1, 0, 0),
                        datasource(2L, 0, 1, 1)
                ), new AtomicInteger()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.provision(10L, request()));

        assertEquals("当前数据存储未允许自动建表，请在高级数据设置中开启自动建表", error.getMessage());
        assertEquals(0, objectCreateService.calls);
        assertEquals(0, designerService.calls);
        assertEquals(0, applicationObjectService.replaceCalls);
    }

    @Test
    @DisplayName("automatic storage requires a datasource that allows managed table creation")
    void missingAutomaticDdlDatasourceIsRejected() {
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000001L);
        StubDesignerService designerService = new StubDesignerService();
        StubApplicationObjectService applicationObjectService = new StubApplicationObjectService(List.of());
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application()),
                applicationObjectService,
                new StubObjectService(Map.of(), List.of()),
                objectCreateService,
                designerService,
                datasourceService(List.of(
                        datasource(1L, 1, 1, 0, 0)
                ), new AtomicInteger()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.provision(10L, request()));

        assertEquals("当前数据存储未允许自动建表，请在高级数据设置中开启自动建表", error.getMessage());
        assertEquals(0, objectCreateService.calls);
        assertEquals(0, designerService.calls);
        assertEquals(0, applicationObjectService.replaceCalls);
        assertEquals(0, lastTableMappingService.calls);
    }

    @Test
    @DisplayName("database creation failure keeps committed form metadata and returns a retryable message")
    void databaseFailureKeepsCommittedMetadata() {
        StubApplicationObjectService applicationObjectService = new StubApplicationObjectService(List.of());
        StubObjectCreateService objectCreateService = new StubObjectCreateService(900000000000000001L);
        StubDesignerService designerService = new StubDesignerService();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        StubTableMappingService tableMappingService = new StubTableMappingService(
                () -> transactionManager.commitCount > 0,
                new BusinessException("目标数据库暂时不可用"));
        BusinessApplicationFormDataService service = service(
                new StubApplicationService(application()),
                applicationObjectService,
                new StubObjectService(Map.of(
                        900000000000000001L,
                        object(900000000000000001L, "crm_customer_form", "runtime_customer")
                ), List.of()),
                objectCreateService,
                designerService,
                datasourceService(List.of(datasource(2L, 1, 1, 0)), new AtomicInteger()),
                transactionManager,
                tableMappingService);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.provision(10L, request()));

        assertEquals("数据表创建失败：目标数据库暂时不可用；已保留表单设计，可直接重试", error.getMessage());
        assertEquals(1, transactionManager.commitCount);
        assertEquals(0, transactionManager.rollbackCount);
        assertEquals(1, objectCreateService.calls);
        assertEquals(1, designerService.calls);
        assertEquals(1, applicationObjectService.replaceCalls);
        assertEquals(1, tableMappingService.calls);
        assertTrue(tableMappingService.metadataCommittedBeforeSync);
    }

    @Test
    @DisplayName("a form without persistent fields cannot create hidden storage")
    void emptyPersistentFieldsAreRejected() {
        StubApplicationService applicationService = new StubApplicationService(application());
        BusinessApplicationFormDataService service = service(
                applicationService,
                new StubApplicationObjectService(List.of()),
                new StubObjectService(Map.of(), List.of()),
                new StubObjectCreateService(900000000000000001L),
                new StubDesignerService(),
                datasourceService(List.of(), new AtomicInteger()));
        BusinessApplicationFormDataProvisionDTO request = request();
        request.setFields(List.of());

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.provision(10L, request));

        assertEquals("表单还没有可保存的数据字段", error.getMessage());
        assertEquals(0, applicationService.requireCalls);
    }

    private BusinessApplicationFormDataService service(
            BusinessApplicationService applicationService,
            BusinessApplicationObjectService applicationObjectService,
            BusinessObjectService objectService,
            BusinessObjectCreateService objectCreateService,
            BusinessObjectDesignerService designerService,
            IGenDatasourceService datasourceService) {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        StubTableMappingService tableMappingService = new StubTableMappingService(
                () -> transactionManager.commitCount > 0, null);
        lastTableMappingService = tableMappingService;
        return service(applicationService, applicationObjectService, objectService, objectCreateService,
                designerService, datasourceService, transactionManager, tableMappingService);
    }

    private BusinessApplicationFormDataService service(
            BusinessApplicationService applicationService,
            BusinessApplicationObjectService applicationObjectService,
            BusinessObjectService objectService,
            BusinessObjectCreateService objectCreateService,
            BusinessObjectDesignerService designerService,
            IGenDatasourceService datasourceService,
            RecordingTransactionManager transactionManager,
            StubTableMappingService tableMappingService) {
        return new BusinessApplicationFormDataService(
                applicationService,
                applicationObjectService,
                objectService,
                objectCreateService,
                designerService,
                namingService,
                datasourceService,
                tableMappingService,
                noOpObjectMapper(),
                transactionManager);
    }

    /** 签名写入仅用到 updateById；其余方法全部返回默认值即可 */
    private static BusinessObjectMapper noOpObjectMapper() {
        return (BusinessObjectMapper) Proxy.newProxyInstance(
                BusinessObjectMapper.class.getClassLoader(),
                new Class<?>[] {BusinessObjectMapper.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class)
                        return false;
                    if (returnType == int.class)
                        return 0;
                    if (returnType == long.class)
                        return 0L;
                    return null;
                });
    }

    private AiBusinessApplication application() {
        AiBusinessApplication application = new AiBusinessApplication();
        application.setId(10L);
        application.setSuiteCode("crm");
        application.setApplicationCode("crm_center");
        application.setApplicationName("CRM 中心");
        return application;
    }

    private BusinessApplicationFormDataProvisionDTO request() {
        BusinessFieldDTO field = new BusinessFieldDTO();
        field.setFieldName("客户名称");
        field.setFieldCode("customerName");
        field.setColumnName("customer_name");
        field.setFieldType("TEXT");
        field.setSystemField(false);

        BusinessApplicationFormDataProvisionDTO request = new BusinessApplicationFormDataProvisionDTO();
        request.setFormAssetId("form_customer");
        request.setFormName("客户登记表");
        request.setFields(List.of(field));
        return request;
    }

    private GenDatasource datasource(Long id, Integer isDefault, Integer allowWrite, Integer readonly) {
        return datasource(id, isDefault, allowWrite, readonly, 1);
    }

    private GenDatasource datasource(
            Long id, Integer isDefault, Integer allowWrite, Integer readonly, Integer allowDdl) {
        GenDatasource datasource = new GenDatasource();
        datasource.setDatasourceId(id);
        datasource.setDatasourceCode("runtime_" + id);
        datasource.setDatasourceName("运行库 " + id);
        datasource.setDbType("MYSQL");
        datasource.setUsageScope("LOWCODE_RUNTIME");
        datasource.setIsDefault(isDefault);
        datasource.setAllowRuntimeWrite(allowWrite);
        datasource.setAllowRuntimeDdl(allowDdl);
        datasource.setReadonly(readonly);
        datasource.setRiskLevel("LOW");
        datasource.setSort(id.intValue());
        return datasource;
    }

    private AiBusinessObject object(Long id, String objectCode, String configKey) {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(id);
        object.setObjectCode(objectCode);
        object.setObjectName("客户登记表");
        object.setConfigKey(configKey);
        return object;
    }

    @SuppressWarnings("unchecked")
    private IGenDatasourceService datasourceService(List<GenDatasource> datasources, AtomicInteger calls) {
        return (IGenDatasourceService) Proxy.newProxyInstance(
                IGenDatasourceService.class.getClassLoader(),
                new Class[]{IGenDatasourceService.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.getName().equals("toString") ? "GenDatasourceServiceProxy" : null;
                    }
                    if ("selectEnabledDatasources".equals(method.getName())) {
                        calls.incrementAndGet();
                        return datasources;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class || returnType == long.class
                || returnType == short.class || returnType == byte.class) {
            return 0;
        }
        return null;
    }

    private static class StubApplicationService extends BusinessApplicationService {

        private final AiBusinessApplication application;
        private int requireCalls;

        StubApplicationService(AiBusinessApplication application) {
            super(null, null, null, null);
            this.application = application;
        }

        @Override
        public AiBusinessApplication requireEntity(Long id) {
            requireCalls++;
            return application;
        }
    }

    private static class StubApplicationObjectService extends BusinessApplicationObjectService {

        private final List<BusinessApplicationObjectVO> associations;
        private int replaceCalls;
        private List<BusinessApplicationObjectDTO> replaced = List.of();

        StubApplicationObjectService(List<BusinessApplicationObjectVO> associations) {
            super(null, null);
            this.associations = associations;
        }

        @Override
        public List<BusinessApplicationObjectVO> list(Long applicationId) {
            return associations;
        }

        @Override
        public void replace(Long applicationId, List<BusinessApplicationObjectDTO> objects) {
            replaceCalls++;
            replaced = objects;
        }
    }

    private static class StubObjectService extends BusinessObjectService {

        private final Map<Long, AiBusinessObject> objects;
        private final List<BusinessObjectVO> candidates;

        StubObjectService(Map<Long, AiBusinessObject> objects, List<BusinessObjectVO> candidates) {
            super(null, null, null, null, null);
            this.objects = new HashMap<>(objects);
            this.candidates = candidates;
        }

        @Override
        public List<BusinessObjectVO> list(BusinessObjectQueryDTO query) {
            return candidates;
        }

        @Override
        public AiBusinessObject requireEntity(Long id) {
            AiBusinessObject object = objects.get(id);
            if (object == null) {
                throw new BusinessException("业务对象不存在");
            }
            return object;
        }
    }

    private static class StubObjectCreateService extends BusinessObjectCreateService {

        private final Long objectId;
        private int calls;
        private BusinessObjectDTO created;

        StubObjectCreateService(Long objectId) {
            super(null, null, null, null);
            this.objectId = objectId;
        }

        @Override
        public Long create(BusinessObjectDTO dto) {
            calls++;
            created = dto;
            return objectId;
        }
    }

    private static class StubDesignerService extends BusinessObjectDesignerService {

        private int calls;
        private Long objectId;
        private BusinessObjectDesignerDTO designer;

        StubDesignerService() {
            super(null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public BusinessObjectDesignerService.DesignerContext saveDesigner(Long objectId, BusinessObjectDesignerDTO dto) {
            calls++;
            this.objectId = objectId;
            designer = dto;
            BusinessObjectDesignerService.DesignerContext context = new BusinessObjectDesignerService.DesignerContext();
            AiBusinessObject object = new AiBusinessObject();
            object.setId(objectId);
            context.setObject(object);
            return context;
        }
    }

    private static class StubTableMappingService extends BusinessObjectTableMappingService {

        private final BooleanSupplier committed;
        private final RuntimeException failure;
        private int calls;
        private Long objectId;
        private Long applicationId;
        private String formAssetId;
        private boolean metadataCommittedBeforeSync;

        StubTableMappingService(BooleanSupplier committed, RuntimeException failure) {
            super(null, null, null, null);
            this.committed = committed;
            this.failure = failure;
        }

        @Override
        public void syncManagedDatabase(Long objectId, Long applicationId, String formAssetId) {
            calls++;
            this.objectId = objectId;
            this.applicationId = applicationId;
            this.formAssetId = formAssetId;
            metadataCommittedBeforeSync = committed.getAsBoolean();
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        public void syncManagedDatabase(
                BusinessObjectDesignerService.DesignerContext context,
                Long applicationId, String formAssetId) {
            Long objectId = context == null || context.getObject() == null
                    ? null
                    : context.getObject().getId();
            syncManagedDatabase(objectId, applicationId, formAssetId);
        }
    }

    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private int commitCount;
        private int rollbackCount;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbackCount++;
        }
    }
}
