package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.PluginTaskReviewDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginBuild;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskReviewService;
import org.h2.jdbcx.JdbcDataSource;
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

/** 独占H2/实际XML与Spring事务；不是目标MySQL锁行为验收。 */
final class PluginReviewTestDatabase {
    final JdbcDataSource source = new JdbcDataSource();
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final SysPluginTaskMapper tasks;
    final SysPluginBuildMapper builds;
    final SysPluginTaskReviewMapper reviews;
    final PluginTaskReviewService service;

    PluginReviewTestDatabase() throws Exception {
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        for (String file : new String[]{"V1.0.211__add_plugin_install_workbench.sql",
                "V1.0.212__add_plugin_build_leases.sql", "V1.0.213__add_plugin_task_review.sql"}) {
            String sql = Files.readString(Path.of("../../../db/migration", file));
            int start = sql.indexOf("CREATE TABLE");
            execute(sql.substring(start, sql.indexOf(';', start)));
        }
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(SysPluginTaskMapper.class);
        configuration.addMapper(SysPluginBuildMapper.class);
        configuration.addMapper(SysPluginTaskReviewMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mapper/SysPluginTaskMapper.xml"),
                new ClassPathResource("mapper/SysPluginBuildMapper.xml"),
                new ClassPathResource("mapper/SysPluginTaskReviewMapper.xml"));
        factory.afterPropertiesSet();
        var session = new SqlSessionTemplate(factory.getObject());
        tasks = session.getMapper(SysPluginTaskMapper.class);
        builds = session.getMapper(SysPluginBuildMapper.class);
        reviews = session.getMapper(SysPluginTaskReviewMapper.class);
        var proxy = new ProxyFactory(new PluginTaskReviewService(tasks, builds, reviews, json));
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),
                new AnnotationTransactionAttributeSource()));
        service = (PluginTaskReviewService) proxy.getProxy();
        seed();
    }

    void execute(String sql) throws Exception {
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    PluginTaskReviewDTO close() {
        var dto = new PluginTaskReviewDTO();
        dto.setRequestId(UUID.randomUUID().toString());
        dto.setRevision(2);
        dto.setSha256("a".repeat(64));
        dto.setDecision("close_task");
        dto.setExecutorStopped(true);
        dto.setNotDeployed(true);
        dto.setNote("已核查本次执行器和容器均已停止，产物尚未部署");
        return dto;
    }

    private void seed() {
        var now = LocalDateTime.now();
        var task = new SysPluginTask();
        task.setId("task");
        task.setTenantId(1L);
        task.setRequestId("request");
        task.setPluginId("demo");
        task.setPluginName("测试");
        task.setPluginVersion("1.0.0");
        task.setOperationType("install");
        task.setTaskStatus("building");
        task.setRevision(2);
        task.setActivePluginId("demo");
        task.setArchiveSha256("a".repeat(64));
        task.setArchiveBytes(3);
        task.setArchiveData(new byte[]{1, 2, 3});
        task.setFileName("demo.zip");
        task.setRuntimeSnapshot("b".repeat(64));
        task.setPreviewJson("{}");
        task.setConfirmedBy(9L);
        task.setCreateBy(9L);
        task.setUpdateBy(9L);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setDelFlag(0);
        tasks.insert(task);
        var build = new SysPluginBuild();
        build.setId("task");
        build.setTenantId(1L);
        build.setWorkerId("worker");
        build.setLeaseHash("c".repeat(64));
        build.setPhase("container_build");
        build.setSourceCommit("d".repeat(40));
        build.setImage("builder@sha256:" + "e".repeat(64));
        build.setStartedTime(now.minusMinutes(5));
        build.setLeaseExpiresTime(now.minusSeconds(5));
        build.setDeadlineTime(now.plusMinutes(20));
        build.setCreateBy(9L);
        build.setUpdateBy(9L);
        build.setCreateTime(now);
        build.setUpdateTime(now);
        build.setDelFlag(0);
        builds.insert(build);
    }
}
