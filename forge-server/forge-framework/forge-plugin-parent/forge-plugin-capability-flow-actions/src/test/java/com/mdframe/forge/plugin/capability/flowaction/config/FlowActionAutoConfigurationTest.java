package com.mdframe.forge.plugin.capability.flowaction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionExecutionLogMapper;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionSourceMapper;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowProcessSystemServiceMapper;
import com.mdframe.forge.plugin.capability.flowaction.publish.FlowActionCapabilityController;
import com.mdframe.forge.plugin.capability.flowaction.publish.FlowActionCapabilityPublisher;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowApplicationSubmissionService;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowActionExecutionAdapter;
import com.mdframe.forge.plugin.capability.flowaction.service.FlowActionExecutionLogService;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FlowActionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlowActionAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(FlowActionSourceMapper.class, () -> mock(FlowActionSourceMapper.class))
            .withBean(FlowProcessSystemServiceMapper.class, () -> mock(FlowProcessSystemServiceMapper.class))
            .withBean(FlowClient.class, () -> mock(FlowClient.class))
            .withBean(CapabilityCatalogService.class, () -> mock(CapabilityCatalogService.class))
            .withBean(FlowActionExecutionLogMapper.class, () -> mock(FlowActionExecutionLogMapper.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(DynamicCrudService.class, () -> mock(DynamicCrudService.class))
            .withBean(LowcodeRuntimeDataSourceResolver.class,
                    () -> mock(LowcodeRuntimeDataSourceResolver.class))
            .withBean(BusinessFlowService.class, () -> mock(BusinessFlowService.class));

    @Test
    void shouldKeepControlPlaneAvailableWhenRuntimeExecutionIsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FlowActionSourceService.class);
            assertThat(context).hasSingleBean(FlowActionCapabilityPublisher.class);
            assertThat(context).doesNotHaveBean(FlowActionExecutionLogService.class);
            assertThat(context).doesNotHaveBean(FlowActionExecutionAdapter.class);
        });
    }

    @Test
    void shouldEnableRuntimeBeansOnlyWhenFlowActionsAreEnabled() {
        contextRunner
                .withPropertyValues("forge.capability.flow-actions.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(FlowActionSourceService.class);
                    assertThat(context).hasSingleBean(FlowActionCapabilityPublisher.class);
                    assertThat(context).hasSingleBean(FlowActionExecutionLogService.class);
                    assertThat(context).hasSingleBean(FlowApplicationSubmissionService.class);
                    assertThat(context).hasSingleBean(FlowActionExecutionAdapter.class);
                });
    }

    @Test
    void shouldNotConditionControlPlaneControllerOnRuntimeSwitch() {
        assertThat(FlowActionCapabilityController.class
                .getAnnotation(ConditionalOnProperty.class)).isNull();
    }
}
