package com.mdframe.forge.starter.cache.managed.key;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.cache.managed.context.CacheIdentity;
import com.mdframe.forge.starter.cache.managed.context.CacheIdentityProvider;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 只对代码内可信 SpEL 求值，最终 Redis entry key 始终为摘要。
 */
public class ForgeCacheKeyResolver {

    private final ObjectMapper objectMapper;
    private final CacheIdentityProvider identityProvider;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public ForgeCacheKeyResolver(ObjectMapper objectMapper, CacheIdentityProvider identityProvider) {
        this.objectMapper = objectMapper;
        this.identityProvider = identityProvider;
    }

    public Optional<String> resolve(Method method, Object target, Object[] args, String expression,
                                    CacheScope scope, Object result) {
        Optional<String> scopeMaterial = resolveScope(scope);
        if (scopeMaterial.isEmpty()) {
            return Optional.empty();
        }
        Object keyValue = resolveKeyValue(method, target, args, expression, result);
        String material = scopeMaterial.get() + "|" + toJson(keyValue);
        return Optional.of(sha256(material));
    }

    private Object resolveKeyValue(Method method, Object target, Object[] args, String expression, Object result) {
        Object[] safeArgs = args == null ? new Object[0] : args;
        if (!StringUtils.hasText(expression)) {
            return Arrays.asList(safeArgs);
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                target, method, safeArgs, parameterNameDiscoverer);
        context.setVariable("result", result);
        return expressionParser.parseExpression(expression).getValue(context);
    }

    private Optional<String> resolveScope(CacheScope scope) {
        if (scope == CacheScope.GLOBAL) {
            return Optional.of("global");
        }
        CacheIdentity identity = identityProvider.current();
        if (identity == null || identity.tenantId() == null) {
            return Optional.empty();
        }
        if (scope == CacheScope.TENANT) {
            return Optional.of("tenant:" + identity.tenantId());
        }
        if (identity.userId() == null) {
            return Optional.empty();
        }
        if (scope == CacheScope.TENANT_USER) {
            return Optional.of("tenant:" + identity.tenantId() + ":user:" + identity.userId());
        }
        String org = identity.activeOrgId() == null ? "none" : identity.activeOrgId().toString();
        return Optional.of("tenant:" + identity.tenantId() + ":user:" + identity.userId() + ":org:" + org);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法序列化缓存键材料", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JVM不支持SHA-256", exception);
        }
    }
}
