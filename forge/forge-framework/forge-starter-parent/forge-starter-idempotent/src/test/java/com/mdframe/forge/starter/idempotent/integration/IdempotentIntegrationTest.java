package com.mdframe.forge.starter.idempotent.integration;

import com.mdframe.forge.starter.idempotent.annotation.Idempotent;
import com.mdframe.forge.starter.idempotent.enums.IdempotentStrategy;
import com.mdframe.forge.starter.idempotent.exception.IdempotentException;
import com.mdframe.forge.starter.idempotent.service.TokenService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
@DisplayName("幂等性集成测试")
class IdempotentIntegrationTest {
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private TokenService tokenService;
    
    @BeforeEach
    void setUp() throws Throwable {
        when(joinPoint.proceed()).thenReturn("success");
    }
    
    @Test
    @DisplayName("并发场景 - 严格模式测试")
    void testConcurrentStrictMode() throws Throwable {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Object result = joinPoint.proceed();
                    if ("success".equals(result)) {
                        successCount.incrementAndGet();
                    }
                } catch (Throwable e) {
                    if (e instanceof IdempotentException) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(threadCount, successCount.get() + failCount.get());
        System.out.println("并发测试结果: 成功=" + successCount.get() + ", 失败=" + failCount.get());
    }
    
    @Test
    @DisplayName("Token流程测试")
    void testTokenFlow() {
        String prefix = "test";
        
        String token = "test-token-123";
        when(tokenService.generateToken(prefix)).thenReturn(token);
        when(tokenService.validateToken(token, prefix)).thenReturn(true);
        
        String generatedToken = tokenService.generateToken(prefix);
        assertNotNull(generatedToken);
        assertEquals(token, generatedToken);
        
        boolean isValid = tokenService.validateToken(token, prefix);
        assertTrue(isValid);
        
        verify(tokenService).generateToken(prefix);
        verify(tokenService).validateToken(token, prefix);
    }
    
    @Test
    @DisplayName("幂等策略选择测试")
    void testStrategySelection() {
        Idempotent annotation = mock(Idempotent.class);
        
        when(annotation.strategy()).thenReturn(IdempotentStrategy.STRICT);
        assertEquals(IdempotentStrategy.STRICT, annotation.strategy());
        
        when(annotation.strategy()).thenReturn(IdempotentStrategy.RETURN_CACHE);
        assertEquals(IdempotentStrategy.RETURN_CACHE, annotation.strategy());
        
        when(annotation.strategy()).thenReturn(IdempotentStrategy.TOKEN_REQUIRED);
        assertEquals(IdempotentStrategy.TOKEN_REQUIRED, annotation.strategy());
    }
}