package com.mdframe.forge.plugin.capability.flowaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionExecutionLogMapper;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionSourceMapper;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowProcessSystemServiceMapper;
import com.mdframe.forge.plugin.capability.flowaction.publish.FlowActionCapabilityPublisher;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowApplicationSubmissionService;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowActionExecutionAdapter;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowActionExecutionLogService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.capability.flowaction.system.FlowProcessStartSystemService;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
public class FlowActionAutoConfiguration {

    @Bean
    public FlowProcessStartSystemService flowProcessStartSystemService(
            FlowProcessSystemServiceMapper mapper,
            FlowClient flowClient,
            ObjectMapper objectMapper) {
        return new FlowProcessStartSystemService(mapper, flowClient, objectMapper);
    }

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

    /**
     * Runtime execution remains fail-closed. Publishing and registration-source validation are
     * control-plane operations and stay available even when external execution is disabled.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "forge.capability.flow-actions",
            name = "enabled",
            havingValue = "true")
    static class RuntimeConfiguration {

        @Bean
        FlowActionExecutionLogService flowActionExecutionLogService(
                FlowActionExecutionLogMapper logMapper,
                ObjectMapper objectMapper,
                PlatformTransactionManager transactionManager) {
            return new FlowActionExecutionLogService(logMapper, objectMapper, transactionManager);
        }

        @Bean
        FlowApplicationSubmissionService flowApplicationSubmissionService(
                DynamicCrudService dynamicCrudService,
                LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver,
                ObjectMapper objectMapper) {
            return new FlowApplicationSubmissionService(
                    dynamicCrudService, runtimeDataSourceResolver, objectMapper);
        }

        @Bean
        FlowActionExecutionAdapter flowActionExecutionAdapter(
                FlowActionSourceService sourceService,
                BusinessFlowService flowService,
                FlowActionExecutionLogService executionLogService,
                FlowApplicationSubmissionService submissionService,
                ObjectMapper objectMapper) {
            return new FlowActionExecutionAdapter(
                    sourceService, flowService, executionLogService, submissionService, objectMapper);
        }
    }
}
