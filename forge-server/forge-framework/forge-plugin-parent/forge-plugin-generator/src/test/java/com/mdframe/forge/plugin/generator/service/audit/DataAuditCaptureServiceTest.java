package com.mdframe.forge.plugin.generator.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiDataAuditCursor;
import com.mdframe.forge.plugin.generator.domain.entity.AiDataAuditEvent;
import com.mdframe.forge.plugin.generator.domain.entity.AiDataAuditField;
import com.mdframe.forge.plugin.generator.domain.entity.AiDataAuditPolicy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePrimaryKeyStrategy;
import com.mdframe.forge.plugin.generator.enums.DataAuditEventType;
import com.mdframe.forge.plugin.generator.enums.DataAuditSourceType;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.DataAuditCursorMapper;
import com.mdframe.forge.plugin.generator.mapper.DataAuditEventMapper;
import com.mdframe.forge.plugin.generator.mapper.DataAuditFieldMapper;
import com.mdframe.forge.plugin.generator.mapper.DataAuditPolicyMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudRepository;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 服务采集链路测试；Repository 按运行数据源返回数据，不模拟真实数据库事务。 */
class DataAuditCaptureServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OBJECT_ID = 501L;
    private static final String MASTER_TABLE = "audit_test_master";
    private static final String CHILD_TABLE = "audit_test_detail";
    private final ObjectMapper json = new ObjectMapper();
    private final SnapshotRepository repository = new SnapshotRepository();
    private final List<AiDataAuditEvent> events = new ArrayList<>();
    private final List<AiDataAuditField> fields = new ArrayList<>();
    private final List<LowcodeRuntimeDataSourceContext> persistenceContexts = new ArrayList<>();
    private final LowcodeRuntimeDataSourceContext runtime = runtime(11L);
    private final AiCrudConfig config = new AiCrudConfig();
    private DataAuditCaptureService service;

    @BeforeEach
    void setUp() {
        // 索引是进程级缓存，第一项测试不能继承前一个测试类的租户策略。
        DataAuditTransactionHolder.index().clearTenant(TENANT_ID);
        TenantContextHolder.setTenantId(TENANT_ID);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        config.setConfigKey("audit_test");
        config.setTableName(MASTER_TABLE);
        config.setModelSchema("""
                {"fields":[
                  {"field":"fieldInput","columnName":"field_input","label":"说明","dataType":"varchar"},
                  {"field":"fieldNumber","columnName":"field_number","label":"数量","dataType":"int"}
                ]}
                """);
        config.setOptions("""
                {"masterDetailConfig":{"children":[
                  {"tableName":"audit_test_detail","relationKey":"detail","foreignKey":"master_id"}
                ]}}
                """);
        AiBusinessObject object = new AiBusinessObject();
        object.setId(OBJECT_ID);
        object.setModelId(601L);
        object.setObjectCode("audit_test");
        object.setObjectName("审计测试");
        object.setConfigKey(config.getConfigKey());
        AiDataAuditPolicy policy = new AiDataAuditPolicy();
        policy.setObjectId(OBJECT_ID);
        policy.setEnabled(1);
        policy.setReasonRequired(0);
        policy.setShowInDetail(1);
        policy.setWriteBarrier(0);
        AiDataAuditCursor cursor = new AiDataAuditCursor();
        cursor.setId(701L);
        cursor.setRevision(0L);
        BusinessObjectMapper objects = stub(BusinessObjectMapper.class, (method, args) -> object);
        AiCrudConfigMapper configs = stub(AiCrudConfigMapper.class, (method, args) -> config);
        DataAuditPolicyMapper policies = stub(DataAuditPolicyMapper.class, (method, args) -> List.of(policy));
        DataAuditCursorMapper cursors = stub(DataAuditCursorMapper.class, (method, args) -> {
            if ("updateRevision".equals(method)) {
                cursor.setRevision((Long) args[1]);
                return 1;
            }
            return cursor;
        });
        DataAuditEventMapper eventMapper = stub(DataAuditEventMapper.class, (method, args) -> {
            persistenceContexts.add(LowcodeRuntimeDataSourceContextHolder.get());
            if ("insertBatch".equals(method)) {
                @SuppressWarnings("unchecked")
                List<AiDataAuditEvent> batch = (List<AiDataAuditEvent>) args[0];
                events.addAll(batch);
                return batch.size();
            }
            events.add((AiDataAuditEvent) args[0]);
            return 1;
        });
        DataAuditFieldMapper fieldMapper = stub(DataAuditFieldMapper.class, (method, args) -> {
            persistenceContexts.add(LowcodeRuntimeDataSourceContextHolder.get());
            if ("insertBatch".equals(method)) {
                @SuppressWarnings("unchecked")
                List<AiDataAuditField> batch = (List<AiDataAuditField>) args[0];
                fields.addAll(batch);
                return batch.size();
            }
            fields.add((AiDataAuditField) args[0]);
            return 1;
        });
        DataAuditValueNormalizer normalizer = new DataAuditValueNormalizer(json);
        service = new DataAuditCaptureService(cursors, eventMapper, fieldMapper,
                new DataAuditPolicyService(policies, objects, configs, json),
                new DataAuditDiffEngine(normalizer), new DataAuditValueProtector(null, null, normalizer),
                new DataAuditFieldResolver(json, normalizer), objects, repository, json);
    }

    @AfterEach
    void tearDown() {
        DataAuditTransactionHolder.clear();
        DataAuditTransactionHolder.index().clearTenant(TENANT_ID);
        TransactionSynchronizationManager.clear();
        TenantContextHolder.clear();
    }

    @Test
    void capturesMasterFieldsAndChildDeletionAfterRuntimeScopeCloses() {
        Map<String, Object> before = Map.of("id", 10L, "field_input", "原说明", "field_number", 1);
        repository.put(runtime, MASTER_TABLE, "10", before);
        // 平台连接仍能读到同名记录的旧值，不能用它代替业务连接的最终快照。
        repository.put(null, MASTER_TABLE, "10", before);
        repository.put(runtime, CHILD_TABLE, "21", Map.of("id", 21L, "master_id", 10L));
        repository.put(runtime, CHILD_TABLE, "22", Map.of("id", 22L, "master_id", 10L));
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_input", "新说明", "field_number", 2),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
            write(CHILD_TABLE, "id", "21", null, DataAuditTransactionHolder.WriteKind.DELETE);
            write(CHILD_TABLE, "id", "22", null, DataAuditTransactionHolder.WriteKind.DELETE);
        }

        commit();

        assertEquals(1, events.size());
        assertEquals(3, fields.size(), "同一事件必须包含两项主表变化和一项子表摘要");
        AiDataAuditField input = field("fieldInput");
        assertEquals("", input.getRelationKey());
        assertEquals("说明", input.getFieldLabel());
        assertEquals("原说明", input.getBeforeDisplay());
        assertEquals("新说明", input.getAfterDisplay());
        AiDataAuditField number = field("fieldNumber");
        assertEquals("数量", number.getFieldLabel());
        assertEquals("1", number.getBeforeDisplay());
        assertEquals("2", number.getAfterDisplay());
        AiDataAuditField child = field("__childRows");
        assertEquals("detail", child.getRelationKey());
        assertEquals("删除 2 行（21、22）", child.getAfterDisplay());
        assertTrue(fields.stream().allMatch(field -> events.get(0).getId().equals(field.getEventId())));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
        assertNull(DataAuditTransactionHolder.current());
        assertTrue(persistenceContexts.stream().allMatch(java.util.Objects::isNull));
    }

    @Test
    void capturesMasterOnlyFromFirstBeforeValueToFinalStoredValue() {
        repository.put(runtime, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 1));
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 2),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 3),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
            // 数据库最终值可能不同于请求或 afterWrite 的提示值。
            repository.put(runtime, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 4));
        }

        commit();

        assertEquals(1, events.size());
        assertEquals(1, fields.size());
        assertEquals("1", field("fieldNumber").getBeforeDisplay());
        assertEquals("4", field("fieldNumber").getAfterDisplay());
        assertEquals(1L, events.get(0).getRevision());
    }

    @Test
    void unchangedBusinessValuesDoNotCreateEventAfterContextCloses() {
        repository.put(runtime, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 1, "update_by", 1L));
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 1, "update_by", 2L),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
        }

        commit();

        assertTrue(events.isEmpty());
        assertTrue(fields.isEmpty());
    }

    @Test
    void generatedCustomPrimaryKeyKeepsSourceForFinalRead() {
        LowcodePrimaryKeyStrategy primaryKey = new LowcodePrimaryKeyStrategy();
        primaryKey.setColumnName("record_key");
        runtime.setPrimaryKey(primaryKey);
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, null, DataAuditSourceType.FORM, DataAuditEventType.CREATE, null, false);
            DataAuditTransactionHolder.prepareWrite(MASTER_TABLE, "record_key", null,
                    DataAuditTransactionHolder.WriteKind.INSERT);
            Map<String, Object> stored = Map.of("record_key", 10L, "field_number", 8);
            repository.put(runtime, MASTER_TABLE, "10", stored);
            DataAuditTransactionHolder.afterWrite(MASTER_TABLE, 10L, Map.of("field_number", 7),
                    DataAuditSourceType.FORM, DataAuditTransactionHolder.WriteKind.INSERT, 1);
        }

        commit();

        assertEquals(1, events.size());
        assertEquals("CREATE", events.get(0).getEventType());
        assertEquals("10", events.get(0).getRecordId());
        assertEquals("ABSENT", field("fieldNumber").getBeforeState());
        assertEquals("8", field("fieldNumber").getAfterDisplay());
    }

    @Test
    void eachRowKeepsItsSourceAndRestoresCommitCallerContext() {
        LowcodeRuntimeDataSourceContext other = runtime(12L);
        LowcodeRuntimeDataSourceContext caller = runtime(99L);
        repository.put(runtime, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 1));
        repository.put(other, MASTER_TABLE, "11", Map.of("id", 11L, "field_number", 5));
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 2),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
        }
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(other)) {
            write(MASTER_TABLE, "id", "11", Map.of("id", 11L, "field_number", 6),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
        }
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(caller)) {
            commit();
            assertSame(caller, LowcodeRuntimeDataSourceContextHolder.get());
        }

        assertEquals(2, events.size());
        assertEquals(List.of("1", "5"), fields.stream().map(AiDataAuditField::getBeforeDisplay).toList());
        assertEquals(List.of("2", "6"), fields.stream().map(AiDataAuditField::getAfterDisplay).toList());
        assertTrue(persistenceContexts.stream().allMatch(context -> context == caller));
        assertNull(LowcodeRuntimeDataSourceContextHolder.get());
    }

    @Test
    void readFailureRestoresCallerContextAndBlocksAuditCommit() {
        repository.put(runtime, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 1));
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 2),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
        }
        repository.failRead = true;
        LowcodeRuntimeDataSourceContext caller = runtime(99L);
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(caller)) {
            assertThrows(RuntimeException.class, this::commit);
            assertSame(caller, LowcodeRuntimeDataSourceContextHolder.get());
        }
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(events.isEmpty());
        assertTrue(fields.isEmpty());
        assertNull(DataAuditTransactionHolder.current());
    }

    @Test
    void rollbackClearsCaptureWithoutWritingEvidence() {
        repository.put(runtime, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 1));
        try (var ignored = LowcodeRuntimeDataSourceContextHolder.use(runtime)) {
            service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
            write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 2),
                    DataAuditTransactionHolder.WriteKind.UPDATE);
        }
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertTrue(events.isEmpty());
        assertTrue(fields.isEmpty());
        assertNull(DataAuditTransactionHolder.current());
    }

    @Test
    void platformDefaultSourceStillCapturesMasterFields() {
        repository.put(null, MASTER_TABLE, "10", Map.of("id", 10L, "field_number", 1));
        service.open(config, 10L, DataAuditSourceType.FORM, DataAuditEventType.UPDATE, null, false);
        write(MASTER_TABLE, "id", "10", Map.of("id", 10L, "field_number", 2),
                DataAuditTransactionHolder.WriteKind.UPDATE);

        commit();

        assertEquals(1, fields.size());
        assertEquals("1", field("fieldNumber").getBeforeDisplay());
        assertEquals("2", field("fieldNumber").getAfterDisplay());
    }

    private void write(String table, String primaryKey, String id, Map<String, Object> after,
                       DataAuditTransactionHolder.WriteKind kind) {
        DataAuditTransactionHolder.prepareWrite(table, primaryKey, id, kind);
        repository.put(LowcodeRuntimeDataSourceContextHolder.get(), table, id, after);
        DataAuditTransactionHolder.afterWrite(table, id, after, DataAuditSourceType.FORM, kind, 1);
    }

    private void commit() {
        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
        callbacks.forEach(callback -> callback.beforeCommit(false));
        callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    }

    private AiDataAuditField field(String code) {
        return fields.stream().filter(field -> code.equals(field.getFieldCode())).findFirst().orElseThrow();
    }

    private static LowcodeRuntimeDataSourceContext runtime(Long datasourceId) {
        LowcodeRuntimeDataSourceContext context = new LowcodeRuntimeDataSourceContext();
        context.setDatasourceId(datasourceId);
        context.setTableName(MASTER_TABLE);
        return context;
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.apply(method.getName(), args));
    }

    private static final class SnapshotRepository extends DynamicCrudRepository {
        private final Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        private boolean failRead;

        private SnapshotRepository() {
            super(null, null, null);
        }

        private void put(LowcodeRuntimeDataSourceContext context, String table, String id, Map<String, Object> row) {
            rows.put(key(context, table, id), row == null ? null : new LinkedHashMap<>(row));
        }

        @Override
        public Map<String, Object> selectByIdForUpdate(String table, String primaryKey, Object id, SqlCondition scope) {
            return selectById(table, primaryKey, id, scope);
        }

        @Override
        public Map<String, Object> selectById(String table, String primaryKey, Object id, SqlCondition scope) {
            if (failRead) {
                throw new IllegalStateException("模拟业务数据源读取失败");
            }
            Map<String, Object> row = rows.get(key(LowcodeRuntimeDataSourceContextHolder.get(), table, id));
            return row == null || !row.containsKey(primaryKey) ? null : new LinkedHashMap<>(row);
        }

        private static String key(LowcodeRuntimeDataSourceContext context, String table, Object id) {
            return (context == null ? "platform" : context.getDatasourceId()) + ":" + table + ":" + id;
        }
    }
}
