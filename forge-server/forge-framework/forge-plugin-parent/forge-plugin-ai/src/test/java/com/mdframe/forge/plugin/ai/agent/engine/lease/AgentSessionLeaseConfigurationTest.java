package com.mdframe.forge.plugin.ai.agent.engine.lease;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.TaskScheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentSessionLeaseConfigurationTest {
    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Test
    void should_runScopeFreeHeartbeat_and_notBecomeDefaultBusinessScheduler() {
        var store = mock(AgentSessionLeaseStore.class);
        when(store.acquire(any(), anyLong())).thenReturn(true);
        var heartbeat = new CountDownLatch(1);
        when(store.renew(any(), anyLong())).thenAnswer(call -> {
            if (Thread.currentThread().getName().startsWith("forge-agent-lease-")) {
                assertThat(LoginUserContextHolder.current()).isNull();
                assertThat(TenantContextHolder.getTenantId()).isNull();
                assertThat(ExecutionIdentityContextHolder.current()).isEmpty();
                heartbeat.countDown();
            }
            return 1L;
        });
        new ApplicationContextRunner().withUserConfiguration(AgentSessionLeaseConfiguration.class,
                        AgentSessionLeaseRegistry.class).withBean(AgentSessionLeaseStore.class, () -> store)
                .withPropertyValues("forge.agent.session-lease.ttl=10s",
                        "forge.agent.session-lease.heartbeat-interval=1s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(TaskScheduler.class)).isEmpty();
                    var user = new LoginUser();
                    user.setUserId(31L);
                    user.setTenantId(7L);
                    try (var ignored = LoginUserContextHolder.open(user)) {
                        TenantContextHolder.setTenantId(7L);
                        var execution = new ReactContext();
                        execution.setSessionId("fixture");
                        execution.setUserId(31L);
                        execution.setTenantId(7L);
                        try (var lease = context.getBean(AgentSessionLeaseRegistry.class).acquire(execution)) {
                            assertThat(heartbeat.await(3, TimeUnit.SECONDS)).isTrue();
                            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
                            assertThat(execution.isExecutionLost()).isFalse();
                        }
                    }
                });
    }

    @Test
    void should_rejectInvalidLeaseProperties_during_springAssembly() {
        new ApplicationContextRunner().withUserConfiguration(AgentSessionLeaseConfiguration.class)
                .withPropertyValues("forge.agent.session-lease.ttl=1s")
                .run(context -> assertThat(context).hasFailed());
    }
}
