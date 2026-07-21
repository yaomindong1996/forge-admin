package com.mdframe.forge.plugin.job.registry;

import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.service.JobExecutorCatalogService;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobAutoRegistrarTest {

    @Test
    void shouldRegisterDesiredConfigByExactJobKey() {
        AtomicReference<SysJobConfig> inserted = new AtomicReference<>();
        SysJobConfigMapper mapper = mapper(null, inserted);
        JobAutoRegistrar registrar = new JobAutoRegistrar(mapper, new JobExecutorCatalogService());

        registrar.postProcessAfterInitialization(new AnnotatedJobs(), "annotatedJobs");

        SysJobConfig config = inserted.get();
        assertNotNull(config);
        assertEquals("annotatedJob", config.getJobName());
        assertEquals("BUSINESS", config.getJobGroup());
        assertEquals("annotatedJobs", config.getExecutorBean());
        assertEquals("execute", config.getExecutorMethod());
        assertEquals("BEAN", config.getExecuteMode());
        assertEquals(1, config.getStatus());
        assertEquals("PENDING", config.getSyncStatus());
    }

    @Test
    void shouldNotOverwriteExistingDesiredConfig() {
        SysJobConfig existing = new SysJobConfig();
        existing.setId(8L);
        existing.setJobName("annotatedJob");
        existing.setJobGroup("BUSINESS");
        AtomicReference<SysJobConfig> inserted = new AtomicReference<>();
        JobAutoRegistrar registrar = new JobAutoRegistrar(mapper(existing, inserted), new JobExecutorCatalogService());

        registrar.postProcessAfterInitialization(new AnnotatedJobs(), "annotatedJobs");

        assertNull(inserted.get());
    }

    private SysJobConfigMapper mapper(SysJobConfig existing, AtomicReference<SysJobConfig> inserted) {
        return (SysJobConfigMapper) Proxy.newProxyInstance(
                SysJobConfigMapper.class.getClassLoader(),
                new Class<?>[]{SysJobConfigMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectByJobKey" -> matches(existing, (String) args[0], (String) args[1])
                            ? existing : null;
                    case "insert" -> {
                        inserted.set((SysJobConfig) args[0]);
                        yield 1;
                    }
                    case "toString" -> "JobAutoRegistrarTestMapper";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private boolean matches(SysJobConfig existing, String jobName, String jobGroup) {
        return existing != null
                && existing.getJobName().equals(jobName)
                && existing.getJobGroup().equals(jobGroup);
    }

    static class AnnotatedJobs {
        @ScheduledJob(name = "annotatedJob", group = "BUSINESS", cron = "0 0/5 * * * ?",
                description = "注解任务")
        public void execute() {
        }
    }
}
