package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.manager.JobScheduleCoordinator;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.service.impl.SysJobConfigServiceImpl;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SysJobConfigServiceImplTest {

    @Test
    void shouldRejectManualTriggerWhenQuartzStateIsNotSynchronized() {
        AtomicBoolean triggered = new AtomicBoolean();
        SysJobConfig jobConfig = jobConfig(JobScheduleCoordinator.SYNC_FAILED);
        SysJobConfigServiceImpl service = service(jobConfig, triggered);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.triggerJob(jobConfig.getId()));

        assertTrue(exception.getMessage().contains("先重新同步"));
        assertFalse(triggered.get());
    }

    @Test
    void shouldAllowManualTriggerAfterSynchronization() {
        AtomicBoolean triggered = new AtomicBoolean();
        SysJobConfig jobConfig = jobConfig(JobScheduleCoordinator.SYNCED);
        SysJobConfigServiceImpl service = service(jobConfig, triggered);

        service.triggerJob(jobConfig.getId());

        assertTrue(triggered.get());
    }

    private SysJobConfigServiceImpl service(SysJobConfig jobConfig, AtomicBoolean triggered) {
        JobScheduler scheduler = new JobScheduler(null, new JobScheduleDomainService()) {
            @Override
            public void triggerJob(String jobName, String jobGroup, Long jobConfigId) {
                triggered.set(true);
            }
        };
        SysJobConfigServiceImpl service = new SysJobConfigServiceImpl(
                scheduler, null, null, null, null, mock(JobManagementSecurityService.class), null);
        ReflectionTestUtils.setField(service, "baseMapper", mapper(jobConfig));
        return service;
    }

    private SysJobConfigMapper mapper(SysJobConfig jobConfig) {
        return (SysJobConfigMapper) Proxy.newProxyInstance(
                SysJobConfigMapper.class.getClassLoader(),
                new Class<?>[]{SysJobConfigMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> jobConfig;
                    case "toString" -> "SysJobConfigMapperTestProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private SysJobConfig jobConfig(String syncStatus) {
        SysJobConfig jobConfig = new SysJobConfig();
        jobConfig.setId(1L);
        jobConfig.setJobName("sampleJob");
        jobConfig.setJobGroup("DEFAULT");
        jobConfig.setSyncStatus(syncStatus);
        return jobConfig;
    }
}
