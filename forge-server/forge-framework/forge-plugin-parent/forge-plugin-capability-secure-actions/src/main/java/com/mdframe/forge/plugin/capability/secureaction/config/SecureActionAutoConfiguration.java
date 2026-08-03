package com.mdframe.forge.plugin.capability.secureaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.publish.BusinessActionCapabilityPublisher;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.capability.secureaction.source.BusinessActionSourceService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.mcp.SecureActionMcpHandler;
import com.mdframe.forge.plugin.capability.secureaction.mcp.SecureActionMcpToolContributor;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
import com.mdframe.forge.plugin.capability.secureaction.system.SystemServiceCapabilityDefinition;
import com.mdframe.forge.plugin.capability.secureaction.system.SystemServiceDefinitionRegistry;
import com.mdframe.forge.plugin.capability.secureaction.system.SystemServiceOpenGatewayAdapter;
import com.mdframe.forge.plugin.capability.secureaction.system.SystemServiceCapabilityPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 已发布业务动作的 MCP 受控写入组合配置。
 */
@AutoConfiguration
public class SecureActionAutoConfiguration {

    @Bean
    public SystemServiceDefinitionRegistry systemServiceDefinitionRegistry(
            List<SystemServiceCapabilityDefinition> definitions) {
        return new SystemServiceDefinitionRegistry(definitions);
    }

    @Bean
    public SystemServiceOpenGatewayAdapter systemServiceOpenGatewayAdapter(
            SystemServiceDefinitionRegistry registry) {
        return new SystemServiceOpenGatewayAdapter(registry);
    }

    @Bean
    public SystemServiceCapabilityPublisher systemServiceCapabilityPublisher(
            SystemServiceDefinitionRegistry registry,
            CapabilityCatalogService catalogService,
            ObjectMapper objectMapper) {
        return new SystemServiceCapabilityPublisher(registry, catalogService, objectMapper);
    }

    @Bean
    public SecureActionStepValidator secureActionStepValidator() {
        return new SecureActionStepValidator();
    }

    @Bean
    public SecureActionPublishedModelPolicy secureActionPublishedModelPolicy(ObjectMapper objectMapper) {
        return new SecureActionPublishedModelPolicy(objectMapper);
    }

    @Bean
    public BusinessActionCapabilityPublisher businessActionCapabilityPublisher(
            BusinessObjectActionService actionService,
            CapabilityCatalogService catalogService,
            SecureActionStepValidator stepValidator,
            SecureActionPublishedModelPolicy publishedModelPolicy,
            ObjectMapper objectMapper) {
        return new BusinessActionCapabilityPublisher(
                actionService, catalogService, stepValidator, publishedModelPolicy, objectMapper);
    }

    @Bean
    public BusinessActionSourceService businessActionSourceService(
            BusinessObjectActionService actionService,
            SecureActionStepValidator stepValidator,
            SecureActionPublishedModelPolicy publishedModelPolicy) {
        return new BusinessActionSourceService(actionService, stepValidator, publishedModelPolicy);
    }

    /**
     * Runtime MCP exposure remains controlled by the feature switch. Capability publishing is an
     * authenticated control-plane operation and must not disappear when runtime exposure is off.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "forge.capability.secure-actions",
            name = "enabled",
            havingValue = "true")
    static class RuntimeConfiguration {

        @Bean
        SecureActionCatalogService secureActionCatalogService(
                SecureActionCatalogMapper catalogMapper,
                ObjectMapper objectMapper) {
            return new SecureActionCatalogService(catalogMapper, objectMapper);
        }

        @Bean
        SecureActionMcpHandler secureActionMcpHandler(
                SecureActionCatalogService catalogService,
                BusinessObjectActionService actionService,
                BusinessActionExecutionService executionService,
                SecureActionStepValidator stepValidator,
                SecureActionPublishedModelPolicy publishedModelPolicy,
                CapabilitySchemaValidator schemaValidator,
                CapabilityInvocationAuditService auditService,
                ObjectMapper objectMapper,
                List<GovernedCapabilityExecutionAdapter> executionAdapters) {
            return new SecureActionMcpHandler(
                    catalogService, actionService, executionService, stepValidator, publishedModelPolicy,
                    schemaValidator, auditService, objectMapper, executionAdapters);
        }

        @Bean
        SecureActionMcpToolContributor secureActionMcpToolContributor(
                SecureActionMcpHandler handler) {
            return new SecureActionMcpToolContributor(handler);
        }
    }
}
