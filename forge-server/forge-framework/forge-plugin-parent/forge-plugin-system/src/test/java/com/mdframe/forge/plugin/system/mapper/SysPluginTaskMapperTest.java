package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskQuery;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 执行实际 Mapper/表结构，H2 不替代 MySQL/平台登录验收。 */
class SysPluginTaskMapperTest {
    @Test
    void private_blob_paging_tenant_cas_unique_active_and_soft_delete_contract() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        String script = Files.readString(Path.of("../../../db/migration/V1.0.211__add_plugin_install_workbench.sql"));
        int start = script.indexOf("CREATE TABLE");
        String ddl = script.substring(start, script.indexOf(";", start));
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute(ddl);
        }
        var factory = factory(source);
        try (var session = factory.getObject().openSession(true)) {
            SysPluginTaskMapper mapper = session.getMapper(SysPluginTaskMapper.class);
            var task = task("a", 1L);
            mapper.insert(task);
            assertThat(mapper.selectTask(2L, "a")).isNull();
            assertThat(mapper.selectTask(1L, "a").getArchiveData()).isNull();
            assertThat(mapper.selectArchive(1L, "a").getArchiveData()).isEqualTo(new byte[]{1, 2});
            assertThat(mapper.selectByRequest(1L, 9L, "a").getId()).isEqualTo("a");
            assertThat(mapper.selectByRequest(1L, 10L, "a")).isNull();
            var page = mapper.selectTaskPage(new Page<>(1, 15), 1L, new SysPluginTaskQuery());
            assertThat(page.getTotal()).isEqualTo(1);
            assertThat(page.getRecords().get(0).getPreviewJson()).isNull();
            assertThatThrownBy(() -> mapper.insert(task("b", 1L))).hasMessageContaining("uk_sys_plugin_task_active");
            mapper.insert(task("c", 2L));
            task.setTaskStatus("queued");
            assertThat(mapper.transition(task, "await_confirmation", 0)).isEqualTo(1);
            assertThat(mapper.transition(task, "await_confirmation", 0)).isZero();
            task.setTaskStatus("cancelled");
            task.setActivePluginId(null);
            assertThat(mapper.transition(task, "queued", 1)).isEqualTo(1);
            mapper.insert(task("b", 1L));
            // 此隔离 Factory 未注册宿主的自动审计填充器，显式传入审计字段验证逻辑删除。
            mapper.deleteById(task);
            assertThat(mapper.selectTask(1L, "a")).isNull();
            assertThat(mapper.selectArchive(1L, "a")).isNull();
        }
    }

    private MybatisSqlSessionFactoryBean factory(JdbcDataSource source) throws Exception {
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(SysPluginTaskMapper.class);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(config);
        factory.setPlugins(interceptor);
        factory.setMapperLocations(new ClassPathResource("mapper/SysPluginTaskMapper.xml"));
        factory.afterPropertiesSet();
        return factory;
    }

    private SysPluginTask task(String id, Long tenant) {
        SysPluginTask task = new SysPluginTask();
        task.setId(id);
        task.setTenantId(tenant);
        task.setRequestId(id);
        task.setPluginId("demo");
        task.setPluginName("测试");
        task.setPluginVersion("1.0.0");
        task.setOperationType("install");
        task.setTaskStatus("await_confirmation");
        task.setActivePluginId("demo");
        task.setRevision(0);
        task.setArchiveSha256("a".repeat(64));
        task.setFileName("demo.zip");
        task.setArchiveBytes(2);
        task.setArchiveData(new byte[]{1, 2});
        task.setRuntimeSnapshot("b".repeat(64));
        task.setPreviewJson("{}");
        task.setCreateBy(9L);
        task.setUpdateBy(9L);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setDelFlag(0);
        return task;
    }
}
