package com.mdframe.forge.plugin.capability.highrisk.service;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class HighRiskApprovalFlowModelServiceTest {

    @Test
    void shouldCreateAndDeployMissingModel() {
        FlowClient client = mock(FlowClient.class);
        when(client.getModelByKey(HighRiskApprovalFlowDefinition.MODEL_KEY))
                .thenReturn(FlowResult.success(null));
        when(client.createModel(any())).thenReturn(FlowResult.success(Map.of("id", "model-1")));
        when(client.deployModel("model-1")).thenReturn(FlowResult.success("deployment-1"));

        new HighRiskApprovalFlowModelService(client).ensureModel(1L);

        verify(client).createModel(argThat(model -> String.valueOf(model.get("bpmnXml"))
                .contains("flowable:candidateGroups=\"${approvalCandidateGroup}\"")));
        verify(client).deployModel("model-1");
    }

    @Test
    void shouldPreserveExistingNonEmptyBpmn() {
        FlowClient client = mock(FlowClient.class);
        when(client.getModelByKey(HighRiskApprovalFlowDefinition.MODEL_KEY)).thenReturn(
                FlowResult.success(Map.of("id", "model-1", "bpmnXml", "designer-edited",
                        "status", 1, "deploymentId", "deployment-1")));

        new HighRiskApprovalFlowModelService(client).ensureModel(1L);

        verify(client, never()).updateModel(any());
        verify(client, never()).createModel(any());
        verify(client, never()).deployModel(anyString());
    }

    @Test
    void shouldFillEmptyBpmnAndDeploy() {
        FlowClient client = mock(FlowClient.class);
        when(client.getModelByKey(HighRiskApprovalFlowDefinition.MODEL_KEY)).thenReturn(
                FlowResult.success(Map.of("id", "model-1", "bpmnXml", "", "status", 0)));
        when(client.updateModel(any())).thenReturn(FlowResult.success(Map.of("id", "model-1")));
        when(client.deployModel("model-1")).thenReturn(FlowResult.success("deployment-1"));

        new HighRiskApprovalFlowModelService(client).ensureModel(1L);

        verify(client).updateModel(argThat(model -> assertBpmn(model)));
        verify(client).deployModel("model-1");
    }

    @Test
    void shouldFailClosedWhenModelLookupFails() {
        FlowClient client = mock(FlowClient.class);
        when(client.getModelByKey(HighRiskApprovalFlowDefinition.MODEL_KEY))
                .thenReturn(FlowResult.error("unavailable"));

        assertThatThrownBy(() -> new HighRiskApprovalFlowModelService(client).ensureModel(1L))
                .hasMessage("APPROVAL_FLOW_MODEL_LOOKUP_FAILED");
        verify(client, never()).createModel(any());
    }

    private boolean assertBpmn(Map<String, Object> model) {
        String bpmn = String.valueOf(model.get("bpmnXml"));
        assertThat(bpmn).contains("forge_capability_high_risk_approval")
                .contains("forge_capability_high_risk_approval_form")
                .contains("flowable:allowApprove=\"true\"")
                .contains("flowable:allowReject=\"true\"");
        return true;
    }
}
