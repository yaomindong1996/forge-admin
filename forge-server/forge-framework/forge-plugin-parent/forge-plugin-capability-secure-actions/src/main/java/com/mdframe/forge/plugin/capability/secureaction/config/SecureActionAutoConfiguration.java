package com.mdframe.forge.plugin.capability.secureaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.publish.BusinessActionCapabilityPublisher;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.mcp.SecureActionMcpHandler;
import com.mdframe.forge.plugin.capability.secureaction.mcp.SecureActionMcpToolContributor;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityInvocationAuditService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.capability.secureaction.spi.GovernedCapabilityExecutionAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 已发布业务动作的 MCP 受控写入组合配置。
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "forge.capability.secure-actions",
        name = "enabled",
        havingValue = "true")
public class SecureActionAutoConfiguration {

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
    public SecureActionCatalogService secureActionCatalogService(
            SecureActionCatalogMapper catalogMapper,
            ObjectMapper objectMapper) {
        return new SecureActionCatalogService(catalogMapper, objectMapper);
    }

    @Bean
    public SecureActionMcpHandler secureActionMcpHandler(
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
    public SecureActionMcpToolContributor secureActionMcpToolContributor(
            SecureActionMcpHandler handler) {
        return new SecureActionMcpToolContributor(handler);
    }
}
