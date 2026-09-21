package com.mdframe.forge.plugin.generator.service.printing;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationVersionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationSnapshotService;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 + Spring 真实事务 + 生产 MyBatis XML；不连接业务库。MySQL 迁移/隔离级别另验。 */
class PrintApplicationPersistenceTest {
    private Connection anchor;
    private ValidatorFactory validation;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private TransactionTemplate tx;
    private BusinessApplicationMapper applications;
    private BusinessApplicationVersionMapper versions;
    private PrintApplicationLock lock;
    private PrintApplicationVersionGuard guard;
    private BusinessApplicationVersionService service;
    private PrintTemplate template;
    private PrintTemplateVersion version;
    private String candidate;
    private com.mdframe.forge.plugin.print.mapper.PrintBindingMapper bindingMapper;
    private PrintBindingValidationService bindingValidation;
    private PrintTemplateMapper templateMapper;
    private PrintTemplateVersionMapper printVersionMapper;
    private com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper configs;

    @BeforeEach void setup() throws Exception {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:app_print_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=2000");
        anchor = ds.getConnection(); jdbc = new JdbcTemplate(ds);
        transactionManager = new DataSourceTransactionManager(ds); tx = new TransactionTemplate(transactionManager);
        jdbc.execute("""
                CREATE TABLE ai_business_application (
                  id BIGINT PRIMARY KEY, tenant_id BIGINT, application_code VARCHAR(80), portal_slug VARCHAR(80),
                  application_name VARCHAR(100), suite_code VARCHAR(80), icon VARCHAR(80), description VARCHAR(200),
                  status INT DEFAULT 1, design_status VARCHAR(20) DEFAULT 'DRAFT', last_publish_version INT,
                  last_publish_time DATETIME, options LONGTEXT, portal_config LONGTEXT, ai_assistant_config LONGTEXT,
                  del_flag BIGINT DEFAULT 0, create_by BIGINT, create_time DATETIME, create_dept BIGINT,
                  update_by BIGINT, update_time DATETIME)
                """);
        jdbc.execute("""
                CREATE TABLE ai_business_application_version (
                  id BIGINT PRIMARY KEY, tenant_id BIGINT, application_id BIGINT, version_no INT,
                  snapshot_json LONGTEXT, snapshot_hash VARCHAR(64), publish_status VARCHAR(30),
                  publish_summary VARCHAR(1000), source_version_no INT, published_by BIGINT, published_time DATETIME,
                  del_flag BIGINT DEFAULT 0, create_by BIGINT, create_time DATETIME, create_dept BIGINT,
                  update_by BIGINT, update_time DATETIME, UNIQUE(tenant_id, application_id, version_no))
                """);
        jdbc.update("INSERT INTO ai_business_application(id,tenant_id) VALUES(2,1)");
        jdbc.update("INSERT INTO ai_business_application(id,tenant_id,del_flag) VALUES(3,1,3)");
        String ddl = Files.readString(Path.of("../../../db/migration/V1.0.171__add_native_print_tables.sql"))
                .replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci", "");
        for (String statement : ddl.split(";")) {
            if (!statement.isBlank()) { jdbc.execute(statement); }
        }
        jdbc.execute("ALTER TABLE sys_print_template MODIFY page_id VARCHAR(128)");
        jdbc.execute("ALTER TABLE sys_print_binding MODIFY page_id VARCHAR(128)");
        var configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        for (String name : List.of("BusinessApplication", "BusinessApplicationVersion", "PrintTemplate", "PrintTemplateVersion", "PrintBinding", "AiCrudConfig")) {
            String resource = "mapper/" + name + "Mapper.xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        var session = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
        applications = session.getMapper(BusinessApplicationMapper.class);
        versions = session.getMapper(BusinessApplicationVersionMapper.class);
        var templates = session.getMapper(PrintTemplateMapper.class);
        var printVersions = session.getMapper(PrintTemplateVersionMapper.class);
        bindingMapper = session.getMapper(com.mdframe.forge.plugin.print.mapper.PrintBindingMapper.class);
        templateMapper = templates; printVersionMapper = printVersions;
        configs = session.getMapper(com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper.class);
        bindingValidation = mock(PrintBindingValidationService.class);
        template = new PrintTemplate(); template.setTenantId(1L); template.setApplicationId(2L);
        template.setTemplateCode("synthetic"); template.setTemplateName("合成模板"); template.setStatus(1);
        template.setSourceType("LOWCODE"); template.setPageId(SOURCE.pageId()); template.setSourceKey(SOURCE.key());
        template.setObjectCode("purchase"); template.setDraftSchema(SCHEMA); template.setDraftRevision(1L);
        template.setDesignStatus("DRAFT"); template.setDelFlag(0L);
        template.setCreateBy(9L); template.setUpdateBy(9L); template.setCreateDept(1L);
        template.setCreateTime(LocalDateTime.now()); template.setUpdateTime(LocalDateTime.now());
        templates.insert(template);
        version = new PrintTemplateVersion(); version.setTenantId(1L); version.setTemplateId(template.getId());
        version.setVersionNo(1); version.setSchemaVersion(1); version.setSchemaJson(SCHEMA); version.setSchemaHash(HASH);
        version.setResourceManifest("[]"); version.setPublishTime(LocalDateTime.now()); version.setDelFlag(0);
        version.setCreateBy(9L); version.setUpdateBy(9L); version.setCreateDept(1L);
        version.setCreateTime(LocalDateTime.now()); version.setUpdateTime(LocalDateTime.now());
        printVersions.insert(version);
        candidate = snapshot(new PrintApplicationSnapshotCodec.Binding(SOURCE,
                com.mdframe.forge.plugin.print.enums.PrintScene.DETAIL, template.getId(), version.getId(), HASH, true, 0));
        validation = Validation.buildDefaultValidatorFactory();
        var identity = mock(PrintIdentity.class);
        when(identity.current()).thenReturn(new PrintActor(1L, 9L, 1L));
        lock = new PrintApplicationLock(applications);
        guard = new PrintApplicationVersionGuard(identity, lock,
                new PrintApplicationSnapshotCodec(validation.getValidator()), templates, printVersions, new PrintProtocolValidator(),
                bindingValidation, mock(PrintMetadataResolver.class));
        service = service(applications);
    }

    @AfterEach void close() throws Exception {
        if (validation != null) { validation.close(); }
        if (anchor != null) { anchor.close(); }
    }

    @Test void mapperLocksRequireTransactionAndRespectTenantAndLogicalDeletion() {
        assertThatThrownBy(() -> lock.lock(1L, 2L)).isInstanceOf(IllegalStateException.class);
        tx.executeWithoutResult(status -> {
            assertThat(lock.lock(1L, 2L).getId()).isEqualTo(2L);
            assertThatThrownBy(() -> lock.lock(2L, 2L)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> lock.lock(1L, 3L)).isInstanceOf(BusinessException.class);
        });
    }

    @Test void commitsPinnedVersionAndRollbackSnapshotWithoutFollowingLatestTemplate() {
        var published = commit(service, 1, candidate, false);
        jdbc.update("UPDATE sys_print_template SET draft_schema='changed draft',published_version_id=999 WHERE id=?", template.getId());
        var rollback = commit(service, 2, candidate, true);
        assertThat(rollback.getSnapshotJson()).isEqualTo(published.getSnapshotJson());
        assertThat(jdbc.queryForObject("SELECT last_publish_version FROM ai_business_application WHERE id=2", Integer.class)).isEqualTo(2);
        tx.executeWithoutResult(status -> {
            assertThat(versions.lockRetainedSnapshots(1L, 2L)).hasSize(2);
            assertThat(versions.lockRetainedSnapshots(2L, 2L)).isEmpty();
            assertThat(versions.lockRetainedSnapshots(1L, 3L)).isEmpty();
        });
        jdbc.update("UPDATE ai_business_application_version SET del_flag=id WHERE version_no=1");
        tx.executeWithoutResult(status -> assertThat(versions.lockRetainedSnapshots(1L, 2L))
                .singleElement().satisfies(row -> assertThat(row.getVersionNo()).isEqualTo(2)));
    }

    @Test void invalidTemplateCannotInsertVersionOrAdvancePointer() {
        commit(service, 1, "{}", false);
        jdbc.update("UPDATE sys_print_template SET status=0 WHERE id=?", template.getId());
        assertThatThrownBy(() -> commit(service, 2, candidate, false)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_business_application_version", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT last_publish_version FROM ai_business_application WHERE id=2", Integer.class)).isEqualTo(1);
    }

