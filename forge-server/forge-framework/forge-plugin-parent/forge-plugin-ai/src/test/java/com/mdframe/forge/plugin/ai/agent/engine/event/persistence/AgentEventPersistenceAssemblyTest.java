package com.mdframe.forge.plugin.ai.agent.engine.event.persistence;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.event.mapper.AgentEventMapper;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentEventPersistenceAssemblyTest {
    @Test
    void should_resolveFrameworkGenerator_and_notDispatchThroughGlobalAsyncPool() {
        var mapper = mock(AgentEventMapper.class);
        var writerThread = new AtomicReference<Thread>();
        when(mapper.insertBatch(any())).thenAnswer(call -> {
            writerThread.set(Thread.currentThread());
            List<AgentEventEntity> batch = call.getArgument(0);
            assertThat(batch).hasSize(1).allMatch(row -> row.getTenantId().equals(7L));
            return batch.size();
        });
        new ApplicationContextRunner().withUserConfiguration(AsyncEnabledConfiguration.class)
                .withBean(AgentEventMapper.class, () -> mapper)
                .withBean(IdentifierGenerator.class, () -> entity -> 9007199254740993L)
                .withBean(AgentEventPersistence.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AgentEventPersistence.class);
                    var persistence = context.getBean(AgentEventPersistence.class);
                    assertThat(AopUtils.isAopProxy(persistence)).isFalse();
                    try (var ignored = LoginUserContextHolder.open(AgentEventPersistenceTest.user(31L, 7L))) {
                        persistence.persist(AgentEvent.of("synthetic-assembly", 0, AgentEventType.AGENT_END, "{}"));
                        persistence.flush();
                    }
                    assertThat(writerThread.get()).isSameAs(Thread.currentThread());
                    assertThat(context.getBean(ThreadPoolTaskExecutor.class).getActiveCount()).isZero();
                });
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class AsyncEnabledConfiguration {
        @Bean
        ThreadPoolTaskExecutor taskExecutor() {
            var executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setThreadNamePrefix("synthetic-global-async-");
            return executor;
        }
    }
}
