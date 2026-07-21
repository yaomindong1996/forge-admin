package com.mdframe.forge.starter.flow.job;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobFlowRuntimeServiceTest {

    @Mock
    private FlowModelMapper flowModelMapper;

    @Mock
    private FlowBusinessMapper flowBusinessMapper;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private IdentityService identityService;

    @Mock
    private ProcessDefinitionQuery definitionQuery;

    @Mock
    private ProcessDefinition processDefinition;

    @Mock
    private ProcessInstance processInstance;

    private JobFlowRuntimeService runtime;

    @BeforeEach
    void setUp() {
        JobFlowTechnicalIdentityProperties identity = new JobFlowTechnicalIdentityProperties();
        identity.setTenantId(1L);
        identity.setUserId("9001");
        identity.setUserName("任务流程服务");
        identity.setActiveOrgId(100L);
        identity.setActiveOrgName("平台运营中心");
        runtime = new JobFlowRuntimeService(flowModelMapper, flowBusinessMapper,
                repositoryService, runtimeService, identityService, identity);
    }

    @Test
    void shouldValidatePublishedSnapshotByExactProcessDefinitionId() {
        JobFlowBindingSnapshot snapshot = binding();
        when(flowModelMapper.selectPublishedJobBinding(1L, "daily-settlement", 7))
                .thenReturn(snapshot);
        stubDefinition(snapshot, false);

        JobFlowBindingSnapshot result = runtime.validateBinding("daily-settlement", 7);

        assertEquals(snapshot, result);
        verify(definitionQuery).processDefinitionId(snapshot.processDefinitionId());
        verify(definitionQuery, never()).latestVersion();
        verify(definitionQuery, never()).processDefinitionKey(any());
    }

    @Test
    void shouldRejectDraftOrMissingPublishedVersion() {
        when(flowModelMapper.selectPublishedJobBinding(1L, "draft-flow", 1))
                .thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> runtime.validateBinding("draft-flow", 1));

        assertTrue(error.getMessage().contains("已发布"));
        verify(repositoryService, never()).createProcessDefinitionQuery();
    }

    @Test
    void shouldRejectMissingOrSuspendedProcessDefinition() {
        JobFlowBindingSnapshot snapshot = binding();
        when(flowModelMapper.selectPublishedJobBinding(1L, "daily-settlement", 7))
                .thenReturn(snapshot);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionId(snapshot.processDefinitionId()))
                .thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(null, processDefinition);
        when(processDefinition.isSuspended()).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> runtime.validateBinding("daily-settlement", 7));
        BusinessException suspended = assertThrows(BusinessException.class,
                () -> runtime.validateBinding("daily-settlement", 7));
        assertTrue(suspended.getMessage().contains("挂起"));
    }

    @Test
    void shouldRejectDefinitionVersionOrDeploymentMismatch() {
        JobFlowBindingSnapshot snapshot = binding();
        when(flowModelMapper.selectPublishedJobBinding(1L, "daily-settlement", 7))
                .thenReturn(snapshot);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionId(snapshot.processDefinitionId()))
                .thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(processDefinition);
        when(processDefinition.getId()).thenReturn(snapshot.processDefinitionId());
        when(processDefinition.getKey()).thenReturn(snapshot.modelKey());
        when(processDefinition.getVersion()).thenReturn(8, 7);
        when(processDefinition.getDeploymentId()).thenReturn("deployment-other");

        BusinessException versionMismatch = assertThrows(BusinessException.class,
                () -> runtime.validateBinding("daily-settlement", 7));
        assertTrue(versionMismatch.getMessage().contains("版本"));
        BusinessException deploymentMismatch = assertThrows(BusinessException.class,
                () -> runtime.validateBinding("daily-settlement", 7));
        assertTrue(deploymentMismatch.getMessage().contains("部署"));
    }

    @Test
    void shouldStartExactDefinitionWithNestedInputAndTechnicalIdentity() {
        JobFlowBindingSnapshot snapshot = binding();
        JobFlowExecutionRequest request = request(snapshot);
        when(flowBusinessMapper.selectByBusinessKeyAndTenantId(1L, request.businessKey()))
                .thenReturn(null);
        when(flowModelMapper.selectPublishedJobBinding(1L, "daily-settlement", 7))
                .thenReturn(snapshot);
        stubDefinition(snapshot, false);
        when(flowBusinessMapper.insert(any(FlowBusiness.class))).thenReturn(1);
        when(runtimeService.startProcessInstanceById(
                eq(snapshot.processDefinitionId()), eq(request.businessKey()), any()))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("process-instance-1");
        when(flowBusinessMapper.updateById(any(FlowBusiness.class))).thenReturn(1);

        JobFlowExecutionResult result = runtime.start(request);

        assertEquals("process-instance-1", result.processInstanceId());
        assertFalse(result.recovered());
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceById(
                eq(snapshot.processDefinitionId()), eq(request.businessKey()), variables.capture());
        assertEquals(Map.of("amount", 1000, "currency", "CNY"), variables.getValue().get("jobInput"));
        assertEquals("9001", variables.getValue().get("initiator"));
        assertEquals(1L, variables.getValue().get("tenantId"));
        assertEquals(100L, variables.getValue().get("activeOrgId"));
        assertFalse(variables.getValue().containsKey("amount"));
        verify(identityService).setAuthenticatedUserId("9001");
        verify(identityService).setAuthenticatedUserId(null);
    }

    @Test
    void shouldReturnExistingProcessForDuplicateBusinessKey() {
        JobFlowExecutionRequest request = request(binding());
        FlowBusiness existing = new FlowBusiness();
        existing.setBusinessKey(request.businessKey());
        existing.setProcessInstanceId("process-instance-existing");
        when(flowBusinessMapper.selectByBusinessKeyAndTenantId(1L, request.businessKey()))
                .thenReturn(existing);

        JobFlowExecutionResult result = runtime.start(request);

        assertEquals("process-instance-existing", result.processInstanceId());
        assertTrue(result.recovered());
        verify(flowModelMapper, never()).selectPublishedJobBinding(any(), any(), any());
        verify(runtimeService, never()).startProcessInstanceById(any(), any(), any());
    }

    @Test
    void shouldRejectClientBindingThatNoLongerMatchesPublishedSnapshot() {
        JobFlowBindingSnapshot clientSnapshot = binding();
        JobFlowBindingSnapshot currentSnapshot = new JobFlowBindingSnapshot(
                "daily-settlement", 7, "deployment-7", "definition-other");
        JobFlowExecutionRequest request = request(clientSnapshot);
        when(flowBusinessMapper.selectByBusinessKeyAndTenantId(1L, request.businessKey()))
                .thenReturn(null);
        when(flowModelMapper.selectPublishedJobBinding(1L, "daily-settlement", 7))
                .thenReturn(currentSnapshot);
        stubDefinition(currentSnapshot, false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> runtime.start(request));

        assertTrue(error.getMessage().contains("快照"));
        verify(runtimeService, never()).startProcessInstanceById(any(), any(), any());
    }

    private void stubDefinition(JobFlowBindingSnapshot snapshot, boolean suspended) {
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
        when(definitionQuery.processDefinitionId(snapshot.processDefinitionId()))
                .thenReturn(definitionQuery);
        when(definitionQuery.singleResult()).thenReturn(processDefinition);
        when(processDefinition.getId()).thenReturn(snapshot.processDefinitionId());
        when(processDefinition.getKey()).thenReturn(snapshot.modelKey());
        when(processDefinition.getVersion()).thenReturn(snapshot.modelVersion());
        when(processDefinition.getDeploymentId()).thenReturn(snapshot.deploymentId());
        when(processDefinition.isSuspended()).thenReturn(suspended);
    }

    private JobFlowBindingSnapshot binding() {
        return new JobFlowBindingSnapshot(
                "daily-settlement", 7, "deployment-7", "daily-settlement:7:definition-7");
    }

    private JobFlowExecutionRequest request(JobFlowBindingSnapshot snapshot) {
        return new JobFlowExecutionRequest(
                11L, 22L, "job:11:22", snapshot,
                Map.of("amount", 1000, "currency", "CNY"));
    }
}
