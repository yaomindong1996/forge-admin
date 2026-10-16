package com.mdframe.forge.plugin.capability.secureaction.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.execution.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.starter.core.annotation.api.OpenRestCapability;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestEndpointSystemServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
    private final RestEndpointSystemService service;

    @SuppressWarnings("unchecked")
    RestEndpointSystemServiceTest() throws Exception {
        ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenAnswer(ignored -> Stream.of(mapping));
        service = new RestEndpointSystemService(provider, mapper, Validation.buildDefaultValidatorFactory().getValidator());
        TestController controller = new TestController();
        mapping.registerMapping(RequestMappingInfo.paths("/test/{id}").methods(RequestMethod.POST).build(), controller,
                TestController.class.getMethod("create", Long.class, Input.class));
        mapping.registerMapping(RequestMappingInfo.paths("/hidden").methods(RequestMethod.GET).build(), controller,
                TestController.class.getMethod("hidden"));
    }

    @Test
    void onlyOptedInMethodsAreDiscoverableAndLongIdsRemainStrings() {
        var sources = service.registrationSource(1L).options().path("endpoints");
        assertThat(sources.size()).isEqualTo(1);
        var schema = sources.get(0).path("inputSchema");
        new CapabilitySchemaValidator().validateDefinition(schema);
        assertThat(schema.path("properties").path("id").path("type").asText()).isEqualTo("string");
        assertThat(schema.path("properties").path("body").path("additionalProperties").asBoolean()).isFalse();
    }

    @Test
    void executesWithTrustedIdentityAndGatewayInjectedIdempotencyKey() {
        var descriptor = descriptor();
        try (var ignored = ExecutionIdentityContextHolder.open(identity(Set.of("test:write")))) {
            var result = service.execute(descriptor, Map.of("id", "9007199254740999", "body", Map.of("title", "example"), "idempotencyKey", "request-1"), "request-1");
            assertThat(((Map<?, ?>) result.get("result")).get("code")).isEqualTo(200);
        }
    }

    @Test
    void executesWithServiceActorIdentity() {
        var descriptor = descriptor();
        LoginUser user = new LoginUser();
        user.setUserId(7L);
        user.setTenantId(1L);
        user.setActiveOrgId(2L);
        user.setPermissions(Set.of("test:write"));
        var serviceIdentity = new ExecutionIdentity(user, "SERVICE", 7L, 7L, 3L, "svc", "svc-token", Set.of());
        try (var ignored = ExecutionIdentityContextHolder.open(serviceIdentity)) {
            var result = service.execute(descriptor,
                    Map.of("id", "42", "body", Map.of("title", "svc")), "request-svc");
            assertThat(((Map<?, ?>) result.get("result")).get("code")).isEqualTo(200);
        }
    }

    @Test
    void refusesMissingIdentityBusinessPermissionAndNestedUnknownFields() {
        var descriptor = descriptor();
        var valid = Map.<String, Object>of("id", "1", "body", Map.of("title", "example"));
        assertThatThrownBy(() -> service.validate(descriptor, valid)).isInstanceOf(BusinessException.class);
        try (var ignored = ExecutionIdentityContextHolder.open(identity(Set.of()))) {
            assertThatThrownBy(() -> service.validate(descriptor, valid)).hasMessageContaining("无权");
        }
        try (var ignored = ExecutionIdentityContextHolder.open(identity(Set.of("test:write")))) {
            assertThatThrownBy(() -> service.validate(descriptor, Map.of("id", "1", "body", Map.of("title", "ok", "tenantId", "2")))).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> service.validate(descriptor, Map.of("id", "1", "body", Map.of("title", "")))).hasMessageContaining("校验");
        }
    }

    @Test
    void rejectsArbitraryUrlsAndBodyIdempotencyOverride() {
        assertThatThrownBy(() -> service.preparePublication(1L, mapper.valueToTree(Map.of("url", "http://localhost/internal")))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.prepareInput(Map.of("idempotencyKey", "forged"))).isInstanceOf(BusinessException.class);
    }

    private SecureActionDescriptor descriptor() {
        String id = service.registrationSource(1L).options().path("endpoints").get(0).path("id").asText();
        var publication = service.preparePublication(1L, mapper.valueToTree(Map.of("endpointId", id)));
        new CapabilitySchemaValidator().validateDefinition(publication.outputSchema());
        return new SecureActionDescriptor(1L, "rest.test", "测试接口", "", "1.0.0", "SYSTEM_SERVICE", service.serviceCode(), "1", "ACTION", "MEDIUM",
                "system", service.serviceCode(), service.serviceCode(), null, "test:write", Set.of(), Set.of(),
                publication.policySnapshot(), publication.inputSchema(), publication.outputSchema());
    }

    private ExecutionIdentity identity(Set<String> permissions) {
        LoginUser user = new LoginUser(); user.setUserId(7L); user.setTenantId(1L); user.setActiveOrgId(2L); user.setPermissions(permissions);
        return new ExecutionIdentity(user, "USER", 7L, null, 3L, "test", "test-token", Set.of());
    }

    public record Input(@NotBlank String title) { }
    public static class TestController {
        @OpenRestCapability(name = "测试创建", permission = "test:write")
        public RespInfo<String> create(@PathVariable("id") Long id, @RequestBody Input body) { return RespInfo.success(id + body.title()); }
        public RespInfo<String> hidden() { return RespInfo.success("not exposed"); }
    }
}
