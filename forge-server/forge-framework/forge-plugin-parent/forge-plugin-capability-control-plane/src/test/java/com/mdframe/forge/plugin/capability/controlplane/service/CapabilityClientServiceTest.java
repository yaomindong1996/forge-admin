package com.mdframe.forge.plugin.capability.controlplane.service;

import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityClientCreateDTO;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientSecretCodec;
import com.mdframe.forge.plugin.capability.controlplane.security.IssuedClientSecret;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientSecretVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapabilityClientServiceTest {

    @Test
    void shouldPersistOnlyPrefixAndHashWhileReturningSecretOnce() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientSecretCodec codec = mock(CapabilityClientSecretCodec.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        CapabilityClientService service = new CapabilityClientService(mapper, codec, clock);
        String rawSecret = "fcp_client_a_test-only-raw-secret";
        String keyHash = "a".repeat(64);
        when(codec.issue("client_a"))
                .thenReturn(new IssuedClientSecret(
                        rawSecret, "key-id-12345678901234", "fcp_key-id-12345678901234", keyHash));
        doAnswer(invocation -> {
            invocation.<AiCapabilityClient>getArgument(0).setId(99L);
            return 1;
        }).when(mapper).insert(any(AiCapabilityClient.class));

        CapabilityClientSecretVO response = service.create(
                1L,
                new CapabilityClientCreateDTO("client_a", "测试客户端", 10L, 20L, null, null));

        ArgumentCaptor<AiCapabilityClient> captor = ArgumentCaptor.forClass(AiCapabilityClient.class);
        verify(mapper).insert(captor.capture());
        AiCapabilityClient stored = captor.getValue();
        assertThat(stored.getKeyId()).isEqualTo("key-id-12345678901234");
        assertThat(stored.getKeyPrefix()).isEqualTo("fcp_key-id-12345678901234");
        assertThat(stored.getKeyHash()).isEqualTo(keyHash);
        assertThat(stored.toString()).doesNotContain(rawSecret);
        assertThat(response.clientSecret()).isEqualTo(rawSecret);
        assertThat(response.clientId()).isEqualTo(99L);
    }

    @Test
    void shouldRejectInvalidServiceIdentityBeforeIssuingSecret() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientSecretCodec codec = mock(CapabilityClientSecretCodec.class);
        CapabilityClientService service = new CapabilityClientService(
                mapper, codec, Clock.systemUTC());

        assertThatThrownBy(() -> service.create(
                1L,
                new CapabilityClientCreateDTO("client_a", "测试客户端", 0L, 20L, null, null)))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class)
                .hasMessageContaining("服务账号");
        verify(codec, org.mockito.Mockito.never()).issue("client_a");
    }

    @Test
    void shouldRejectPersistedClientWithInvalidServiceIdentity() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientSecretCodec codec = mock(CapabilityClientSecretCodec.class);
        CapabilityClientService service = new CapabilityClientService(
                mapper, codec, Clock.systemUTC());
        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(99L);
        client.setTenantId(1L);
        client.setClientCode("client_a");
        client.setKeyHash("a".repeat(64));
        client.setStatus("ENABLED");
        client.setServiceUserId(0L);
        client.setActiveOrgId(20L);
        when(codec.extractKeyId("raw-secret")).thenReturn("key-id-12345678901234");
        when(mapper.selectCredentialByKeyId("key-id-12345678901234")).thenReturn(client);
        when(codec.matches("raw-secret", client.getKeyHash())).thenReturn(true);

        assertThatThrownBy(() -> service.authenticate("raw-secret"))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class)
                .hasMessageContaining("凭据无效");
        verify(mapper, org.mockito.Mockito.never())
                .updateById(org.mockito.ArgumentMatchers.any(AiCapabilityClient.class));
    }

    @Test
    void shouldDeriveTenantAndActorFromAuthenticatedCredentialRecord() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientSecretCodec codec = mock(CapabilityClientSecretCodec.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        CapabilityClientService service = new CapabilityClientService(mapper, codec, clock);
        AiCapabilityClient client = enabledClient();
        when(codec.extractKeyId("raw-secret")).thenReturn("key-id-12345678901234");
        when(mapper.selectCredentialByKeyId("key-id-12345678901234")).thenReturn(client);
        when(codec.matches("raw-secret", client.getKeyHash())).thenReturn(true);
        when(mapper.touchLastUsed(
                7L, 99L, 3, client.getKeyHash(), LocalDateTime.of(2026, 7, 11, 0, 0)))
                .thenReturn(1);

        com.mdframe.forge.plugin.capability.controlplane.security.CapabilityClientPrincipal principal =
                service.authenticate("raw-secret");

        assertThat(principal.tenantId()).isEqualTo(7L);
        assertThat(principal.serviceUserId()).isEqualTo(10L);
        assertThat(principal.activeOrgId()).isEqualTo(20L);
    }

    @Test
    void shouldFailAuthenticationWhenCredentialChangesBeforeLastUsedTouch() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientSecretCodec codec = mock(CapabilityClientSecretCodec.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC);
        CapabilityClientService service = new CapabilityClientService(mapper, codec, clock);
        AiCapabilityClient client = enabledClient();
        when(codec.extractKeyId("raw-secret")).thenReturn("key-id-12345678901234");
        when(mapper.selectCredentialByKeyId("key-id-12345678901234")).thenReturn(client);
        when(codec.matches("raw-secret", client.getKeyHash())).thenReturn(true);
        when(mapper.touchLastUsed(
                7L, 99L, 3, client.getKeyHash(), LocalDateTime.of(2026, 7, 11, 0, 0)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.authenticate("raw-secret"))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class)
                .hasMessageContaining("凭据无效");
        verify(mapper, org.mockito.Mockito.never())
                .updateById(org.mockito.ArgumentMatchers.any(AiCapabilityClient.class));
    }

    @Test
    void shouldRotateCredentialWithCompareAndSet() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientSecretCodec codec = mock(CapabilityClientSecretCodec.class);
        CapabilityClientService service = new CapabilityClientService(mapper, codec, Clock.systemUTC());
        AiCapabilityClient client = enabledClient();
        String rotatedSecret = "fcp_new-key-12345678901234_rotated-secret";
        when(mapper.selectTenantById(7L, 99L)).thenReturn(client);
        when(codec.issue("client_a")).thenReturn(new IssuedClientSecret(
                rotatedSecret, "new-key-12345678901234", "fcp_new-key-12345678901234", "b".repeat(64)));
        when(mapper.rotateCredential(
                7L, 99L, 3, "new-key-12345678901234",
                "fcp_new-key-12345678901234", "b".repeat(64))).thenReturn(1);

        CapabilityClientSecretVO response = service.rotate(7L, 99L);

        assertThat(response.clientSecret()).isEqualTo(rotatedSecret);
        assertThat(response.credentialVersion()).isEqualTo(4);
        verify(mapper, org.mockito.Mockito.never())
                .updateById(org.mockito.ArgumentMatchers.any(AiCapabilityClient.class));
    }

    @Test
    void shouldRevokeCredentialWithCompareAndSet() {
        AiCapabilityClientMapper mapper = mock(AiCapabilityClientMapper.class);
        CapabilityClientService service = new CapabilityClientService(
                mapper, mock(CapabilityClientSecretCodec.class), Clock.systemUTC());
        AiCapabilityClient client = enabledClient();
        when(mapper.selectTenantById(7L, 99L)).thenReturn(client);
        when(mapper.revokeCredential(7L, 99L, 3)).thenReturn(1);

        service.revoke(7L, 99L);

        verify(mapper).revokeCredential(7L, 99L, 3);
        verify(mapper, org.mockito.Mockito.never())
                .updateById(org.mockito.ArgumentMatchers.any(AiCapabilityClient.class));
    }

    private AiCapabilityClient enabledClient() {
        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(99L);
        client.setTenantId(7L);
        client.setClientCode("client_a");
        client.setKeyId("key-id-12345678901234");
        client.setKeyHash("a".repeat(64));
        client.setCredentialVersion(3);
        client.setStatus("ENABLED");
        client.setServiceUserId(10L);
        client.setActiveOrgId(20L);
        return client;
    }
}
