package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemAuthServiceImplClientCredentialTest {

    @Test
    void shouldAllowPublicClientWithoutSecretAfterValidatingAppId() {
        SysClient client = client("public-client", "public-app", "none");
        AtomicInteger secretValidationCount = new AtomicInteger();
        IClientService clientService = clientService(client, false, false, secretValidationCount);

        SysClient result = authService(clientService, false)
                .validateAndLoadClient("public-client", "public-app", null);

        assertThat(result).isSameAs(client);
        assertThat(secretValidationCount).hasValue(0);
    }

    @Test
    void shouldRejectMismatchedPublicClientAppIdWhenValidationSwitchIsOff() {
        SysClient client = client("public-client", "public-app", "none");
        AtomicInteger secretValidationCount = new AtomicInteger();
        IClientService clientService = clientService(client, false, false, secretValidationCount);

        assertThatThrownBy(() -> authService(clientService, false)
                .validateAndLoadClient("public-client", "different-app", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("客户端认证失败");
        assertThat(secretValidationCount).hasValue(0);
    }

    @Test
    void shouldValidateSecretForConfidentialClient() {
        SysClient client = client("server-client", "server-app", "client_secret");
        AtomicInteger secretValidationCount = new AtomicInteger();
        IClientService clientService = clientService(client, true, true, secretValidationCount);

        SysClient result = authService(clientService, true)
                .validateAndLoadClient("server-client", "server-app", "supplied-secret");

        assertThat(result).isSameAs(client);
        assertThat(secretValidationCount).hasValue(1);
    }

    @Test
    void shouldValidateConfidentialClientEvenWhenCompatibilitySwitchIsOff() {
        SysClient client = client("server-client", "server-app", "client_secret");
        AtomicInteger secretValidationCount = new AtomicInteger();
        IClientService clientService = clientService(client, true, true, secretValidationCount);

        SysClient result = authService(clientService, false)
                .validateAndLoadClient("server-client", "server-app", "supplied-secret");

        assertThat(result).isSameAs(client);
        assertThat(secretValidationCount).hasValue(1);
    }

    @Test
    void shouldReturnGenericFailureForUnknownDisabledOrInvalidClientCredentials() {
        AtomicInteger validationCount = new AtomicInteger();
        IClientService missingClientService = clientService(null, false, false, validationCount);
        assertThatThrownBy(() -> authService(missingClientService, true)
                .validateAndLoadClient("missing-client", "app", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("客户端认证失败");

        SysClient disabled = client("disabled-client", "app", "none");
        disabled.setStatus(0);
        assertThatThrownBy(() -> authService(
                clientService(disabled, false, false, validationCount), true)
                .validateAndLoadClient("disabled-client", "app", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("客户端认证失败");

        SysClient confidential = client("server-client", "server-app", "client_secret");
        assertThatThrownBy(() -> authService(
                clientService(confidential, true, false, validationCount), true)
                .validateAndLoadClient("server-client", "server-app", "invalid-secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("客户端认证失败");
    }

    @Test
    void shouldAllowOnlyPublicClientsAsSsoTargets() {
        AtomicInteger validationCount = new AtomicInteger();
        SysClient publicClient = client("public-client", "public-app", "none");
        assertThat(authService(
                clientService(publicClient, false, false, validationCount), true)
                .validateSsoTargetClient("public-client"))
                .isSameAs(publicClient);

        SysClient confidential = client("server-client", "server-app", "client_secret");
        assertThatThrownBy(() -> authService(
                clientService(confidential, true, false, validationCount), true)
                .validateSsoTargetClient("server-client"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("目标客户端不支持SSO");
    }

    private SystemAuthServiceImpl authService(IClientService clientService, boolean clientValidationEnabled) {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setEnableClientValidation(clientValidationEnabled);
        return new SystemAuthServiceImpl(
                null, null, null, null, null, authProperties, null,
                clientService, null, null, null);
    }

    private IClientService clientService(SysClient client, boolean requiresSecret,
                                         boolean secretMatches, AtomicInteger validationCount) {
        return (IClientService) Proxy.newProxyInstance(
                IClientService.class.getClassLoader(),
                new Class<?>[]{IClientService.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getByCode" -> client;
                    case "requiresAppSecret" -> requiresSecret;
                    case "validateAppSecret" -> {
                        validationCount.incrementAndGet();
                        yield secretMatches;
                    }
                    case "toString" -> "test-client-service";
                    default -> null;
                });
    }

    private SysClient client(String clientCode, String appId, String authMethod) {
        SysClient client = new SysClient();
        client.setClientCode(clientCode);
        client.setAppId(appId);
        client.setClientAuthMethod(authMethod);
        client.setStatus(1);
        return client;
    }
}
