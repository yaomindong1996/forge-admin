package com.mdframe.forge.plugin.capability.highrisk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.highrisk.crypto.CapabilityPayloadCrypto;
import com.mdframe.forge.plugin.capability.highrisk.crypto.VersionedKekCapabilityPayloadCrypto;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityPolicyMapper;
import com.mdframe.forge.plugin.capability.highrisk.publish.HighRiskActionPublisher;
import com.mdframe.forge.plugin.capability.highrisk.form.HighRiskApprovalCodeFormProvider;
import com.mdframe.forge.plugin.capability.highrisk.mcp.HighRiskApprovalMcpToolContributor;
import com.mdframe.forge.plugin.capability.highrisk.mcp.HighRiskApprovalQueryService;
import com.mdframe.forge.plugin.capability.highrisk.service.CapabilityPolicyService;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalCallbackService;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalExecutionAdapter;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalFlowModelService;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalSubmissionService;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskBusinessStateService;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
import com.mdframe.forge.plugin.capability.schema.CapabilitySchemaValidator;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionCatalogService;
import com.mdframe.forge.plugin.capability.secureaction.publish.BusinessActionCapabilityPublisher;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessActionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@EnableConfigurationProperties(HighRiskApprovalProperties.class)
@ConditionalOnProperty(prefix = "forge.capability.high-risk", name = "enabled", havingValue = "true")
public class HighRiskApprovalAutoConfiguration {

    @Bean
    public CapabilityPayloadCrypto capabilityPayloadCrypto(HighRiskApprovalProperties properties) {
        if (!HighRiskApprovalFlowDefinition.MODEL_KEY.equals(properties.getFlowModelKey())) {
            throw new BusinessException("HIGH_RISK_FLOW_MODEL_KEY_INVALID");
        }
        return new VersionedKekCapabilityPayloadCrypto(properties);
    }

    @Bean
    public CapabilityPolicyService capabilityPolicyService(CapabilityPolicyMapper mapper) {
        return new CapabilityPolicyService(mapper);
    }

    @Bean
    public HighRiskActionPublisher highRiskActionPublisher(
            BusinessActionCapabilityPublisher definitionFactory,
            CapabilityCatalogService catalogService,
            CapabilityPolicyService policyService,
            HighRiskApprovalProperties properties,
            HighRiskApprovalFlowModelService flowModelService) {
        return new HighRiskActionPublisher(
                definitionFactory, catalogService, policyService, properties, flowModelService);
    }

    @Bean
    public HighRiskApprovalFlowModelService highRiskApprovalFlowModelService(FlowClient flowClient) {
        return new HighRiskApprovalFlowModelService(flowClient);
    }

    @Bean
    public HighRiskBusinessStateService highRiskBusinessStateService(
            BusinessObjectActionService actionService,
            DynamicCrudService crudService,
            ObjectMapper objectMapper) {
        return new HighRiskBusinessStateService(actionService, crudService, objectMapper);
    }

    @Bean
    public HighRiskApprovalSubmissionService highRiskApprovalSubmissionService(
            CapabilityApprovalMapper approvalMapper,
            CapabilityPolicyService policyService,
            AiCapabilityClientMapper clientMapper,
            CapabilityPayloadCrypto payloadCrypto,
            FlowClient flowClient,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            HighRiskBusinessStateService businessStateService) {
        return new HighRiskApprovalSubmissionService(
                approvalMapper, policyService, clientMapper, payloadCrypto,
                flowClient, objectMapper, transactionManager, businessStateService);
    }

    @Bean
    public HighRiskApprovalExecutionAdapter highRiskApprovalExecutionAdapter(
            CapabilityPolicyService policyService,
            HighRiskApprovalSubmissionService submissionService,
            BusinessObjectActionService actionService,
            SecureActionStepValidator stepValidator,
            SecureActionPublishedModelPolicy publishedModelPolicy) {
        return new HighRiskApprovalExecutionAdapter(
                policyService, submissionService, actionService, stepValidator, publishedModelPolicy);
    }

    @Bean
    public HighRiskApprovalCallbackService highRiskApprovalCallbackService(
            CapabilityApprovalMapper approvalMapper,
            CapabilityPolicyService policyService,
            AiCapabilityClientMapper clientMapper,
            IUserLoadService userLoadService,
            SecureActionCatalogService catalogService,
            CapabilitySchemaValidator schemaValidator,
            BusinessObjectActionService actionService,
            SecureActionStepValidator stepValidator,
            SecureActionPublishedModelPolicy modelPolicy,
            BusinessActionExecutionService executionService,
            HighRiskApprovalSubmissionService submissionService,
            HighRiskBusinessStateService stateService,
            ObjectMapper objectMapper) {
        return new HighRiskApprovalCallbackService(
                approvalMapper, policyService, clientMapper, userLoadService, catalogService, schemaValidator,
                actionService, stepValidator, modelPolicy, executionService, submissionService,
                stateService, objectMapper);
    }

    @Bean
    public HighRiskApprovalCodeFormProvider highRiskApprovalCodeFormProvider(
            CapabilityApprovalMapper approvalMapper,
            HighRiskApprovalSubmissionService submissionService,
            ObjectMapper objectMapper) {
        return new HighRiskApprovalCodeFormProvider(approvalMapper, submissionService, objectMapper);
    }

    @Bean
    public HighRiskApprovalQueryService highRiskApprovalQueryService(
            CapabilityApprovalMapper approvalMapper) {
        return new HighRiskApprovalQueryService(approvalMapper);
    }

    @Bean
    public HighRiskApprovalMcpToolContributor highRiskApprovalMcpToolContributor(
            HighRiskApprovalQueryService queryService,
            ObjectMapper objectMapper) {
        return new HighRiskApprovalMcpToolContributor(queryService, objectMapper);
    }
}
