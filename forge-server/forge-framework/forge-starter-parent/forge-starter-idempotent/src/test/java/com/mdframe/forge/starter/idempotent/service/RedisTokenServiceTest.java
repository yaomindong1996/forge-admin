package com.mdframe.forge.starter.idempotent.service;

import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
import com.mdframe.forge.starter.idempotent.exception.TokenInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.clearInvocations;

@Tag("dev")
@DisplayName("Token服务单元测试：原子状态和 TTL")
class RedisTokenServiceTest {
    private TokenRedisFixture fixture;
    private TokenProperties properties;
    private RedisTokenService service;

    @BeforeEach
    void setUp() {
        fixture = new TokenRedisFixture();
        properties = new TokenProperties();
        service = new RedisTokenService(fixture.redis, properties);
    }

    @Test
    @DisplayName("生成Token - 成功")
    void testGenerateToken_Success() {
        String token = service.generateToken("test");
        assertNotNull(token);
        assertEquals(32, token.length());
        assertEquals(300000, fixture.ttl(token, "test"));
        assertTrue(service.validateToken(token, "test"));
        assertFalse(service.isTokenConsumed(token, "test"));
    }

    @Test
    @DisplayName("验证Token - Token有效")
    void testValidateToken_Valid() {
        fixture.put("test-token", "test", "UNUSED", 1000);
        assertTrue(service.validateToken("test-token", "test"));
        assertEquals(1000, fixture.ttl("test-token", "test"));
    }

    @Test
    @DisplayName("验证Token - Token不存在")
    void testValidateToken_NotExists() {
        assertFalse(service.validateToken("test-token", "test"));
        assertEquals(-2, fixture.ttl("test-token", "test"));
    }

    @Test
    @DisplayName("验证Token - Token已消费")
    void testValidateToken_AlreadyConsumed() {
        fixture.put("test-token", "test", "CONSUMED", 1000);
        assertFalse(service.validateToken("test-token", "test"));
    }

    @Test
    @DisplayName("验证Token - Token为空")
    void testValidateToken_EmptyToken() {
        clearInvocations(fixture.redis);
        for (String value : new String[] {null, "", " ", "a:b", "x".repeat(129)}) {
            assertFalse(service.validateToken(value, "test"));
            assertFalse(service.tryConsumeToken(value, "test"));
            assertFalse(service.isTokenConsumed(value, "test"));
        }
        verifyNoMoreInteractions(fixture.redis);
    }

    @Test
    @DisplayName("消费Token - 成功并拒绝第二次消费")
    void testConsumeToken_Success() {
        fixture.put("test-token", "test", "UNUSED", 1000);
        service.consumeToken("test-token", "test");
        assertTrue(service.isTokenConsumed("test-token", "test"));
        assertEquals(60000, fixture.ttl("test-token", "test"));
        assertFalse(service.tryConsumeToken("test-token", "test"));
        assertThrows(TokenInvalidException.class, () -> service.consumeToken("test-token", "test"));
    }

    @Test
    @DisplayName("检查Token是否已消费 - 已消费")
    void testIsTokenConsumed_Consumed() {
        fixture.put("test-token", "test", "CONSUMED", 1000);
        assertTrue(service.isTokenConsumed("test-token", "test"));
    }

    @Test
    @DisplayName("检查Token是否已消费 - 未消费")
    void testIsTokenConsumed_NotConsumed() {
        fixture.put("test-token", "test", "UNUSED", 1000);
        assertFalse(service.isTokenConsumed("test-token", "test"));
    }

    @Test
    @DisplayName("生成Token - 默认前缀")
    void testGenerateToken_DefaultPrefix() {
        String token = service.generateToken(null);
        assertTrue(service.validateToken(token, ""));
        assertTrue(service.validateToken(token, "idempotent:"));
        assertEquals(300000, fixture.ttl(token, "global"));
        assertFalse(service.tryConsumeToken(token, "foreign-prefix"));
        assertTrue(service.tryConsumeToken(token, "global"));
    }

    @Test
    @DisplayName("Token 状态缺失或未知必须失败关闭")
    void missingOrUnknownStatusCannotValidate() {
        for (String status : new String[] {null, "UNKNOWN", "CONSUMED"}) {
            fixture.put("fixture-token", "test", status, 1000);
            assertFalse(service.validateToken("fixture-token", "test"));
            assertFalse(service.tryConsumeToken("fixture-token", "test"));
            assertEquals(1000, fixture.ttl("fixture-token", "test"));
        }
    }

    @Test
    void should_notCreateGhostToken_when_missingOrExpired() {
        assertFalse(service.tryConsumeToken("missing", "test"));
        assertEquals(-2, fixture.ttl("missing", "test"));
        fixture.put("expired", "test", "UNUSED", 1000);
        fixture.advance(1000);
        assertFalse(service.validateToken("expired", "test"));
        assertFalse(service.tryConsumeToken("expired", "test"));
        assertEquals(-2, fixture.ttl("expired", "test"));
    }

    @Test
    void should_failClosed_when_redisOrConfigurationIsInvalid() {
        properties.setExpire(0);
        assertThrows(com.mdframe.forge.starter.core.exception.BusinessException.class,
                () -> service.generateToken(null));
        properties.setExpire(300);
        fixture.available = false;
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,
                () -> service.tryConsumeToken("fixture-token", "test"));
    }
}
