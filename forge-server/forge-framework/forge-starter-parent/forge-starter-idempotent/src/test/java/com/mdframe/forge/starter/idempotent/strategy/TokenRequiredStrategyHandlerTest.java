package com.mdframe.forge.starter.idempotent.strategy;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.aop.IdempotentAspect;
import com.mdframe.forge.starter.idempotent.enums.IdempotentStrategy;
import com.mdframe.forge.starter.idempotent.exception.TokenInvalidException;
import com.mdframe.forge.starter.idempotent.generator.IdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.properties.IdempotentProperties;
import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
import com.mdframe.forge.starter.idempotent.service.RedisTokenService;
import com.mdframe.forge.starter.idempotent.service.TokenRedisFixture;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenRequiredStrategyHandlerTest {
    private final TokenRedisFixture fixture = new TokenRedisFixture();
    private final TokenProperties properties = new TokenProperties();
    private final RedisTokenService tokens = new RedisTokenService(fixture.redis, properties);
    private final AtomicInteger executions = new AtomicInteger();
    private final IdempotentStrategyHandler delegate = (point, annotation, key) -> {
        executions.incrementAndGet();
        return point.proceed();
    };

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void should_readConfiguredHeaderAndConsumeOnce_before_enteringBusiness() throws Throwable {
        properties.setHeader("X-Fixture-Token");
        String token = tokens.generateToken("fixture");
        request(token);
        var point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn("ok");
        var handler = new TokenRequiredStrategyHandler(tokens, properties, delegate);
        assertThat(handler.handle(point, annotation(), "fixture-key")).isEqualTo("ok");
        assertThatThrownBy(() -> handler.handle(point, annotation(), "fixture-key"))
                .isInstanceOf(TokenInvalidException.class);
        assertThat(executions).hasValue(1);
        assertThat(tokens.isTokenConsumed(token, "fixture")).isTrue();
    }

    @Test
    void should_enterBusinessExactlyOnce_when_twoAspectInstancesReceiveConcurrentToken() throws Throwable {
        var first = aspect(new TokenRequiredStrategyHandler(tokens, properties, delegate));
        var secondTokens = new RedisTokenService(fixture.redis, properties);
        var second = aspect(new TokenRequiredStrategyHandler(secondTokens, properties, delegate));
        String token = tokens.generateToken("fixture");
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(8);
        pool.setMaxPoolSize(8);
        pool.setQueueCapacity(32);
        pool.initialize();
        var gate = new CountDownLatch(1);
        var point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn("ok");
        try {
            var results = new ArrayList<Future<Boolean>>();
            for (int index = 0; index < 32; index++) {
                IdempotentAspect aspect = index % 2 == 0 ? first : second;
                results.add(pool.submit(() -> invoke(aspect, point, token, gate)));
            }
            gate.countDown();
            int accepted = 0;
            for (var result : results) {
                accepted += result.get(5, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertThat(accepted).isEqualTo(1);
            assertThat(executions).hasValue(1);
            assertThat(secondTokens.isTokenConsumed(token, "fixture")).isTrue();
        } finally {
            gate.countDown();
            pool.shutdown();
        }
    }

    @Test
    void should_rejectMissingRequestAndWrongHeader_without_businessExecution() {
        var handler = new TokenRequiredStrategyHandler(tokens, properties, delegate);
        var point = mock(ProceedingJoinPoint.class);
        assertThatThrownBy(() -> handler.handle(point, annotation(), "fixture"))
                .isInstanceOf(TokenInvalidException.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        assertThatThrownBy(() -> handler.handle(point, annotation(), "fixture"))
                .isInstanceOf(TokenInvalidException.class);
        assertThat(executions).hasValue(0);
    }

    @Test
    void should_notResetConsumedToken_when_businessFails() throws Throwable {
        String token = tokens.generateToken("fixture");
        request(token);
        var point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenThrow(new IllegalStateException("fixture business error"));
        var handler = new TokenRequiredStrategyHandler(tokens, properties, delegate);
        assertThatThrownBy(() -> handler.handle(point, annotation(), "fixture"))
                .hasMessage("fixture business error");
        assertThat(tokens.tryConsumeToken(token, "fixture")).isFalse();
        assertThat(executions).hasValue(1);
    }

    @Test
    void should_failClosed_when_atomicStoreIsUnavailable() {
        request(tokens.generateToken("fixture"));
        fixture.available = false;
        var handler = new TokenRequiredStrategyHandler(tokens, properties, delegate);
        assertThatThrownBy(() -> handler.handle(mock(ProceedingJoinPoint.class), annotation(), "fixture"))
                .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
        assertThat(executions).hasValue(0);
    }

    @Test
    void should_consumeDefaultEndpointToken_when_annotationUsesDefaultPrefix() throws Throwable {
        request(tokens.generateToken(null));
        var handler = new TokenRequiredStrategyHandler(tokens, properties, delegate);
        var point = mock(ProceedingJoinPoint.class);
        when(point.proceed()).thenReturn("ok");
        var annotation = Example.class.getMethod("defaultPerform").getAnnotation(Idempotent.class);
        assertThat(handler.handle(point, annotation, "fixture-key")).isEqualTo("ok");
        assertThat(executions).hasValue(1);
    }

    private IdempotentAspect aspect(IdempotentStrategyHandler handler) {
        var generator = mock(IdempotentKeyGenerator.class);
        when(generator.generate(any(), anyString(), anyString())).thenReturn("fixture-key");
        return new IdempotentAspect(generator, new IdempotentProperties(),
                Map.of(IdempotentStrategy.TOKEN_REQUIRED, handler));
    }

    private boolean invoke(IdempotentAspect aspect, ProceedingJoinPoint point, String token, CountDownLatch gate)
            throws Exception {
        gate.await(3, TimeUnit.SECONDS);
        request(token);
        try {
            return "ok".equals(aspect.around(point, annotation()));
        } catch (TokenInvalidException ex) {
            return false;
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void request(String token) {
        var request = new MockHttpServletRequest();
        request.addHeader(properties.getHeader(), token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Idempotent annotation() throws NoSuchMethodException {
        return Example.class.getMethod("perform").getAnnotation(Idempotent.class);
    }

    static class Example {
        @Idempotent(prefix = "fixture", strategy = IdempotentStrategy.TOKEN_REQUIRED)
        public void perform() { }

        @Idempotent(strategy = IdempotentStrategy.TOKEN_REQUIRED)
        public void defaultPerform() { }
    }
}
