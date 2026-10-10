package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginBuildClaimDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildHeartbeatDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.service.plugin.PluginBuildService;
import com.mdframe.forge.plugin.system.service.plugin.PluginBuildViews;
import com.mdframe.forge.plugin.system.service.plugin.PluginPreviewPlanner;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** H2 实际 XML + Spring 事务/CAS 回归；不代表真实 MySQL/登录或 HTTPS 已验收。 */
class SysPluginBuildMapperTest {
    private SysPluginTaskMapper tasks;
    private SysPluginBuildMapper builds;
    private PluginBuildService service;
    private final PluginPreviewPlanner planner = mock(PluginPreviewPlanner.class);
    private final PluginWorkerIdentity worker = new PluginWorkerIdentity(1L, "test-worker");
    private final byte[] archive = new byte[]{1, 2, 3};

    @BeforeEach
    void database() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        for (String file : new String[]{"V1.0.211__add_plugin_install_workbench.sql",
                "V1.0.212__add_plugin_build_leases.sql"}) {
            String sql = Files.readString(Path.of("../../../db/migration", file));
            int start = sql.indexOf("CREATE TABLE");
            try (var connection = source.getConnection(); var statement = connection.createStatement()) {
                statement.execute(sql.substring(start, sql.indexOf(';', start)));
            }
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(SysPluginTaskMapper.class);
        configuration.addMapper(SysPluginBuildMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/SysPluginTaskMapper.xml"),
                new ClassPathResource("mapper/SysPluginBuildMapper.xml"));
        factory.afterPropertiesSet();
        var session = new SqlSessionTemplate(factory.getObject());
        tasks = session.getMapper(SysPluginTaskMapper.class);
        builds = session.getMapper(SysPluginBuildMapper.class);
        var views = new PluginBuildViews(builds, new ObjectMapper().findAndRegisterModules());
        var target = new PluginBuildService(builds, tasks, views, planner);
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),
                new AnnotationTransactionAttributeSource()));
        service = (PluginBuildService) proxy.getProxy();
        when(planner.snapshot()).thenReturn("snapshot");
        tasks.insert(task());
    }

    @Test
    void private_lease_tenant_archive_monotonic_phase_cas_and_expiry_execute_real_xml() {
        var command = claim();
        service.claim("task", command, worker);
        assertThat(service.archive("task", command, worker)).isEqualTo(archive);
        assertThat(builds.selectSummary(1L, "task").getLeaseHash()).isNull();
        assertThat(builds.selectBuild(2L, "task")).isNull();
        var build = builds.selectBuild(1L, "task");
        var now = LocalDateTime.now();
        build.setPhase("container_build");
        build.setLeaseExpiresTime(now.plusSeconds(90));
        assertThat(builds.heartbeat(build, "source_snapshot", now)).isEqualTo(1);
        assertThat(builds.heartbeat(build, "source_snapshot", now)).isZero();
        assertThat(builds.finish(build, "source_snapshot", now)).isZero();
        build.setLeaseHash("0".repeat(64));
        assertThat(builds.heartbeat(build, "container_build", now)).isZero();
        build = builds.selectBuild(1L, "task");
        build.setLeaseExpiresTime(now.minusSeconds(1));
        assertThat(builds.heartbeat(build, "container_build", now)).isEqualTo(1);
        var heartbeat = new PluginBuildHeartbeatDTO();
        heartbeat.setLeaseToken(command.getLeaseToken());
        heartbeat.setPhase("container_build");
        assertThatThrownBy(() -> service.heartbeat("task", heartbeat, worker)).hasMessageContaining("到期");
        assertThatThrownBy(() -> service.claim("task", command, worker)).hasMessageContaining("到期");
        assertThat(tasks.selectTask(1L, "task").getActivePluginId()).isEqualTo("demo");
    }

    @Test
    void build_insert_failure_rolls_back_task_claim_in_actual_spring_transaction() {
        var command = claim();
        command.setImage("x".repeat(300));
        assertThatThrownBy(() -> service.claim("task", command, worker)).isInstanceOf(RuntimeException.class);
        assertThat(tasks.selectTask(1L, "task").getTaskStatus()).isEqualTo("queued");
        assertThat(tasks.selectTask(1L, "task").getRevision()).isEqualTo(1);
        assertThat(builds.selectBuild(1L, "task")).isNull();
    }

    @Test
    void simultaneous_workers_only_one_can_claim_and_audit_is_not_duplicated() throws Exception {
        var barrier = new CountDownLatch(2);
        when(planner.snapshot()).thenAnswer(invocation -> {
            barrier.countDown();
            assertThat(barrier.await(5, TimeUnit.SECONDS)).isTrue();
            return "snapshot";
        });
        var success = new AtomicInteger();
        var rejected = new AtomicInteger();
        Runnable action = () -> {
            try {
                service.claim("task", claim(), worker);
                success.incrementAndGet();
            } catch (RuntimeException failure) {
                rejected.incrementAndGet();
            }
        };
        var first = new Thread(action, "test-plugin-claim-1");
        var second = new Thread(action, "test-plugin-claim-2");
        first.start();
        second.start();
        first.join(10000);
        second.join(10000);
        assertThat(first.isAlive() || second.isAlive()).isFalse();
        assertThat(success).hasValue(1);
        assertThat(rejected).hasValue(1);
        assertThat(tasks.selectTask(1L, "task").getRevision()).isEqualTo(2);
        assertThat(builds.selectBuild(1L, "task")).isNotNull();
    }

    private PluginBuildClaimDTO claim() {
        var dto = new PluginBuildClaimDTO();
        dto.setLeaseToken(UUID.randomUUID().toString().replace("-", "").repeat(2));
        dto.setRevision(1);
        dto.setSha256(PackageDigests.sha256(archive));
        dto.setSourceCommit("a".repeat(40));
        dto.setImage("test-builder@sha256:" + "b".repeat(64));
        return dto;
    }

    private SysPluginTask task() {
        var task = new SysPluginTask();
        task.setId("task");
        task.setTenantId(1L);
        task.setRequestId("request");
        task.setPluginId("demo");
        task.setPluginName("测试");
        task.setPluginVersion("1.0.0");
        task.setTaskStatus("queued");
        task.setOperationType("install");
        task.setRevision(1);
        task.setActivePluginId("demo");
        task.setArchiveSha256(PackageDigests.sha256(archive));
        task.setArchiveBytes(archive.length);
        task.setArchiveData(archive);
        task.setFileName("demo.zip");
        task.setRuntimeSnapshot("snapshot");
        task.setPreviewJson("{}");
        task.setConfirmedBy(9L);
        task.setCreateBy(9L);
        task.setUpdateBy(9L);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setDelFlag(0);
        return task;
    }
}
