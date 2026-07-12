package com.mdframe.forge.plugin.capability.secureaction.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecureActionCatalogServiceTest {

    private final SecureActionCatalogMapper mapper = mock(SecureActionCatalogMapper.class);
    private final SecureActionCatalogService service = new SecureActionCatalogService(
            mapper, new ObjectMapper());

    @AfterEach
    void clearIdentity() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldIntersectVersionAndGrantFields() {
        openIdentity(Set.of("ai:capability:business-action:invoke", "purchase:order:confirm"));
        when(mapper.selectGrantedAction(1L, 301L, "business.order.confirm"))
                .thenReturn(row(10L, "business.order.confirm", "purchase:order:confirm",
                        "[\"status\",\"remark\"]", "[\"status\"]", 3));

        SecureActionDescriptor descriptor = service.requireAuthorized("business.order.confirm");

        assertThat(descriptor.allowedFields()).containsExactly("status");
        assertThat(descriptor.requiredFields()).containsExactly("status");
        List<String> inputFields = new ArrayList<>();
        descriptor.inputSchema().path("properties").path("arguments")
                .path("properties").fieldNames().forEachRemaining(inputFields::add);
        assertThat(inputFields).containsExactly("status");
    }

    @Test
    void shouldRequirePlatformAndActionPermissions() {
        openIdentity(Set.of("ai:capability:business-action:invoke"));
        when(mapper.selectGrantedAction(1L, 301L, "business.order.confirm"))
                .thenReturn(row(10L, "business.order.confirm", "purchase:order:confirm",
                        "[\"status\"]", "[\"status\"]", 3));

        assertThatThrownBy(() -> service.requireAuthorized("business.order.confirm"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权执行");
    }

    @Test
    void shouldContinueKeysetScanAfterUnauthorizedBatch() {
        openIdentity(Set.of("ai:capability:business-action:invoke", "purchase:order:confirm"));
        List<SecureActionCatalogRow> denied = new ArrayList<>();
        for (long index = 1; index <= 20; index++) {
            denied.add(row(index, "business.denied." + index, "purchase:order:denied",
                    "[\"status\"]", "[\"status\"]", 3));
        }
        when(mapper.selectGrantedActions(1L, 301L, null, null, null, 20))
                .thenReturn(denied);
        when(mapper.selectGrantedActions(1L, 301L, null,
                "business.denied.20", 20L, 20))
                .thenReturn(List.of(
                        row(21L, "business.order.confirm", "purchase:order:confirm",
                                "[\"status\"]", "[\"status\"]", 3),
                        row(22L, "business.order.confirm_more", "purchase:order:confirm",
                                "[\"status\"]", "[\"status\"]", 3)));

        SecureActionSearchResult result = service.search(null, 1);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).capabilityCode()).isEqualTo("business.order.confirm");
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    void shouldRejectMismatchedPublishedVersionBinding() {
        openIdentity(Set.of("ai:capability:business-action:invoke", "purchase:order:confirm"));
        when(mapper.selectGrantedAction(1L, 301L, "business.order.confirm"))
                .thenReturn(row(10L, "business.order.confirm", "purchase:order:confirm",
                        "[\"status\"]", "[\"status\"]", 4));

        assertThatThrownBy(() -> service.requireAuthorized("business.order.confirm"))
                .isInstanceOf(SecureActionUnavailableException.class)
                .extracting("errorCode")
                .isEqualTo("CATALOG_UNAVAILABLE");
    }

    @Test
    void shouldReturnAuthorizationUnavailableWhenGrantQueryFails() {
        openIdentity(Set.of("ai:capability:business-action:invoke", "purchase:order:confirm"));
        when(mapper.selectGrantedAction(1L, 301L, "business.order.confirm"))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> service.requireAuthorized("business.order.confirm"))
                .isInstanceOf(SecureActionUnavailableException.class)
                .extracting("errorCode")
                .isEqualTo("AUTHORIZATION_UNAVAILABLE");
    }

    @Test
    void shouldResolveSourceBindingFromGrantedVersionInMapperXml() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "mapper/SecureActionCatalogMapper.xml")) {
            assertThat(stream).isNotNull();
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xml).contains("v.source_key", "v.source_version");
            assertThat(xml).doesNotContain("c.source_key,", "c.source_version,");
        }
    }

    @Test
    void shouldResolveGrantedFlowOperationWithoutBusinessFieldPolicy() {
        openIdentity(Set.of("ai:capability:flow-action:invoke", "ai:businessFlow:view"));
        SecureActionCatalogRow row = new SecureActionCatalogRow();
        row.setCapabilityId(30L);
        row.setCapabilityCode("purchase.order.flow.approve");
        row.setCapabilityName("同意采购单");
        row.setDescription("受控流程动作");
        row.setSourceType("FLOW_ACTION");
        row.setBehavior("FLOW");
        row.setSourceKey("purchase/order/APPROVE");
        row.setSourceVersion("3");
        row.setVersion("1.0.0");
        row.setInputSchema(schema());
        row.setOutputSchema(schema());
        row.setPolicySnapshot("{\"allowedOperations\":[\"APPROVE\"],"
                + "\"operation\":\"APPROVE\",\"confirmationMode\":\"MCP_ELICITATION\","
                + "\"publishedObjectVersion\":3,\"permission\":\"ai:businessFlow:view\"}");
        row.setFieldPolicy("{\"allowedOperations\":[\"APPROVE\"]}");
        when(mapper.selectGrantedAction(1L, 301L, "purchase.order.flow.approve"))
                .thenReturn(row);

        SecureActionDescriptor descriptor = service.requireAuthorized("purchase.order.flow.approve");

        assertThat(descriptor.sourceType()).isEqualTo("FLOW_ACTION");
        assertThat(descriptor.behavior()).isEqualTo("FLOW");
        assertThat(descriptor.actionCode()).isEqualTo("APPROVE");
        assertThat(descriptor.allowedFields()).isEmpty();
    }

    private void openIdentity(Set<String> permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setPermissions(permissions);
        ExecutionIdentityContextHolder.open(new ExecutionIdentity(
                user, "USER", 101L, 999L, 301L,
                "agent_client", "token-1", Set.of("capability:discover")));
    }

    private SecureActionCatalogRow row(
            Long id,
            String code,
            String permission,
            String versionFields,
            String grantFields,
            int sourceVersion) {
        SecureActionCatalogRow row = new SecureActionCatalogRow();
        row.setCapabilityId(id);
        row.setCapabilityCode(code);
        row.setCapabilityName(code);
        row.setDescription("受控动作");
        row.setSourceKey("purchase/order/confirm");
        row.setSourceVersion(String.valueOf(sourceVersion));
        row.setVersion("1.0.0");
        row.setInputSchema(schema());
        row.setOutputSchema(schema());
        row.setPolicySnapshot("{\"allowedFields\":" + versionFields
                + ",\"requiredFields\":[\"status\"],\"confirmationMode\":\"MCP_ELICITATION\""
                + ",\"publishedObjectVersion\":3,\"permission\":\"" + permission + "\"}");
        row.setFieldPolicy("{\"allowedFields\":" + grantFields + "}");
        row.setRiskLevel("MEDIUM");
        return row;
    }

    private String schema() {
        return "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"additionalProperties\":false,\"properties\":{"
                + "\"idempotencyKey\":{\"type\":\"string\"},"
                + "\"arguments\":{\"type\":\"object\",\"additionalProperties\":false,"
                + "\"properties\":{\"status\":{\"type\":\"string\"},"
                + "\"remark\":{\"type\":\"string\"}},\"required\":[\"status\"]}},"
                + "\"required\":[\"idempotencyKey\",\"arguments\"]}";
    }
}
