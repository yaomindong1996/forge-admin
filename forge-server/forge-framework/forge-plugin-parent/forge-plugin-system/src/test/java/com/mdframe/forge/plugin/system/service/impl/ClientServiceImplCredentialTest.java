package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.SysClientDTO;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.mapper.SysClientMapper;
import com.mdframe.forge.plugin.system.security.ClientCredentialPolicy;
import com.mdframe.forge.plugin.system.security.ClientSecretCodec;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientServiceImplCredentialTest {

    @Test
    void shouldBypassStaleCacheWhenCredentialConfigurationChanges() {
        SysClient staleClient = client(20L, "rotating-client", "client_secret", "old-stored-value");
        SysClient currentClient = client(20L, "rotating-client", "client_secret", "new-stored-value");
        AtomicReference<String> cacheAccess = new AtomicReference<>();
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) ->
                "selectByClientCode".equals(methodName) ? currentClient : defaultValue(methodName));
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) -> {
            if ("get".equals(methodName) || "set".equals(methodName)) {
                cacheAccess.set(methodName);
            }
            return "get".equals(methodName) ? staleClient : defaultValue(methodName);
        });
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        SysClient loaded = service.getByCode("rotating-client");

        assertThat(loaded).isSameAs(currentClient);
        assertThat(cacheAccess.get()).isNull();
    }

    @Test
    void shouldConditionallyUpgradeMatchedLegacySecretAndEvictCache() {
        AtomicReference<Object[]> updateArguments = new AtomicReference<>();
        AtomicReference<String> deletedCacheKey = new AtomicReference<>();
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> {
            if ("updateAppSecretIfUnchanged".equals(methodName)) {
                updateArguments.set(arguments);
                return 1;
            }
            return defaultValue(methodName);
        });
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) -> {
            if ("delete".equals(methodName)) {
                deletedCacheKey.set((String) arguments[0]);
                return true;
            }
            return defaultValue(methodName);
        });
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));
        SysClient client = client(7L, "server-client", "client_secret", "legacy-secret");

        boolean matched = service.validateAppSecret(client, "legacy-secret");

        assertThat(matched).isTrue();
        assertThat(updateArguments.get()[0]).isEqualTo(7L);
        assertThat(updateArguments.get()[1]).isEqualTo("client_secret");
        assertThat(updateArguments.get()[2]).isEqualTo("legacy-secret");
        assertThat((String) updateArguments.get()[3]).startsWith("{bcrypt}");
        assertThat(deletedCacheKey.get()).isEqualTo("client:config:server-client");
    }

    @Test
    void shouldNotEvictCacheWhenConditionalLegacyUpgradeLosesRace() {
        AtomicReference<String> deletedCacheKey = new AtomicReference<>();
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) ->
                "updateAppSecretIfUnchanged".equals(methodName) ? 0 : defaultValue(methodName));
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) -> {
            if ("delete".equals(methodName)) {
                deletedCacheKey.set((String) arguments[0]);
                return true;
            }
            return defaultValue(methodName);
        });
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        boolean matched = service.validateAppSecret(
                client(12L, "racing-client", "client_secret", "legacy-secret"), "legacy-secret");

        assertThat(matched).isTrue();
        assertThat(deletedCacheKey.get()).isNull();
    }

    @Test
    void shouldNotAcceptSecretForPublicClient() {
        SysClientMapper mapper = proxy(SysClientMapper.class,
                (methodName, arguments) -> defaultValue(methodName));
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) ->
                "delete".equals(methodName) ? true : defaultValue(methodName));
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        assertThat(service.validateAppSecret(
                client(8L, "public-client", "none", null), "supplied-secret"))
                .isFalse();
    }

    @Test
    void shouldRejectLegacySecretWhenCompatibilityReadIsDisabled() {
        AtomicReference<Object[]> updateArguments = new AtomicReference<>();
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> {
            if ("updateAppSecretIfUnchanged".equals(methodName)) {
                updateArguments.set(arguments);
            }
            return defaultValue(methodName);
        });
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) ->
                "delete".equals(methodName) ? true : defaultValue(methodName));
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(false));

        boolean matched = service.validateAppSecret(
                client(9L, "legacy-client", "client_secret", "legacy-secret"), "legacy-secret");

        assertThat(matched).isFalse();
        assertThat(updateArguments.get()).isNull();
    }

    @Test
    void shouldApplyConfidentialRotationAndPublicClientClearing() {
        String existingSecret = new ClientSecretCodec().encode("existing-client-secret-123456");
        AtomicReference<Object[]> atomicUpdateArguments = new AtomicReference<>();
        AtomicInteger atomicUpdateCount = new AtomicInteger();
        SysClient existing = client(10L, "managed-client", "client_secret", existingSecret);
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> switch (methodName) {
            case "selectById" -> existing;
            case "updateClientIfUnchanged" -> {
                atomicUpdateArguments.set(arguments);
                atomicUpdateCount.incrementAndGet();
                yield 1;
            }
            default -> defaultValue(methodName);
        });
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) ->
                "delete".equals(methodName) ? true : defaultValue(methodName));
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        SysClientDTO preserve = updateRequest(10L, "managed-client", "client_secret", "");
        assertThat(service.updateClient(preserve)).isTrue();
        assertThat(atomicUpdateCount.get()).isEqualTo(1);
        assertThat(atomicUpdateArguments.get()[1]).isEqualTo("client_secret");
        assertThat(atomicUpdateArguments.get()[2]).isEqualTo(existingSecret);
        assertThat(atomicUpdateArguments.get()[3]).isEqualTo(existingSecret);
        assertThat(atomicUpdateArguments.get()[4]).isEqualTo(false);

        SysClientDTO rotate = updateRequest(
                10L, "managed-client", "client_secret", "rotated-client-secret-123456");
        assertThat(service.updateClient(rotate)).isTrue();
        assertThat(atomicUpdateCount.get()).isEqualTo(2);
        assertThat(atomicUpdateArguments.get()[1]).isEqualTo("client_secret");
        assertThat(atomicUpdateArguments.get()[2]).isEqualTo(existingSecret);
        assertThat((String) atomicUpdateArguments.get()[3]).startsWith("{bcrypt}").isNotEqualTo(existingSecret);
        assertThat(atomicUpdateArguments.get()[4]).isEqualTo(true);

        SysClientDTO makePublic = updateRequest(10L, "managed-client", "none", null);
        assertThat(service.updateClient(makePublic)).isTrue();
        assertThat(atomicUpdateCount.get()).isEqualTo(3);
        assertThat(atomicUpdateArguments.get()[1]).isEqualTo("client_secret");
        assertThat(atomicUpdateArguments.get()[2]).isEqualTo(existingSecret);
        assertThat(atomicUpdateArguments.get()[3]).isNull();
        assertThat(atomicUpdateArguments.get()[4]).isEqualTo(true);
    }

    @Test
    void shouldRejectSecretRotationWhenStoredValueChangesConcurrently() {
        String existingSecret = new ClientSecretCodec().encode("existing-client-secret-123456");
        AtomicReference<Object[]> conditionalUpdateArguments = new AtomicReference<>();
        SysClient existing = client(13L, "concurrent-client", "client_secret", existingSecret);
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> switch (methodName) {
            case "selectById" -> existing;
            case "updateClientIfUnchanged" -> {
                conditionalUpdateArguments.set(arguments);
                yield 0;
            }
            default -> defaultValue(methodName);
        });
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                proxy(ICacheService.class, (methodName, arguments) -> defaultValue(methodName)),
                mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        assertThatThrownBy(() -> service.updateClient(updateRequest(
                13L, "concurrent-client", "client_secret", "rotated-client-secret-123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户端更新失败");
        assertThat(conditionalUpdateArguments.get()[1]).isEqualTo("client_secret");
        assertThat(conditionalUpdateArguments.get()[2]).isEqualTo(existingSecret);
        assertThat((String) conditionalUpdateArguments.get()[3]).startsWith("{bcrypt}");
        assertThat(conditionalUpdateArguments.get()[4]).isEqualTo(true);
    }

    @Test
    void shouldRejectMetadataUpdateWhenAuthMethodOrSecretSnapshotIsStale() {
        AtomicReference<Object[]> atomicUpdateArguments = new AtomicReference<>();
        SysClient existing = client(14L, "metadata-client", "none", null);
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> switch (methodName) {
            case "selectById" -> existing;
            case "updateClientIfUnchanged" -> {
                atomicUpdateArguments.set(arguments);
                yield 0;
            }
            default -> defaultValue(methodName);
        });
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                proxy(ICacheService.class, (methodName, arguments) -> defaultValue(methodName)),
                mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        assertThatThrownBy(() -> service.updateClient(updateRequest(
                14L, "metadata-client", "none", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("客户端更新失败");
        assertThat(atomicUpdateArguments.get()[1]).isEqualTo("none");
        assertThat(atomicUpdateArguments.get()[2]).isNull();
        assertThat(atomicUpdateArguments.get()[3]).isNull();
        assertThat(atomicUpdateArguments.get()[4]).isEqualTo(false);
    }

    @Test
    void shouldEvictClientCacheOnlyAfterTransactionCommit() {
        AtomicReference<String> deletedCacheKey = new AtomicReference<>();
        SysClient existing = client(11L, "transactional-client", "client_secret",
                new ClientSecretCodec().encode("existing-client-secret-123456"));
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> switch (methodName) {
            case "selectById" -> existing;
            case "updateClientIfUnchanged", "updateAppSecretIfUnchanged" -> 1;
            default -> defaultValue(methodName);
        });
        ICacheService cacheService = proxy(ICacheService.class, (methodName, arguments) -> {
            if ("delete".equals(methodName)) {
                deletedCacheKey.set((String) arguments[0]);
                return true;
            }
            return defaultValue(methodName);
        });
        ClientSecretCodec codec = new ClientSecretCodec();
        ClientServiceImpl service = new ClientServiceImpl(
                cacheService, mapper, codec, new ClientCredentialPolicy(codec), authProperties(true));

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.updateClient(updateRequest(
                    11L, "transactional-client", "client_secret", "rotated-client-secret-123456"));
            assertThat(deletedCacheKey.get()).isNull();

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            assertThat(deletedCacheKey.get()).isEqualTo("client:config:transactional-client");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private SysClientDTO updateRequest(Long id, String code, String authMethod, String rawSecret) {
        SysClientDTO request = new SysClientDTO();
        request.setId(id);
        request.setClientCode(code);
        request.setClientAuthMethod(authMethod);
        request.setAppSecret(rawSecret);
        return request;
    }

    private AuthProperties authProperties(boolean allowLegacy) {
        AuthProperties properties = new AuthProperties();
        properties.setEnableLegacyClientSecretRead(allowLegacy);
        return properties;
    }

    private SysClient client(Long id, String code, String authMethod, String storedSecret) {
        SysClient client = new SysClient();
        client.setId(id);
        client.setClientCode(code);
        client.setClientAuthMethod(authMethod);
        client.setAppSecret(storedSecret);
        return client;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    private Object defaultValue(String methodName) {
        if ("toString".equals(methodName)) {
            return "test-proxy";
        }
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] arguments);
    }
}
