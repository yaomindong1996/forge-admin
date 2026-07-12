package com.mdframe.forge.plugin.capability.flowaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionExecutionLogMapper;
import com.mdframe.forge.plugin.capability.flowaction.publish.FlowActionCapabilityPublisher;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowActionExecutionAdapter;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowActionExecutionLogService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceMapper;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = "forge.capability.flow-actions",
        name = "enabled",
        havingValue = "true")
public class FlowActionAutoConfiguration {

    @Bean
    public FlowActionSourceService flowActionSourceService(
            FlowActionSourceMapper sourceMapper,
            ObjectMapper objectMapper) {
        return new FlowActionSourceService(sourceMapper, objectMapper);
    }

    @Bean
    public FlowActionCapabilityPublisher flowActionCapabilityPublisher(
            FlowActionSourceService sourceService,
            CapabilityCatalogService catalogService,
            ObjectMapper objectMapper) {
        return new FlowActionCapabilityPublisher(sourceService, catalogService, objectMapper);
    }

    @Bean
    public FlowActionExecutionLogService flowActionExecutionLogService(
            FlowActionExecutionLogMapper logMapper,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        return new FlowActionExecutionLogService(logMapper, objectMapper, transactionManager);
    }

    @Bean
    public FlowActionExecutionAdapter flowActionExecutionAdapter(
            FlowActionSourceService sourceService,
            BusinessFlowService flowService,
            FlowActionExecutionLogService executionLogService) {
        return new FlowActionExecutionAdapter(sourceService, flowService, executionLogService);
    }
}
