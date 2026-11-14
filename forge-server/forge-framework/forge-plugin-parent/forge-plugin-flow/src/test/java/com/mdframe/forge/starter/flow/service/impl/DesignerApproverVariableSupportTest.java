package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DesignerApproverVariableSupportTest {

    private final FlowOrgIntegrationService org = mock(FlowOrgIntegrationService.class);

    @Test
    void resolvesBuiltInApproversFromInitiatorOrganization() {
        when(org.getLeaderUserIdByLevel("9", 1)).thenReturn("100");
        when(org.getDeptManagerUserIds("9")).thenReturn(List.of("200"));
        when(org.getUserIdsByRoleCode("hr")).thenReturn(List.of("300", "301"));
        Map<String, Object> variables = new HashMap<>();

        DesignerApproverVariableSupport.enrich(model("${initiatorLeader}", "${deptManager}", "${hr}"),
                variables, "9", "12", org);

        assertThat(variables)
                .containsEntry("initiatorLeader", "100")
                .containsEntry("deptManager", "200")
                .containsEntry("hr", "300");
    }

    @Test
    void refusesToStartWhenTheInitiatorHasNoLeader() {
        when(org.getLeaderUserIds("1")).thenReturn(List.of());

        assertThatThrownBy(() -> DesignerApproverVariableSupport.enrich(model("${initiatorLeader}"),
                new HashMap<>(), "1", "5", org))
                .hasMessage("发起人未设置上级领导，无法确定审批节点「审批1」的审批人");
    }

    @Test
    void keepsCallerProvidedValuesAndIgnoresOtherAssignees() {
        Map<String, Object> variables = new HashMap<>(Map.of("deptManager", "777"));

        DesignerApproverVariableSupport.enrich(model("${deptManager}", "42", "${flowSpelService.getInitiator(execution)}"),
                variables, "9", "12", org);

        assertThat(variables).containsEntry("deptManager", "777").doesNotContainKeys("initiatorLeader", "hr");
        verifyNoInteractions(org);
    }

    private BpmnModel model(String... assignees) {
        Process process = new Process();
        process.setId("p");
        for (int i = 0; i < assignees.length; i++) {
            UserTask task = new UserTask();
            task.setId("Node_" + (i + 1));
            task.setName("审批" + (i + 1));
            task.setAssignee(assignees[i]);
            process.addFlowElement(task);
        }
        BpmnModel model = new BpmnModel();
        model.addProcess(process);
        return model;
    }
}
