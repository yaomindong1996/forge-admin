package com.mdframe.forge.plugin.ai.agent.engine.context;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecoratorPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentExecutorConfigurationTest {

    @AfterEach
    void clearRequest() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
        MDC.clear();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Test
    void should_notLeaveInheritedRequestContext_when_poolThreadIsCreatedAndReused() throws Exception {
        ThreadPoolTaskExecutor pool = pool();
        try {
            CompletableFuture<Void> done = new CompletableFuture<>();
            LoginUser caller = new LoginUser();
            caller.setUserId(31L);
            caller.setTenantId(7L);
            try (var ignored = LoginUserContextHolder.open(caller)) {
                TenantContextHolder.setTenantId(7L);
                TenantContextHolder.setIgnore(true);
                DynamicDataSourceContextHolder.push("fixture-ds");
                MDC.put("fixture", "request");
                new AgentLoopTaskExecutor(pool, new AgentExecutionTaskDecorator()).execute(() -> {
                    try {
                        assertThat(SessionHelper.getUserId()).isEqualTo(31L);
                        assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
                        assertThat(TenantContextHolder.isIgnore()).isFalse();
                        assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("fixture-ds");
                        assertThat(MDC.get("fixture")).isEqualTo("request");
                        done.complete(null);
                    } catch (Throwable ex) {
                        done.completeExceptionally(ex);
                    }
                });
            }
            done.get(3, TimeUnit.SECONDS);
            pool.submit(() -> {
                assertThat(TenantContextHolder.getTenantId()).isNull();
                assertThat(TenantContextHolder.isIgnore()).isFalse();
                assertThat(LoginUserContextHolder.current()).isNull();
                assertThat(ExecutionIdentityContextHolder.current()).isEmpty();
                assertThat(DynamicDataSourceContextHolder.peek()).isNull();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }).get(3, TimeUnit.SECONDS);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void should_rejectOverflowInsteadOfRunningOnCaller_when_queueIsFull() throws Exception {
        ThreadPoolTaskExecutor pool = pool();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            pool.execute(() -> {
                started.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
            pool.execute(() -> { });
            assertThatThrownBy(() -> pool.execute(() -> {
                throw new AssertionError("不能回退请求线程");
            })).isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        } finally {
            release.countDown();
            pool.shutdown();
        }
    }

    @Test
    void should_keepUserScope_when_globalTenantDecoratorPostProcessorIsEnabled() {
        new ApplicationContextRunner().withUserConfiguration(AgentExecutorConfiguration.class,
                        AgentExecutionTaskDecorator.class, AgentLoopTaskExecutor.class,
                        TenantBusinessDataSourceTaskDecorator.class,
                        TenantBusinessDataSourceTaskDecoratorPostProcessor.class)
                .withPropertyValues("forge.business.datasource.enabled=true",
                        "forge.agent.executor.core-pool-size=1", "forge.agent.executor.max-pool-size=1",
                        "forge.agent.executor.queue-capacity=2")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var executor = context.getBean(AgentLoopTaskExecutor.class);
                    CompletableFuture<Long> result = new CompletableFuture<>();
                    LoginUser caller = new LoginUser();
                    caller.setUserId(31L);
                    caller.setTenantId(7L);
                    try (var ignored = LoginUserContextHolder.open(caller)) {
                        executor.execute(() -> result.complete(SessionHelper.getUserId()));
                    }
                    assertThat(result.get(3, TimeUnit.SECONDS)).isEqualTo(31L);
                });
    }

    private ThreadPoolTaskExecutor pool() {
        AgentExecutorProperties properties = new AgentExecutorProperties();
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        properties.setQueueCapacity(1);
        var pool = new AgentExecutorConfiguration().forgeAgentTaskExecutor(properties);
        pool.initialize();
        return pool;
    }
}
