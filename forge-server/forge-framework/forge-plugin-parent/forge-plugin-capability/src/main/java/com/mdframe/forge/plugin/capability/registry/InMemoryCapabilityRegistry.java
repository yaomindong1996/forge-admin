package com.mdframe.forge.plugin.capability.registry;

import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import com.mdframe.forge.plugin.capability.model.CapabilityAuthorizationDecision;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityCursor;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityPage;
import com.mdframe.forge.plugin.capability.model.CapabilityQuery;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidationException;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.spi.CapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.capability.spi.CapabilityExecutor;
import com.mdframe.forge.plugin.capability.spi.CapabilityInvocationObserver;
import com.mdframe.forge.plugin.capability.spi.CapabilitySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class InMemoryCapabilityRegistry implements CapabilityRegistry {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCapabilityRegistry.class);
    private static final Pattern SEMANTIC_VERSION = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");
    private static final String SORT_DELIMITER = "\u0000";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Map<String, RegisteredCapability> byCapabilityKey;
    private final Map<String, RegisteredCapability> bySortKey;
    private final CapabilityAuthorizationPolicy authorizationPolicy;
    private final CapabilitySchemaValidator schemaValidator;
    private final String snapshotVersion;
    private final byte[] cursorSigningKey;
    private final List<CapabilityInvocationObserver> invocationObservers;

    public InMemoryCapabilityRegistry(
            Collection<CapabilitySource> sources,
            Collection<CapabilityExecutor> executors,
            CapabilityAuthorizationPolicy authorizationPolicy,
            CapabilityToolNameMapper nameMapper,
            CapabilitySchemaValidator schemaValidator) {
        this(sources, executors, authorizationPolicy, nameMapper, schemaValidator, List.of());
    }

    public InMemoryCapabilityRegistry(
            Collection<CapabilitySource> sources,
            Collection<CapabilityExecutor> executors,
            CapabilityAuthorizationPolicy authorizationPolicy,
            CapabilityToolNameMapper nameMapper,
            CapabilitySchemaValidator schemaValidator,
            Collection<CapabilityInvocationObserver> invocationObservers) {
        this.authorizationPolicy = Objects.requireNonNull(authorizationPolicy, "authorizationPolicy 不能为空");
        this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator 不能为空");
        this.invocationObservers = List.copyOf(invocationObservers);
        Objects.requireNonNull(nameMapper, "nameMapper 不能为空");
        List<CapabilityExecutor> executorList = List.copyOf(executors);
        List<CapabilityDefinition> definitions = loadDefinitions(sources);

        Map<String, RegisteredCapability> capabilityEntries = new LinkedHashMap<>();
        Map<String, RegisteredCapability> sortedEntries = new TreeMap<>();
        Map<String, String> toolNames = new LinkedHashMap<>();
        for (CapabilityDefinition definition : definitions) {
            validateDefinition(definition, nameMapper);
            if (capabilityEntries.containsKey(definition.key())) {
                throw definitionError("能力定义重复：" + definition.key());
            }
            String existingKey = toolNames.putIfAbsent(definition.protocolToolName(), definition.key());
            if (existingKey != null) {
                throw definitionError("协议工具名重复：" + definition.protocolToolName());
            }
            List<CapabilityExecutor> matchedExecutors = executorList.stream()
                    .filter(executor -> executor.supports(definition))
                    .toList();
            if (matchedExecutors.size() != 1) {
                throw definitionError("能力必须且只能匹配一个 Executor：" + definition.key());
            }
            RegisteredCapability registered = new RegisteredCapability(definition, matchedExecutors.get(0));
            capabilityEntries.put(definition.key(), registered);
            sortedEntries.put(sortKey(definition), registered);
        }
        this.byCapabilityKey = Map.copyOf(capabilityEntries);
        this.bySortKey = Map.copyOf(sortedEntries);
        this.snapshotVersion = calculateSnapshotVersion(sortedEntries);
        this.cursorSigningKey = generateCursorSigningKey();
    }

    @Override
    public CapabilityPage list(CapabilityQuery query, CapabilityCallerContext caller) {
        Objects.requireNonNull(query, "query 不能为空");
        Objects.requireNonNull(caller, "caller 不能为空");
        String queryFingerprint = calculateQueryFingerprint(query, caller);
        String lastSortKey = null;
        if (query.cursor() != null) {
            CapabilityCursor cursor = CapabilityCursor.decode(query.cursor());
            if (!isValidCursorSignature(cursor)) {
                throw new IllegalArgumentException("游标签名无效");
            }
            if (!snapshotVersion.equals(cursor.snapshotVersion())) {
                throw new IllegalArgumentException("游标已过期或不属于当前能力快照");
            }
            if (!queryFingerprint.equals(cursor.queryFingerprint())) {
                throw new IllegalArgumentException("游标不属于当前查询或调用方");
            }
            RegisteredCapability cursorCapability = bySortKey.get(cursor.lastSortKey());
            if (cursorCapability == null
                    || !matchesPrefix(query, cursorCapability.definition())
                    || !authorizationPolicy.canDiscover(cursorCapability.definition(), caller)) {
                throw new IllegalArgumentException("游标位置无效或调用方不可见");
            }
            lastSortKey = cursor.lastSortKey();
        }

        String cursorSortKey = lastSortKey;
        List<Map.Entry<String, RegisteredCapability>> authorized = bySortKey.entrySet().stream()
                .filter(entry -> cursorSortKey == null || entry.getKey().compareTo(cursorSortKey) > 0)
                .filter(entry -> matchesPrefix(query, entry.getValue().definition()))
                .filter(entry -> authorizationPolicy.canDiscover(entry.getValue().definition(), caller))
                .sorted(Map.Entry.comparingByKey())
                .toList();
        int resultSize = Math.min(query.pageSize(), authorized.size());
        List<CapabilityDefinition> items = authorized.subList(0, resultSize).stream()
                .map(entry -> entry.getValue().definition())
                .toList();
        String nextCursor = null;
        if (authorized.size() > resultSize && resultSize > 0) {
            String nextSortKey = authorized.get(resultSize - 1).getKey();
            nextCursor = new CapabilityCursor(
                    snapshotVersion,
                    queryFingerprint,
                    nextSortKey,
                    cursorSignature(snapshotVersion, queryFingerprint, nextSortKey)).encode();
        }
        return new CapabilityPage(items, nextCursor, snapshotVersion);
    }

    @Override
    public CapabilityDefinition requireActive(String capabilityCode, String version) {
        RegisteredCapability registered = byCapabilityKey.get(capabilityCode + "@" + version);
        if (registered == null) {
            throw new IllegalArgumentException("能力不存在或未启用");
        }
        return registered.definition();
    }

    @Override
    public CapabilityResult invoke(CapabilityInvocation invocation) {
        long startedAt = System.nanoTime();
        RegisteredCapability registered = byCapabilityKey.get(
                invocation.capabilityCode() + "@" + invocation.version());
        if (registered == null) {
            return audit(invocation, error(invocation, CapabilityErrorCode.CAPABILITY_NOT_FOUND,
                    "能力不存在或未启用", startedAt));
        }
        CapabilityAuthorizationDecision authorization = authorizationPolicy.evaluateInvocation(
                registered.definition(), invocation.caller());
        if (!authorization.allowed()) {
            return audit(invocation, error(invocation, authorization.errorCode(),
                    "当前调用方无权执行该能力", startedAt));
        }
        try {
            schemaValidator.validateInstance(registered.definition().inputSchema(), invocation.arguments());
        }
        catch (CapabilitySchemaValidationException exception) {
            return audit(invocation, error(invocation, CapabilityErrorCode.INVALID_ARGUMENT,
                    "能力参数不符合输入规范", startedAt), exception.getPath());
        }

        try {
            CapabilityResult result = registered.executor().invoke(registered.definition(), invocation);
            if (!invocation.requestId().equals(result.requestId())
                    || !invocation.capabilityCode().equals(result.capabilityCode())) {
                return audit(invocation, error(invocation, CapabilityErrorCode.INTERNAL_ERROR,
                        "能力返回结果关联信息无效", startedAt));
            }
            if (result.status() == CapabilityResultStatus.SUCCESS) {
                try {
                    schemaValidator.validateInstance(registered.definition().outputSchema(), result.data());
                }
                catch (CapabilitySchemaValidationException exception) {
                    return audit(invocation, error(invocation, CapabilityErrorCode.OUTPUT_SCHEMA_INVALID,
                            "能力返回结果不符合输出规范", startedAt), exception.getPath());
                }
            }
            return audit(invocation, result);
        }
        catch (RuntimeException exception) {
            return audit(invocation, error(invocation, CapabilityErrorCode.EXECUTION_FAILED,
                    "能力执行失败，请稍后重试", startedAt));
        }
    }

    public int size() {
        return byCapabilityKey.size();
    }

    private List<CapabilityDefinition> loadDefinitions(Collection<CapabilitySource> sources) {
        List<CapabilityDefinition> definitions = new ArrayList<>();
        for (CapabilitySource source : sources) {
            Collection<CapabilityDefinition> loaded = source.load(CapabilityQuery.all());
            if (loaded != null) {
                definitions.addAll(loaded);
            }
        }
        definitions.sort(Comparator.comparing(InMemoryCapabilityRegistry::sortKey));
        return definitions;
    }

    private void validateDefinition(CapabilityDefinition definition, CapabilityToolNameMapper nameMapper) {
        String mappedName = nameMapper.toProtocolToolName(definition.capabilityCode());
        if (!mappedName.equals(definition.protocolToolName())) {
            throw definitionError("protocolToolName 必须等于稳定映射结果：" + definition.capabilityCode());
        }
        if (!SEMANTIC_VERSION.matcher(definition.version()).matches()) {
            throw definitionError("能力版本必须使用三段语义版本：" + definition.key());
        }
        schemaValidator.validateDefinition(definition.inputSchema());
        schemaValidator.validateDefinition(definition.outputSchema());
    }

    private CapabilityResult error(
            CapabilityInvocation invocation,
            CapabilityErrorCode errorCode,
            String message,
            long startedAt) {
        long durationMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        return CapabilityResult.error(invocation.requestId(), invocation.capabilityCode(),
                errorCode, message, durationMs);
    }

    private CapabilityResult audit(CapabilityInvocation invocation, CapabilityResult result) {
        return audit(invocation, result, null);
    }

    private CapabilityResult audit(
            CapabilityInvocation invocation,
            CapabilityResult result,
            String schemaPath) {
        CapabilityCallerContext caller = invocation.caller();
        String resultCode = result.errorCode() == null ? result.status().name() : result.errorCode();
        if (result.status() == CapabilityResultStatus.SUCCESS) {
            log.info("[Capability调用] requestId={}, clientRef={}, tenantId={}, activeOrgId={}, capabilityCode={}, resultCode={}, schemaPath={}, durationMs={}",
                    result.requestId(), clientReference(caller.machineClientId()), caller.tenantId(),
                    caller.activeOrgId(), invocation.capabilityCode(), resultCode, schemaPath, result.durationMs());
        }
        else {
            log.warn("[Capability调用] requestId={}, clientRef={}, tenantId={}, activeOrgId={}, capabilityCode={}, resultCode={}, schemaPath={}, durationMs={}",
                    result.requestId(), clientReference(caller.machineClientId()), caller.tenantId(),
                    caller.activeOrgId(), invocation.capabilityCode(), resultCode, schemaPath, result.durationMs());
        }
        for (CapabilityInvocationObserver observer : invocationObservers) {
            observer.onCompleted(invocation, result, schemaPath);
        }
        return result;
    }

    private boolean matchesPrefix(CapabilityQuery query, CapabilityDefinition definition) {
        return query.capabilityCodePrefix() == null
                || definition.capabilityCode().startsWith(query.capabilityCodePrefix());
    }

    private String calculateQueryFingerprint(CapabilityQuery query, CapabilityCallerContext caller) {
        String scopes = caller.scopes().stream().sorted().collect(Collectors.joining(","));
        String material = Objects.toString(query.capabilityCodePrefix(), "") + "\n"
                + caller.machineClientId() + "\n"
                + caller.tenantId() + "\n"
                + Objects.toString(caller.userId(), "") + "\n"
                + Objects.toString(caller.activeOrgId(), "") + "\n"
                + scopes;
        return shortHash(material, 12);
    }

    private String clientReference(String machineClientId) {
        return shortHash(machineClientId, 6);
    }

    private byte[] generateCursorSigningKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private boolean isValidCursorSignature(CapabilityCursor cursor) {
        String expected = cursorSignature(
                cursor.snapshotVersion(), cursor.queryFingerprint(), cursor.lastSortKey());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                cursor.signature().getBytes(StandardCharsets.US_ASCII));
    }

    private String cursorSignature(
            String cursorSnapshotVersion,
            String queryFingerprint,
            String lastSortKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(cursorSigningKey, HMAC_ALGORITHM));
            String material = cursorSnapshotVersion + "\n" + queryFingerprint + "\n" + lastSortKey;
            return HexFormat.of().formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        }
        catch (GeneralSecurityException exception) {
            throw new IllegalStateException("当前 JDK 不支持游标签名算法", exception);
        }
    }

    private static String sortKey(CapabilityDefinition definition) {
        return definition.protocolToolName() + SORT_DELIMITER + definition.version();
    }

    private String calculateSnapshotVersion(Map<String, RegisteredCapability> definitions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            definitions.forEach((key, value) -> {
                digest.update(key.getBytes(StandardCharsets.UTF_8));
                digest.update(value.definition().inputSchema().toString().getBytes(StandardCharsets.UTF_8));
                digest.update(value.definition().outputSchema().toString().getBytes(StandardCharsets.UTF_8));
            });
            return HexFormat.of().formatHex(digest.digest(), 0, 12);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private String shortHash(String value, int byteLength) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest(), 0, byteLength);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private CapabilityDefinitionException definitionError(String message) {
        return new CapabilityDefinitionException(CapabilityErrorCode.INVALID_ARGUMENT, message);
    }

    private record RegisteredCapability(
            CapabilityDefinition definition,
            CapabilityExecutor executor) {
    }
}
