package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessRunMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("business flow print identity")
class BusinessFlowServicePrintIdentityTest {

    private BusinessFlowService service;
    private BusinessProcessRunMapper processRuns;
    private BusinessApplicationObjectMapper applicationObjects;
    private Method attachPrintRuntimeIdentity;

    @BeforeEach
    void setUp() throws Exception {
        TenantContextHolder.setTenantId(7L);
        service = new BusinessFlowService(
                mock(BusinessBindingMapper.class),
                mock(BusinessFlowInstanceLinkMapper.class),
                mock(AiCrudConfigMapper.class),
                mock(BusinessObjectMapper.class),
                mock(BusinessDocumentConfigService.class),
                mock(BusinessDocumentRuntimeService.class),
                mock(DynamicCrudService.class),
                mock(BusinessFieldDesignService.class),
                mock(BusinessFlowVariableResolver.class),
                mock(BusinessCodeFormProviderRegistry.class),
                mock(ApplicationEventPublisher.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class));
        processRuns = mock(BusinessProcessRunMapper.class);
        applicationObjects = mock(BusinessApplicationObjectMapper.class);
        setField("businessProcessRunMapper", processRuns);
        setField("businessApplicationObjectMapper", applicationObjects);
        attachPrintRuntimeIdentity = BusinessFlowService.class.getDeclaredMethod(
                "attachPrintRuntimeIdentity", BusinessTaskFormContextVO.class,
                BusinessTaskFormContextQueryDTO.class);
        attachPrintRuntimeIdentity.setAccessible(true);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("process run pins the immutable run and application identity")
    void processRunPinsApplication() throws Exception {
        AiBusinessProcessRun run = new AiBusinessProcessRun();
        run.setId(31L);
        run.setApplicationId(41L);
        when(processRuns.selectByProcessInstanceId(7L, "pi-1")).thenReturn(run);

        BusinessTaskFormContextVO context = context("pi-1", "purchase", "purchase_order");
        context.setApplicationId("legacy-app");
        BusinessTaskFormContextVO result = attach(context, new BusinessTaskFormContextQueryDTO());

        assertEquals(31L, result.getProcessRunId());
        assertEquals("41", result.getApplicationId());
        assertTrue(result.getWarnings().stream().anyMatch(message -> message.contains("流程运行应用身份")));
    }

    @Test
    @DisplayName("legacy flow uses a unique published application for the object")
    void legacyFlowUsesUniquePublishedApplication() throws Exception {
        when(applicationObjects.selectPublishedApplicationIdsByObjectIdentity(
                7L, "purchase", "purchase_order")).thenReturn(List.of(51L));

        BusinessTaskFormContextVO result = attach(
                context("pi-2", "purchase", "purchase_order"),
                new BusinessTaskFormContextQueryDTO());

        assertNull(result.getProcessRunId());
        assertEquals("51", result.getApplicationId());
        verify(applicationObjects).selectPublishedApplicationIdsByObjectIdentity(
                7L, "purchase", "purchase_order");
    }

    @Test
    @DisplayName("ambiguous legacy application scope stays unresolved")
    void ambiguousLegacyApplicationIsRejectedUpstream() throws Exception {
        when(applicationObjects.selectPublishedApplicationIdsByObjectIdentity(
                7L, "purchase", "purchase_order")).thenReturn(List.of(51L, 52L));

        BusinessTaskFormContextVO result = attach(
                context("pi-3", "purchase", "purchase_order"),
                new BusinessTaskFormContextQueryDTO());

        assertNull(result.getApplicationId());
        assertTrue(result.getWarnings().stream().anyMatch(message -> message.contains("多个已发布应用")));
    }

    private BusinessTaskFormContextVO attach(BusinessTaskFormContextVO context,
                                              BusinessTaskFormContextQueryDTO query) throws Exception {
        return (BusinessTaskFormContextVO) attachPrintRuntimeIdentity.invoke(service, context, query);
    }

    private BusinessTaskFormContextVO context(String processInstanceId,
                                              String objectCode,
                                              String configKey) {
        BusinessTaskFormContextVO context = new BusinessTaskFormContextVO();
        context.setProcessInstanceId(processInstanceId);
        context.setObjectCode(objectCode);
        context.setConfigKey(configKey);
        return context;
    }

    private void setField(String name, Object value) throws Exception {
        Field field = BusinessFlowService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
