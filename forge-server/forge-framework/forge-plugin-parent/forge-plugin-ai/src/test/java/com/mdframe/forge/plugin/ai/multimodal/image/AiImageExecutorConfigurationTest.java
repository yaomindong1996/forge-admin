package com.mdframe.forge.plugin.ai.multimodal.image;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecoratorPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiImageExecutorConfigurationTest {
    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        DynamicDataSourceContextHolder.clear();
        MDC.clear();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Test
    void should_preserveTrustedScopeAndClearWorker_even_when_globalDecoratorIsEnabled() {
        new ApplicationContextRunner().withUserConfiguration(AiImageExecutorConfiguration.class,
                        AiImageTaskExecutor.class, AgentExecutionTaskDecorator.class,
                        TenantBusinessDataSourceTaskDecorator.class,
                        TenantBusinessDataSourceTaskDecoratorPostProcessor.class)
                .withPropertyValues("forge.business.datasource.enabled=true", "forge.ai.image.core-pool-size=1",
                        "forge.ai.image.max-pool-size=1", "forge.ai.image.queue-capacity=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var user = new LoginUser();
                    user.setUserId(31L);
                    user.setTenantId(7L);
                    var result = new CompletableFuture<String>();
                    try (var ignored = LoginUserContextHolder.open(user)) {
                        TenantContextHolder.setTenantId(7L);
                        TenantContextHolder.setIgnore(true);
                        DynamicDataSourceContextHolder.push("fixture-ds");
                        MDC.put("fixture", "scope");
                        context.getBean(AiImageTaskExecutor.class).execute(() -> {
                            try {
                                assertThat(SessionHelper.getUserId()).isEqualTo(31L);
                                assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
                                assertThat(TenantContextHolder.isIgnore()).isFalse();
                                assertThat(DynamicDataSourceContextHolder.peek()).isEqualTo("fixture-ds");
                                assertThat(MDC.get("fixture")).isEqualTo("scope");
                                result.complete(Thread.currentThread().getName());
                            } catch (Throwable ex) {
                                result.completeExceptionally(ex);
                            }
                        });
                    }
                    assertThat(result.get(3, TimeUnit.SECONDS)).startsWith("forge-ai-image-");
                    // 探针提交本身也经过全局装饰器；先清理调用线程，不能把再次捕获的身份误判为残留。
                    TenantContextHolder.clear();
                    DynamicDataSourceContextHolder.clear();
                    MDC.clear();
                    context.getBean("forgeAiImageTaskExecutor", ThreadPoolTaskExecutor.class).submit(() -> {
                        assertThat(LoginUserContextHolder.current()).isNull();
                        assertThat(TenantContextHolder.getTenantId()).isNull();
                        assertThat(DynamicDataSourceContextHolder.peek()).isNull();
                        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
                    }).get(3, TimeUnit.SECONDS);
                });
    }

    @Test
    void should_rejectSaturation_without_runningOnCallerThread() throws Exception {
        var pool = pool();
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
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
                throw new AssertionError("图像任务不能回退请求线程");
            })).isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        } finally {
            release.countDown();
            pool.shutdown();
        }
    }

    @Test
    void should_rejectInvalidPoolConfiguration() {
        var properties = new AiImageGenerationProperties();
        properties.setCorePoolSize(4);
        properties.setMaxPoolSize(1);
        assertThatThrownBy(() -> new AiImageExecutorConfiguration().forgeAiImageTaskExecutor(properties))
                .hasMessageContaining("最大线程");
    }

    static ThreadPoolTaskExecutor pool() {
        var properties = new AiImageGenerationProperties();
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        properties.setQueueCapacity(1);
        var pool = new AiImageExecutorConfiguration().forgeAiImageTaskExecutor(properties);
        pool.initialize();
        return pool;
    }
}
