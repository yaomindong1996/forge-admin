package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityGrantCreateDTO;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityGrantMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import com.mdframe.forge.plugin.capability.controlplane.security.CapabilityGrantPolicy;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapabilityGrantServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRevokeGrantByLogicalDeleteSoItCanBeGrantedAgain() {
        AiCapabilityGrantMapper grantMapper = mock(AiCapabilityGrantMapper.class);
        CapabilityGrantService service = new CapabilityGrantService(
                grantMapper,
                mock(AiCapabilityVersionMapper.class),
                mock(CapabilityCatalogService.class),
                mock(CapabilityClientService.class),
                new CapabilityGrantPolicy(),
                new ObjectMapper(),
                Clock.systemUTC());
        when(grantMapper.logicallyRevoke(1L, 20L)).thenReturn(1);

        service.revoke(1L, 20L);

        verify(grantMapper).logicallyRevoke(1L, 20L);
        verify(grantMapper, never()).deleteById(20L);
    }

    @Test
    void shouldRejectActionGrantWithoutFieldPolicy() {
        Fixture fixture = fixture("MEDIUM");

        assertThatThrownBy(() -> fixture.service.grant(1L, dto(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("受控中风险业务动作");
        verify(fixture.grantMapper, never()).insert(any(AiCapabilityGrant.class));
    }

    @Test
    void shouldRejectGrantFieldsOutsideVersionPolicy() throws Exception {
        Fixture fixture = fixture("MEDIUM");

        assertThatThrownBy(() -> fixture.service.grant(1L,
                dto(objectMapper.readTree("{\"allowedFields\":[\"secret\"]}"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超出");
        verify(fixture.grantMapper, never()).insert(any(AiCapabilityGrant.class));
    }

    @Test
    void shouldAllowControlledMediumActionGrant() throws Exception {
        Fixture fixture = fixture("MEDIUM");

        assertThatCode(() -> fixture.service.grant(1L,
                dto(objectMapper.readTree("{\"allowedFields\":[\"status\"]}"))))
                .doesNotThrowAnyException();

        ArgumentCaptor<AiCapabilityGrant> captor = ArgumentCaptor.forClass(AiCapabilityGrant.class);
        verify(fixture.grantMapper).insert(captor.capture());
        assertThat(captor.getValue().getFieldPolicy()).contains("status");
        assertThat(captor.getValue().getStatus()).isEqualTo("ENABLED");
    }

    @Test
    void shouldRejectHighRiskActionGrant() throws Exception {
        Fixture fixture = fixture("HIGH");

        assertThatThrownBy(() -> fixture.service.grant(1L,
                dto(objectMapper.readTree("{\"allowedFields\":[\"status\"]}"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("高风险");
        verify(fixture.grantMapper, never()).insert(any(AiCapabilityGrant.class));
    }

    @Test
    void shouldAllowFlowGrantOnlyForVersionOperation() throws Exception {
        Fixture fixture = flowFixture();

        assertThatCode(() -> fixture.service.grant(1L,
                dto(objectMapper.readTree("{\"allowedOperations\":[\"APPROVE\"]}"))))
                .doesNotThrowAnyException();
        verify(fixture.grantMapper).insert(any(AiCapabilityGrant.class));
    }

    @Test
    void shouldRejectFlowGrantOutsideVersionOperation() throws Exception {
        Fixture fixture = flowFixture();

        assertThatThrownBy(() -> fixture.service.grant(1L,
                dto(objectMapper.readTree("{\"allowedOperations\":[\"REJECT\"]}"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超出");
    }

    private Fixture fixture(String riskLevel) {
        AiCapabilityGrantMapper grantMapper = mock(AiCapabilityGrantMapper.class);
        AiCapabilityVersionMapper versionMapper = mock(AiCapabilityVersionMapper.class);
        CapabilityCatalogService catalogService = mock(CapabilityCatalogService.class);
        CapabilityClientService clientService = mock(CapabilityClientService.class);
        CapabilityGrantService service = new CapabilityGrantService(
                grantMapper, versionMapper, catalogService, clientService,
                new CapabilityGrantPolicy(), objectMapper, Clock.systemUTC());
        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(10L);
        client.setTenantId(1L);
        AiCapability capability = new AiCapability();
        capability.setId(20L);
        capability.setTenantId(1L);
        capability.setSourceType("BUSINESS_ACTION");
        capability.setBehavior("ACTION");
        capability.setRiskLevel(riskLevel);
        AiCapabilityVersion version = new AiCapabilityVersion();
        version.setPolicySnapshot("{\"allowedFields\":[\"status\",\"remark\"],"
                + "\"confirmationMode\":\"MCP_ELICITATION\"}");
        when(clientService.requireClient(1L, 10L)).thenReturn(client);
        when(catalogService.getById(1L, 20L)).thenReturn(capability);
        when(versionMapper.selectVersion(1L, 20L, "1.0.0")).thenReturn(version);
        return new Fixture(service, grantMapper);
    }

    private CapabilityGrantCreateDTO dto(JsonNode fieldPolicy) {
        return new CapabilityGrantCreateDTO(
                10L, 20L, "PINNED", "1.0.0", fieldPolicy, null);
    }

    private Fixture flowFixture() {
        AiCapabilityGrantMapper grantMapper = mock(AiCapabilityGrantMapper.class);
        AiCapabilityVersionMapper versionMapper = mock(AiCapabilityVersionMapper.class);
        CapabilityCatalogService catalogService = mock(CapabilityCatalogService.class);
        CapabilityClientService clientService = mock(CapabilityClientService.class);
        CapabilityGrantService service = new CapabilityGrantService(
                grantMapper, versionMapper, catalogService, clientService,
                new CapabilityGrantPolicy(), objectMapper, Clock.systemUTC());
        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(10L);
        client.setTenantId(1L);
        AiCapability capability = new AiCapability();
        capability.setId(20L);
        capability.setTenantId(1L);
        capability.setSourceType("FLOW_ACTION");
        capability.setBehavior("FLOW");
        capability.setRiskLevel("MEDIUM");
        AiCapabilityVersion version = new AiCapabilityVersion();
        version.setPolicySnapshot("{\"allowedOperations\":[\"APPROVE\"],"
                + "\"confirmationMode\":\"MCP_ELICITATION\"}");
        when(clientService.requireClient(1L, 10L)).thenReturn(client);
        when(catalogService.getById(1L, 20L)).thenReturn(capability);
        when(versionMapper.selectVersion(1L, 20L, "1.0.0")).thenReturn(version);
        return new Fixture(service, grantMapper);
    }

    private record Fixture(
            CapabilityGrantService service,
            AiCapabilityGrantMapper grantMapper) {
    }
}