    @Test void failureAfterVersionInsertAndPointerUpdateRollsBackBothWrites() {
        var failingApplications = mock(BusinessApplicationMapper.class);
        when(failingApplications.markPublished(anyLong(), anyLong(), anyInt(), any())).thenAnswer(invocation -> {
            applications.markPublished(invocation.getArgument(0), invocation.getArgument(1),
                    invocation.getArgument(2), invocation.getArgument(3));
            throw new BusinessException("synthetic failure after update");
        });
        assertThatThrownBy(() -> commit(service(failingApplications), 1, candidate, false)).isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_business_application_version", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT last_publish_version FROM ai_business_application WHERE id=2", Integer.class)).isNull();
    }

    @Test void concurrentApplicationOperationsSerializeOnTheSameRow() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var locked = new CountDownLatch(1); var release = new CountDownLatch(1); var started = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> tx.executeWithoutResult(status -> {
                lock.lock(1L, 2L); locked.countDown();
                try { assertThat(release.await(5, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> { started.countDown(); return commit(service, 1, candidate, false); });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown(); first.get(5, TimeUnit.SECONDS);
            assertThat(second.get(5, TimeUnit.SECONDS).getVersionNo()).isEqualTo(1);
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void currentConfigGuardRespectsTenantDisableAndLogicalDeleteWithoutSelectingDraft() {
        jdbc.execute("CREATE TABLE ai_crud_config(id BIGINT,tenant_id BIGINT,config_key VARCHAR(40),status CHAR(1),mode VARCHAR(20),build_mode VARCHAR(20),del_flag BIGINT)");
        jdbc.execute("CREATE TABLE ai_business_object(id BIGINT,tenant_id BIGINT,config_key VARCHAR(40),object_code VARCHAR(40),status INT,del_flag BIGINT)");
        jdbc.update("INSERT INTO ai_crud_config VALUES(13,1,'synthetic','0','CONFIG','LOWCODE',0)");
        jdbc.update("INSERT INTO ai_business_object VALUES(3,1,'synthetic','purchase',1,0)");
        assertThat(configs.countActiveRuntimeConfig(1L, 13L, "purchase")).isEqualTo(1);
        assertThat(configs.countActiveRuntimeConfig(2L, 13L, "purchase")).isZero();
        assertThat(configs.countActiveRuntimeConfig(1L, 13L, "other")).isZero();
        jdbc.update("UPDATE ai_crud_config SET status='1'");
        assertThat(configs.countActiveRuntimeConfig(1L, 13L, "purchase")).isZero();
        jdbc.update("UPDATE ai_crud_config SET status='0'");
        jdbc.update("UPDATE ai_business_object SET del_flag=id");
        assertThat(configs.countActiveRuntimeConfig(1L, 13L, "purchase")).isZero();
    }

    @Test void fieldValidationFailureDoesNotAdvanceApplicationVersion() {
        commit(service, 1, "{}", false);
        doThrow(new BusinessException("synthetic invalid published field"))
                .when(bindingValidation).validate(any(), any(), anyString(), any(), eq(true));
        assertThatThrownBy(() -> commit(service, 2, candidate, false)).hasMessageContaining("published field");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_business_application_version", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT last_publish_version FROM ai_business_application WHERE id=2", Integer.class)).isEqualTo(1);
    }

    @Test void realBindingMapperCapturesOnlyActiveApplicationRowsAndPinsImmutableVersion() {
        var row = new com.mdframe.forge.plugin.print.entity.PrintBinding();
        row.setTenantId(1L); row.setApplicationId(2L); row.setSourceType("LOWCODE");
        row.setPageId(SOURCE.pageId()); row.setObjectCode(SOURCE.objectCode()); row.setSourceKey(SOURCE.key());
        row.setTemplateId(template.getId()); row.setScene("DETAIL"); row.setIsDefault(true); row.setSortOrder(0);
        row.setStatus(1); row.setBindingRevision(1L); row.setDelFlag(0L);
        row.setCreateBy(9L); row.setUpdateBy(9L); row.setCreateDept(1L);
        row.setCreateTime(LocalDateTime.now()); row.setUpdateTime(LocalDateTime.now());
        bindingMapper.insert(row);
        jdbc.update("UPDATE sys_print_template SET published_version_id=? WHERE id=?", version.getId(), template.getId());
        var identity = mock(PrintIdentity.class); when(identity.current()).thenReturn(new PrintActor(1L, 9L, 1L));
        var codec = new PrintApplicationSnapshotCodec(validation.getValidator());
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var capture = new PrintApplicationSnapshotContributor(identity, lock, bindingMapper, templateMapper,
                printVersionMapper, new PrintProtocolValidator(), codec, json, bindingValidation);
        var result = tx.execute(status -> {
            assertThat(bindingMapper.selectApplication(2L, 2L)).isEmpty();
            assertThat(bindingMapper.selectApplication(1L, 3L)).isEmpty();
            return capture.capture(2L, Map.of());
        });
        assertThat(((java.util.List<?>) result.get("bindings"))).hasSize(1);
        jdbc.update("UPDATE sys_print_binding SET status=0 WHERE id=?", row.getId());
        tx.executeWithoutResult(status -> assertThat(bindingMapper.selectApplication(1L, 2L)).isEmpty());
        jdbc.update("UPDATE sys_print_binding SET status=1,del_flag=id WHERE id=?", row.getId());
        tx.executeWithoutResult(status -> assertThat(bindingMapper.selectApplication(1L, 2L)).isEmpty());
    }

    private com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion commit(
            BusinessApplicationVersionService target, int versionNo, String json, boolean rollback) {
        return target.commitImmutable(2L, versionNo,
                new BusinessApplicationSnapshotService.SnapshotBundle(json, "synthetic-hash", Map.of()),
                rollback ? BusinessApplicationPublishStatus.ROLLBACK.getCode() : BusinessApplicationPublishStatus.PUBLISHED.getCode(),
                rollback ? 1 : null, "synthetic");
    }

    private BusinessApplicationVersionService service(BusinessApplicationMapper applicationMapper) {
        var target = new BusinessApplicationVersionService(null, applicationMapper, null, guard);
        ReflectionTestUtils.setField(target, "baseMapper", versions);
        var proxy = new ProxyFactory(target); proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (BusinessApplicationVersionService) proxy.getProxy();
    }
}
