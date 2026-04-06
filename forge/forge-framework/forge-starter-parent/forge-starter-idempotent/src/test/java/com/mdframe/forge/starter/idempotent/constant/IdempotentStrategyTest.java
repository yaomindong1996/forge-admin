package com.mdframe.forge.starter.idempotent.constant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * IdempotentStrategy 枚举测试
 */
@Tag("dev")
class IdempotentStrategyTest {

    @Test
    void testStrategyValues() {
        // 验证枚举值存在
        IdempotentStrategy[] strategies = IdempotentStrategy.values();
        assertEquals(3, strategies.length, "应该有3个策略枚举值");
    }

    @Test
    void testStrictStrategy() {
        // 验证STRICT策略
        assertEquals("STRICT", IdempotentStrategy.STRICT.name());
        assertNotNull(IdempotentStrategy.STRICT.getDescription());
    }

    @Test
    void testReturnCacheStrategy() {
        // 验证RETURN_CACHE策略
        assertEquals("RETURN_CACHE", IdempotentStrategy.RETURN_CACHE.name());
        assertNotNull(IdempotentStrategy.RETURN_CACHE.getDescription());
    }

    @Test
    void testTokenRequiredStrategy() {
        // 验证TOKEN_REQUIRED策略
        assertEquals("TOKEN_REQUIRED", IdempotentStrategy.TOKEN_REQUIRED.name());
        assertNotNull(IdempotentStrategy.TOKEN_REQUIRED.getDescription());
    }

    @Test
    void testValueOf() {
        // 验证valueOf方法
        assertEquals(IdempotentStrategy.STRICT, IdempotentStrategy.valueOf("STRICT"));
        assertEquals(IdempotentStrategy.RETURN_CACHE, IdempotentStrategy.valueOf("RETURN_CACHE"));
        assertEquals(IdempotentStrategy.TOKEN_REQUIRED, IdempotentStrategy.valueOf("TOKEN_REQUIRED"));
    }
}
