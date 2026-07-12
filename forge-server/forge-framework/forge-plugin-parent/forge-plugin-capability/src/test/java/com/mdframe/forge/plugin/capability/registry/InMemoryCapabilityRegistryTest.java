package com.mdframe.forge.plugin.capability.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import com.mdframe.forge.plugin.capability.model.CapabilityAuthorizationDecision;
import com.mdframe.forge.plugin.capability.model.CapabilityBehavior;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityCursor;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityPage;
import com.mdframe.forge.plugin.capability.model.CapabilityQuery;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.model.CapabilityRiskLevel;
import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;
import com.mdframe.forge.plugin.capability.naming.CapabilityToolNameMapper;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.spi.CapabilityAuthorizationPolicy;
import com.mdframe.forge.plugin.capability.spi.CapabilityExecutor;
import com.mdframe.forge.plugin.capability.spi.CapabilitySource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCapabilityRegistryTest {

    private static final String DISCOVER_SCOPE = "capability:discover";
    private static final String INVOKE_SCOPE = "capability:invoke";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CapabilitySchemaValidator schemaValidator = new CapabilitySchemaValidator();
    private final CapabilityToolNameMapper nameMapper = new CapabilityToolNameMapper();

    @Test
    void shouldRejectDuplicateCodeAndVersion() {
        CapabilityDefinition ping = definition("capability.ping");

        assertThatThrownBy(() -> registry(List.of(query -> List.of(ping), query -> List.of(ping))))
                .isInstanceOf(CapabilityDefinitionException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void shouldFilterAndPageByCallerWithStableCursor() {
        CapabilitySource source = query -> List.of(
                definition("capability.beta"),
                definition("capability.alpha"));
        InMemoryCapabilityRegistry registry = registry(List.of(source));
        CapabilityCallerContext caller = caller(Set.of(DISCOVER_SCOPE, INVOKE_SCOPE));

        CapabilityPage first = registry.list(new CapabilityQuery(null, 1, null), caller);
        CapabilityPage second = registry.list(new CapabilityQuery(null, 1, first.nextCursor()), caller);

        assertThat(first.items()).extracting(CapabilityDefinition::capabilityCode)
                .containsExactly("capability.alpha");
        assertThat(second.items()).extracting(CapabilityDefinition::capabilityCode)
                .containsExactly("capability.beta");
        assertThat(second.nextCursor()).isNull();
        assertThat(registry.list(new CapabilityQuery(null, 10, null), caller).snapshotVersion())
                .isEqualTo(first.snapshotVersion());
    }

    @Test
    void shouldValidateInputAndAuthorizeAgainWhenInvoking() throws Exception {
        InMemoryCapabilityRegistry registry = registry(List.of(query -> List.of(definition("capability.ping"))));
        CapabilityCallerContext allowed = caller(Set.of(DISCOVER_SCOPE, INVOKE_SCOPE));
        CapabilityInvocation invocation = new CapabilityInvocation(
                "request-1", "capability.ping", "1.0.0", allowed, objectMapper.readTree("{}"));

        assertThat(registry.invoke(invocation).status()).isEqualTo(CapabilityResultStatus.SUCCESS);

        CapabilityCallerContext discoverOnly = caller(Set.of(DISCOVER_SCOPE));
        CapabilityInvocation denied = new CapabilityInvocation(
                "request-2", "capability.ping", "1.0.0", discoverOnly, objectMapper.readTree("{}"));
        assertThat(registry.invoke(denied).errorCode()).isEqualTo("FORBIDDEN");

        CapabilityInvocation invalid = new CapabilityInvocation(
                "request-3", "capability.ping", "1.0.0", allowed,
                objectMapper.readTree("{\"tenantId\":2}"));
        assertThat(registry.invoke(invalid).errorCode()).isEqualTo("INVALID_ARGUMENT");
    }

    @Test
    void shouldRejectCursorFromAnotherSnapshot() {
        CapabilityCallerContext caller = caller(Set.of(DISCOVER_SCOPE, INVOKE_SCOPE));
        InMemoryCapabilityRegistry firstRegistry = registry(List.of(query -> List.of(
                definition("capability.alpha"), definition("capability.beta"))));
        String cursor = firstRegistry.list(new CapabilityQuery(null, 1, null), caller).nextCursor();
        InMemoryCapabilityRegistry secondRegistry = registry(List.of(query -> List.of(
                definition("capability.alpha"), definition("capability.gamma"))));

        assertThatThrownBy(() -> secondRegistry.list(new CapabilityQuery(null, 1, cursor), caller))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("游标");
    }

    @Test
    void shouldRejectForgedCursorAndCursorReusedAcrossQueries() {
        CapabilityCallerContext caller = caller(Set.of(DISCOVER_SCOPE, INVOKE_SCOPE));
        InMemoryCapabilityRegistry registry = registry(List.of(query -> List.of(
                definition("capability.alpha"), definition("capability.beta"))));
        CapabilityPage first = registry.list(new CapabilityQuery(null, 1, null), caller);
        CapabilityCursor valid = CapabilityCursor.decode(first.nextCursor());
        String forged = new CapabilityCursor(
                valid.snapshotVersion(),
                valid.queryFingerprint(),
                "capability.beta\u00001.0.0",
                valid.signature()).encode();

        assertThatThrownBy(() -> registry.list(new CapabilityQuery(null, 1, forged), caller))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("游标");
        assertThatThrownBy(() -> registry.list(
                new CapabilityQuery("capability.beta", 1, first.nextCursor()), caller))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("查询");
    }

    @Test
    void shouldPreserveTenantAndOrganizationDenialCodes() throws Exception {
        CapabilityAuthorizationPolicy policy = new CapabilityAuthorizationPolicy() {
            @Override
            public CapabilityAuthorizationDecision evaluateDiscovery(
                    CapabilityDefinition definition,
                    CapabilityCallerContext caller) {
                return evaluateInvocation(definition, caller);
            }

            @Override
            public CapabilityAuthorizationDecision evaluateInvocation(
                    CapabilityDefinition definition,
                    CapabilityCallerContext caller) {
                if (caller.tenantId() != 1L) {
                    return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.TENANT_SCOPE_VIOLATION);
                }
                if (caller.activeOrgId() == null || caller.activeOrgId() != 100L) {
                    return CapabilityAuthorizationDecision.deny(CapabilityErrorCode.ORG_SCOPE_VIOLATION);
                }
                return CapabilityAuthorizationDecision.allow();
            }
        };
        InMemoryCapabilityRegistry registry = registry(
                List.of(query -> List.of(definition("capability.ping"))), policy);

        CapabilityInvocation crossTenant = new CapabilityInvocation(
                "request-tenant", "capability.ping", "1.0.0",
                new CapabilityCallerContext("test-client", 2L, 10L, 100L, Set.of(INVOKE_SCOPE)),
                objectMapper.readTree("{}"));
        CapabilityInvocation crossOrg = new CapabilityInvocation(
                "request-org", "capability.ping", "1.0.0",
                new CapabilityCallerContext("test-client", 1L, 10L, 101L, Set.of(INVOKE_SCOPE)),
                objectMapper.readTree("{}"));

        assertThat(registry.invoke(crossTenant).errorCode()).isEqualTo("TENANT_SCOPE_VIOLATION");
        assertThat(registry.invoke(crossOrg).errorCode()).isEqualTo("ORG_SCOPE_VIOLATION");
    }

    private InMemoryCapabilityRegistry registry(List<CapabilitySource> sources) {
        CapabilityAuthorizationPolicy policy = new CapabilityAuthorizationPolicy() {
            @Override
            public CapabilityAuthorizationDecision evaluateDiscovery(
                    CapabilityDefinition definition,
                    CapabilityCallerContext caller) {
                return caller != null && caller.scopes().contains(DISCOVER_SCOPE)
                        ? CapabilityAuthorizationDecision.allow()
                        : CapabilityAuthorizationDecision.deny(CapabilityErrorCode.FORBIDDEN);
            }

            @Override
            public CapabilityAuthorizationDecision evaluateInvocation(
                    CapabilityDefinition definition,
                    CapabilityCallerContext caller) {
                return caller != null && caller.scopes().contains(INVOKE_SCOPE)
                        ? CapabilityAuthorizationDecision.allow()
                        : CapabilityAuthorizationDecision.deny(CapabilityErrorCode.FORBIDDEN);
            }
        };
        return registry(sources, policy);
    }

    private InMemoryCapabilityRegistry registry(
            List<CapabilitySource> sources,
            CapabilityAuthorizationPolicy policy) {
        CapabilityExecutor executor = new CapabilityExecutor() {
            @Override
            public boolean supports(CapabilityDefinition definition) {
                return definition.capabilityCode().startsWith("capability.");
            }

            @Override
            public CapabilityResult invoke(CapabilityDefinition definition, CapabilityInvocation invocation) {
                return CapabilityResult.success(invocation.requestId(), definition.capabilityCode(),
                        objectMapper.createObjectNode().put("status", "ok"), 1L);
            }
        };
        return new InMemoryCapabilityRegistry(
                sources, List.of(executor), policy, nameMapper, schemaValidator);
    }

    private CapabilityDefinition definition(String code) {
        JsonNode input = objectMapper.createObjectNode()
                .put("$schema", "https://json-schema.org/draft/2020-12/schema")
                .put("type", "object")
                .put("additionalProperties", false);
        JsonNode output = objectMapper.createObjectNode()
                .put("$schema", "https://json-schema.org/draft/2020-12/schema")
                .put("type", "object");
        return new CapabilityDefinition(code, code, "1.0.0", CapabilityBehavior.READ_ONLY,
                CapabilityRiskLevel.LOW, "测试能力", input, output);
    }

    private CapabilityCallerContext caller(Set<String> scopes) {
        return new CapabilityCallerContext("test-client", 1L, 10L, 100L, scopes);
    }
}
