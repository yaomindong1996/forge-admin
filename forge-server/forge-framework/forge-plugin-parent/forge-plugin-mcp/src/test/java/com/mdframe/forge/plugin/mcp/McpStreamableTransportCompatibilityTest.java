package com.mdframe.forge.plugin.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.config.CapabilityAutoConfiguration;
import com.mdframe.forge.plugin.capability.model.CapabilityBehavior;
import com.mdframe.forge.plugin.capability.model.CapabilityCallerContext;
import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;
import com.mdframe.forge.plugin.capability.model.CapabilityInvocation;
import com.mdframe.forge.plugin.capability.model.CapabilityQuery;
import com.mdframe.forge.plugin.capability.model.CapabilityResult;
import com.mdframe.forge.plugin.capability.model.CapabilityRiskLevel;
import com.mdframe.forge.plugin.capability.registry.CapabilityRegistry;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.spi.CapabilityExecutor;
import com.mdframe.forge.plugin.capability.spi.CapabilitySource;
import com.mdframe.forge.plugin.mcp.config.ForgeMcpServerAutoConfiguration;
import com.mdframe.forge.plugin.mcp.security.McpCallerContextResolver;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerObjectMapperAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpStreamableTransportCompatibilityTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    McpServerObjectMapperAutoConfiguration.class,
                    McpServerSseWebMvcAutoConfiguration.class,
                    McpServerStreamableHttpWebMvcAutoConfiguration.class,
                    McpServerAutoConfiguration.class))
            .withPropertyValues(
                    "spring.ai.mcp.server.enabled=true",
                    "spring.ai.mcp.server.name=forge-ai-hub",
                    "spring.ai.mcp.server.version=1.0.0",
                    "spring.ai.mcp.server.type=SYNC",
                    "spring.ai.mcp.server.stdio=false",
                    "spring.ai.mcp.server.protocol=STREAMABLE",
                    "spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp");

    @Test
    void shouldRemainDisabledWhenAdminDefaultIsApplied() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        CapabilityAutoConfiguration.class,
                        McpServerObjectMapperAutoConfiguration.class,
                        ForgeMcpServerAutoConfiguration.class,
                        McpServerSseWebMvcAutoConfiguration.class,
                        McpServerStreamableHttpWebMvcAutoConfiguration.class,
                        McpServerAutoConfiguration.class))
                .withPropertyValues("spring.ai.mcp.server.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WebMvcStreamableServerTransportProvider.class);
                    assertThat(context).doesNotHaveBean(WebMvcSseServerTransportProvider.class);
                });
    }

    @Test
    void shouldCreateOnlyWebMvcStreamableTransport() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebMvcStreamableServerTransportProvider.class);
            assertThat(context).doesNotHaveBean(WebMvcSseServerTransportProvider.class);
            assertThat(context).hasSingleBean(McpServerStreamableHttpWebMvcAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(McpServerSseWebMvcAutoConfiguration.class);
        });
    }

    @Test
    void shouldFailClosedWhenEnabledWithoutTrustedCallerResolver() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        CapabilityAutoConfiguration.class,
                        McpServerObjectMapperAutoConfiguration.class,
                        ForgeMcpServerAutoConfiguration.class,
                        McpServerSseWebMvcAutoConfiguration.class,
                        McpServerStreamableHttpWebMvcAutoConfiguration.class,
                        McpServerAutoConfiguration.class))
                .withPropertyValues(
                        "spring.ai.mcp.server.enabled=true",
                        "spring.ai.mcp.server.protocol=STREAMABLE",
                        "spring.ai.mcp.server.stdio=false")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("McpCallerContextResolver");
                });
    }

    @Test
    void shouldFailClosedWhenProtocolIsOverriddenToLegacySseOrStateless() {
        assertRejectedProtocol("SSE");
        assertRejectedProtocol("STATELESS");
    }

    @Test
    void shouldFailClosedWhenStdioIsEnabled() {
        forgeContextRunner("STREAMABLE")
                .withPropertyValues("spring.ai.mcp.server.stdio=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("stdio");
                });
    }

    @Test
    void shouldFailClosedWhenServerTypeIsAsync() {
        forgeContextRunner("STREAMABLE")
                .withPropertyValues("spring.ai.mcp.server.type=ASYNC")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("SYNC");
                });
    }

    @Test
    void shouldPublishOnlyPingWhenRegistryContainsAnotherCapability() {
        CapabilityCallerContext caller = new CapabilityCallerContext(
                "test-client", 1L, null, null,
                Set.of("capability:discover", "capability:invoke"));
        forgeContextRunner("STREAMABLE")
                .withBean("extraCapabilitySource", CapabilitySource.class,
                        () -> query -> List.of(extraDefinition()))
                .withBean("extraCapabilityExecutor", CapabilityExecutor.class,
                        this::extraExecutor)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CapabilityRegistry.class)
                            .list(new CapabilityQuery(null, 10, null), caller).items()).hasSize(2);
                    List<?> specifications = context.getBean(
                            "forgeMcpToolSpecifications", List.class);
                    assertThat(specifications).hasSize(1);
                    McpServerFeatures.SyncToolSpecification specification =
                            (McpServerFeatures.SyncToolSpecification) specifications.get(0);
                    assertThat(specification.tool().name()).isEqualTo("capability.ping");
                });
    }

    private void assertRejectedProtocol(String protocol) {
        forgeContextRunner(protocol).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("只允许 STREAMABLE");
        });
    }

    private WebApplicationContextRunner forgeContextRunner(String protocol) {
        return new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        CapabilityAutoConfiguration.class,
                        McpServerObjectMapperAutoConfiguration.class,
                        ForgeMcpServerAutoConfiguration.class,
                        McpServerSseWebMvcAutoConfiguration.class,
                        McpServerStreamableHttpWebMvcAutoConfiguration.class,
                        McpServerAutoConfiguration.class))
                .withBean(McpCallerContextResolver.class, () -> request ->
                        new CapabilityCallerContext(
                                "test-client", 1L, null, null,
                                Set.of("capability:discover", "capability:invoke")))
                .withPropertyValues(
                        "spring.ai.mcp.server.enabled=true",
                        "spring.ai.mcp.server.type=SYNC",
                        "spring.ai.mcp.server.protocol=" + protocol,
                        "spring.ai.mcp.server.stdio=false");
    }

    private CapabilityDefinition extraDefinition() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new CapabilityDefinition(
                "capability.extra",
                "capability.extra",
                "1.0.0",
                CapabilityBehavior.READ_ONLY,
                CapabilityRiskLevel.LOW,
                "用于验证 MCP 静态发布闸门",
                objectMapper.createObjectNode()
                        .put("$schema", CapabilitySchemaValidator.DRAFT_2020_12)
                        .put("type", "object")
                        .put("additionalProperties", false),
                objectMapper.createObjectNode()
                        .put("$schema", CapabilitySchemaValidator.DRAFT_2020_12)
                        .put("type", "object"));
    }

    private CapabilityExecutor extraExecutor() {
        return new CapabilityExecutor() {
            @Override
            public boolean supports(CapabilityDefinition definition) {
                return "capability.extra".equals(definition.capabilityCode());
            }

            @Override
            public CapabilityResult invoke(
                    CapabilityDefinition definition,
                    CapabilityInvocation invocation) {
                return CapabilityResult.success(
                        invocation.requestId(), definition.capabilityCode(),
                        new ObjectMapper().createObjectNode(), 0L);
            }
        };
    }
}
