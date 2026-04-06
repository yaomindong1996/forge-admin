package com.mdframe.forge.starter.idempotent.enums;

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
        assertEquals("strict", IdempotentStrategy.STRICT.getCode());
    }

    @Test
    void testReturnCacheStrategy() {
        // 验证RETURN_CACHE策略
        assertEquals("RETURN_CACHE", IdempotentStrategy.RETURN_CACHE.name());
        assertNotNull(IdempotentStrategy.RETURN_CACHE.getDescription());
        assertEquals("return_cache", IdempotentStrategy.RETURN_CACHE.getCode());
    }

    @Test
    void testTokenRequiredStrategy() {
        // 验证TOKEN_REQUIRED策略
        assertEquals("TOKEN_REQUIRED", IdempotentStrategy.TOKEN_REQUIRED.name());
        assertNotNull(IdempotentStrategy.TOKEN_REQUIRED.getDescription());
        assertEquals("token_required", IdempotentStrategy.TOKEN_REQUIRED.getCode());
    }

    @Test
    void testValueOf() {
        // 验证valueOf方法
        assertEquals(IdempotentStrategy.STRICT, IdempotentStrategy.valueOf("STRICT"));
        assertEquals(IdempotentStrategy.RETURN_CACHE, IdempotentStrategy.valueOf("RETURN_CACHE"));
        assertEquals(IdempotentStrategy.TOKEN_REQUIRED, IdempotentStrategy.valueOf("TOKEN_REQUIRED"));
    }

    @Test
    void testDescriptionContent() {
        assertEquals("严格拒绝重复请求", IdempotentStrategy.STRICT.getDescription());
        assertEquals("返回上次缓存结果", IdempotentStrategy.RETURN_CACHE.getDescription());
        assertEquals("必须携带有效Token", IdempotentStrategy.TOKEN_REQUIRED.getDescription());
    }

    @Test
    void testCodeContent() {
        assertEquals("strict", IdempotentStrategy.STRICT.getCode());
        assertEquals("return_cache", IdempotentStrategy.RETURN_CACHE.getCode());
        assertEquals("token_required", IdempotentStrategy.TOKEN_REQUIRED.getCode());
    }

    @Test
    void testFromCode() {
        assertEquals(IdempotentStrategy.STRICT, IdempotentStrategy.fromCode("strict"));
        assertEquals(IdempotentStrategy.RETURN_CACHE, IdempotentStrategy.fromCode("return_cache"));
        assertEquals(IdempotentStrategy.TOKEN_REQUIRED, IdempotentStrategy.fromCode("token_required"));
        // 验证未知code返回默认值RETURN_CACHE
        assertEquals(IdempotentStrategy.RETURN_CACHE, IdempotentStrategy.fromCode("unknown"));
        // 验证null返回默认值RETURN_CACHE
        assertEquals(IdempotentStrategy.RETURN_CACHE, IdempotentStrategy.fromCode(null));
    }

    @Test
    void testIsValid() {
        assertTrue(IdempotentStrategy.isValid("strict"));
        assertTrue(IdempotentStrategy.isValid("return_cache"));
        assertTrue(IdempotentStrategy.isValid("token_required"));
        assertFalse(IdempotentStrategy.isValid("unknown"));
        // 验证null返回false
        assertFalse(IdempotentStrategy.isValid(null));
    }

    @Test
    void testEnumOrder() {
        assertEquals(0, IdempotentStrategy.STRICT.ordinal());
        assertEquals(1, IdempotentStrategy.RETURN_CACHE.ordinal());
        assertEquals(2, IdempotentStrategy.TOKEN_REQUIRED.ordinal());
    }

    @Test
    void testGetterAnnotations() {
        // 验证@Getter注解生成的getter方法可用
        assertNotNull(IdempotentStrategy.STRICT.getCode());
        assertNotNull(IdempotentStrategy.STRICT.getDescription());
    }
}
