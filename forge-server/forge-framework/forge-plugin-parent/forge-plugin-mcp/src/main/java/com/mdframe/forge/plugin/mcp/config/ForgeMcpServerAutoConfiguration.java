package com.mdframe.forge.plugin.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.config.CapabilityAutoConfiguration;
import com.mdframe.forge.plugin.capability.registry.CapabilityRegistry;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.mcp.adapter.McpCapabilityAdapter;
import com.mdframe.forge.plugin.mcp.adapter.McpCapabilityResultMapper;
import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import com.mdframe.forge.plugin.mcp.security.ForgeMcpAuthenticationFilter;
import com.mdframe.forge.plugin.mcp.security.ForgeMcpTransportContextExtractor;
import com.mdframe.forge.plugin.mcp.security.McpCallerContextResolver;
import com.mdframe.forge.plugin.mcp.security.McpRequestLifecycle;
import com.mdframe.forge.plugin.mcp.spi.McpToolContributor;
import com.mdframe.forge.plugin.mcp.spi.McpToolContributorAggregator;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.mcp.server.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import java.util.List;

@AutoConfiguration(
        after = CapabilityAutoConfiguration.class,
        before = McpServerStreamableHttpWebMvcAutoConfiguration.class)
@EnableConfigurationProperties({McpServerProperties.class, McpServerStreamableHttpProperties.class})
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.server",
        name = "enabled",
        havingValue = "true")
public class ForgeMcpServerAutoConfiguration {

    @Bean
    public ForgeMcpProtocolGuard forgeMcpProtocolGuard(McpServerProperties properties) {
        return new ForgeMcpProtocolGuard(properties);
    }

    @Bean
    public FilterRegistrationBean<ForgeMcpAuthenticationFilter> forgeMcpAuthenticationFilter(
            McpServerStreamableHttpProperties properties,
            McpCallerContextResolver callerContextResolver,
            McpRequestLifecycle requestLifecycle,
            ForgeMcpProtocolGuard protocolGuard) {
        ForgeMcpAuthenticationFilter filter = new ForgeMcpAuthenticationFilter(
                properties.getMcpEndpoint(), callerContextResolver, requestLifecycle);
        FilterRegistrationBean<ForgeMcpAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("forgeMcpAuthenticationFilter");
        registration.addUrlPatterns(properties.getMcpEndpoint());
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public McpRequestLifecycle mcpRequestLifecycle() {
        return McpRequestLifecycle.noop();
    }

    @Bean
    @ConditionalOnMissingBean(WebMvcStreamableServerTransportProvider.class)
    public WebMvcStreamableServerTransportProvider forgeWebMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerObjectMapper") ObjectMapper objectMapper,
            McpServerStreamableHttpProperties properties,
            McpCallerContextResolver callerContextResolver,
            ForgeMcpProtocolGuard protocolGuard) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint(properties.getMcpEndpoint())
                .keepAliveInterval(properties.getKeepAliveInterval())
                .disallowDelete(properties.isDisallowDelete())
                .contextExtractor(new ForgeMcpTransportContextExtractor(callerContextResolver))
                .build();
    }

    @Bean
    public McpToolSchemaProjector mcpToolSchemaProjector(ObjectMapper objectMapper) {
        return new McpToolSchemaProjector(objectMapper);
    }

    @Bean
    public McpCapabilityResultMapper mcpCapabilityResultMapper(ObjectMapper objectMapper) {
        return new McpCapabilityResultMapper(objectMapper);
    }

    @Bean
    public McpCapabilityAdapter mcpCapabilityAdapter(
            CapabilityRegistry capabilityRegistry,
            ObjectMapper objectMapper,
            McpCapabilityResultMapper resultMapper,
            CapabilitySchemaValidator schemaValidator) {
        return new McpCapabilityAdapter(
                capabilityRegistry, objectMapper, resultMapper, schemaValidator);
    }

    @Bean
    public McpToolContributor forgePingMcpToolContributor(
            McpCapabilityAdapter adapter,
            McpToolSchemaProjector schemaProjector) {
        return ignored -> List.of(adapter.pingToolSpecification(schemaProjector));
    }

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> forgeMcpToolSpecifications(
            List<McpToolContributor> contributors,
            McpToolSchemaProjector schemaProjector) {
        return McpToolContributorAggregator.aggregate(contributors, schemaProjector);
    }
}
