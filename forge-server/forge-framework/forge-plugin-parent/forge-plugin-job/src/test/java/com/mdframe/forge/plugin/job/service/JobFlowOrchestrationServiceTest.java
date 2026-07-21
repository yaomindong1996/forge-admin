package com.mdframe.forge.plugin.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import com.mdframe.forge.starter.job.flow.JobFlowExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobFlowOrchestrationServiceTest {

    private JobFlowExecutor flowExecutor;
    private JobFlowOrchestrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        flowExecutor = mock(JobFlowExecutor.class);
        ObjectProvider<JobFlowExecutor> executorProvider = mock(ObjectProvider.class);
        when(executorProvider.getIfAvailable()).thenReturn(flowExecutor);
        service = new JobFlowOrchestrationService(new ObjectMapper(), executorProvider);
    }

    @Test
    void shouldApplyTrustedBindingAndClearSingleExecutorTarget() {
        JobConfigSaveRequest request = flowRequest();
        JobFlowBindingSnapshot snapshot = binding();
        when(flowExecutor.validateBinding("daily-settlement", 7)).thenReturn(snapshot);
        SysJobConfig target = new SysJobConfig();
        target.setExecutorBean("staleBean");
        target.setExecutorMethod("execute");
        target.setExecutorHandler("staleHandler");
        target.setExecutorService("staleService");
        target.setExecuteMode("BEAN");

        service.applyBinding(request, target);

        assertEquals("FLOW", target.getInvokeMode());
        assertEquals(snapshot.modelKey(), target.getFlowModelKey());
        assertEquals(snapshot.modelVersion(), target.getFlowModelVersion());
        assertEquals(snapshot.deploymentId(), target.getFlowDeploymentId());
        assertEquals(snapshot.processDefinitionId(), target.getFlowProcessDefinitionId());
        assertNull(target.getExecuteMode());
        assertNull(target.getExecutorBean());
        assertNull(target.getExecutorMethod());
        assertNull(target.getExecutorHandler());
        assertNull(target.getExecutorService());
    }

    @Test
    void shouldClearFlowBindingWithoutCallingFlowServiceForSingleMode() {
        JobConfigSaveRequest request = flowRequest();
        request.setInvokeMode("SINGLE");
        SysJobConfig target = new SysJobConfig();
        target.setFlowModelKey("old-model");
        target.setFlowModelVersion(3);
        target.setFlowDeploymentId("old-deployment");
        target.setFlowProcessDefinitionId("old-definition");

        service.applyBinding(request, target);

        assertEquals("SINGLE", target.getInvokeMode());
        assertNull(target.getFlowModelKey());
        assertNull(target.getFlowModelVersion());
        assertNull(target.getFlowDeploymentId());
        assertNull(target.getFlowProcessDefinitionId());
        verify(flowExecutor, never()).validateBinding("daily-settlement", 7);
    }

    @Test
    void shouldRejectBindingSnapshotForAnotherModelVersion() {
        JobConfigSaveRequest request = flowRequest();
        when(flowExecutor.validateBinding("daily-settlement", 7)).thenReturn(
                new JobFlowBindingSnapshot("another-model", 8,
                        "deployment-8", "another-model:8:definition-8"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.applyBinding(request, new SysJobConfig()));

        assertTrue(exception.getMessage().contains("不一致"));
    }

    @Test
    void shouldStartWithDeterministicBusinessKeyAndNestedObjectInput() {
        JobDataMap dataMap = flowDataMap();
        when(flowExecutor.start(org.mockito.ArgumentMatchers.any())).thenReturn(
                new JobFlowExecutionResult("job:11:22", "process-22", false));

        String processInstanceId = service.start(11L, 22L, dataMap);

        assertEquals("process-22", processInstanceId);
        ArgumentCaptor<JobFlowExecutionRequest> request =
                ArgumentCaptor.forClass(JobFlowExecutionRequest.class);
        verify(flowExecutor).start(request.capture());
        assertEquals("job:11:22", request.getValue().businessKey());
        assertEquals(binding(), request.getValue().binding());
        assertEquals(Map.of("amount", 1000), request.getValue().jobInput());
    }

    @Test
    void shouldRejectNonObjectJobInputAtExecutionBoundary() {
        JobDataMap dataMap = flowDataMap();
        dataMap.put("jobParam", "[1,2]");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.start(11L, 22L, dataMap));

        assertTrue(exception.getMessage().contains("JSON对象"));
    }

    @Test
    void shouldRejectExplicitNullJobInputAtExecutionBoundary() {
        JobDataMap dataMap = flowDataMap();
        dataMap.put("jobParam", "null");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.start(11L, 22L, dataMap));

        assertTrue(exception.getMessage().contains("JSON对象"));
    }

    private JobConfigSaveRequest flowRequest() {
        JobConfigSaveRequest request = new JobConfigSaveRequest();
        request.setInvokeMode("FLOW");
        request.setFlowModelKey("daily-settlement");
        request.setFlowModelVersion(7);
        return request;
    }

    private JobDataMap flowDataMap() {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("flowModelKey", "daily-settlement");
        dataMap.put("flowModelVersion", 7);
        dataMap.put("flowDeploymentId", "deployment-7");
        dataMap.put("flowProcessDefinitionId", "daily-settlement:7:definition-7");
        dataMap.put("jobParam", "{\"amount\":1000}");
        return dataMap;
    }

    private JobFlowBindingSnapshot binding() {
        return new JobFlowBindingSnapshot(
                "daily-settlement", 7, "deployment-7", "daily-settlement:7:definition-7");
    }
}
