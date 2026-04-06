package com.mdframe.forge.starter.idempotent.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("分布式锁管理器单元测试")
class RedissonLockManagerTest {
    
    @Mock
    private RedissonClient redissonClient;
    
    @Mock
    private RLock rLock;
    
    private RedissonLockManager lockManager;
    
    @BeforeEach
    void setUp() {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lockManager = new RedissonLockManager(redissonClient);
    }
    
    @Test
    @DisplayName("获取锁 - 成功")
    void testTryLock_Success() throws InterruptedException {
        String lockKey = "test-lock";
        long waitTime = 3000;
        long leaseTime = 5000;
        
        when(rLock.tryLock(eq(waitTime), eq(leaseTime), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        
        boolean acquired = lockManager.tryLock(lockKey, waitTime, leaseTime);
        
        assertTrue(acquired);
        verify(redissonClient).getLock(contains(lockKey));
        verify(rLock).tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
    }
    
    @Test
    @DisplayName("获取锁 - 失败")
    void testTryLock_Failed() throws InterruptedException {
        String lockKey = "test-lock";
        long waitTime = 3000;
        long leaseTime = 5000;
        
        when(rLock.tryLock(eq(waitTime), eq(leaseTime), eq(TimeUnit.MILLISECONDS))).thenReturn(false);
        
        boolean acquired = lockManager.tryLock(lockKey, waitTime, leaseTime);
        
        assertFalse(acquired);
    }
    
    @Test
    @DisplayName("获取锁 - 被中断")
    void testTryLock_Interrupted() throws InterruptedException {
        String lockKey = "test-lock";
        long waitTime = 3000;
        long leaseTime = 5000;
        
        when(rLock.tryLock(eq(waitTime), eq(leaseTime), eq(TimeUnit.MILLISECONDS)))
            .thenThrow(new InterruptedException());
        
        boolean acquired = lockManager.tryLock(lockKey, waitTime, leaseTime);
        
        assertFalse(acquired);
        assertTrue(Thread.currentThread().isInterrupted());
    }
    
    @Test
    @DisplayName("释放锁 - 当前线程持有")
    void testUnlock_HeldByCurrentThread() {
        String lockKey = "test-lock";
        
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        
        lockManager.unlock(lockKey);
        
        verify(rLock).unlock();
    }
    
    @Test
    @DisplayName("释放锁 - 非当前线程持有")
    void testUnlock_NotHeldByCurrentThread() {
        String lockKey = "test-lock";
        
        when(rLock.isHeldByCurrentThread()).thenReturn(false);
        
        lockManager.unlock(lockKey);
        
        verify(rLock, never()).unlock();
    }
    
    @Test
    @DisplayName("检查锁状态 - 已锁定")
    void testIsLocked_True() {
        String lockKey = "test-lock";
        
        when(rLock.isLocked()).thenReturn(true);
        
        boolean locked = lockManager.isLocked(lockKey);
        
        assertTrue(locked);
    }
    
    @Test
    @DisplayName("检查锁状态 - 未锁定")
    void testIsLocked_False() {
        String lockKey = "test-lock";
        
        when(rLock.isLocked()).thenReturn(false);
        
        boolean locked = lockManager.isLocked(lockKey);
        
        assertFalse(locked);
    }
}