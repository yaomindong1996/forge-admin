package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.FlowStartConfig;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InitiatorSelectedApproverSupportTest {

    @Test
    void discoverNoneCountersignAsSingleSelect() {
        UserTask task = new UserTask();
        task.setId("Node_2");
        task.setName("部门审批");
        ExtensionAttribute attribute = new ExtensionAttribute("assigneeType", "initiatorSelect");
        attribute.setNamespace(InitiatorSelectedApproverSupport.FLOWABLE_NS);
        attribute.setNamespacePrefix("flowable");
        task.addAttribute(attribute);

        Process process = new Process();
        process.setId("Process_1");
        process.addFlowElement(task);
        BpmnModel model = new BpmnModel();
        model.addProcess(process);

        List<FlowStartConfig.ApproverNode> nodes = InitiatorSelectedApproverSupport.discover(model);
        assertEquals(1, nodes.size());
        assertEquals("Node_2", nodes.get(0).getNodeKey());
        assertFalse(nodes.get(0).getMultiple());

        Map<String, Object> variables = new HashMap<>();
        variables.put("PROCESS_START_USER", Map.of("Node_2", List.of("1001", "1002")));
        InitiatorSelectedApproverSupport.validateAndNormalize(model, variables);
        assertEquals("1001", variables.get("INITIATOR_SELECT_Node_2"));
        assertEquals(List.of("1001"), ((Map<?, ?>) variables.get("PROCESS_START_USER")).get("Node_2"));
    }

    @Test
    void acceptSingleStringSelectionForNoneCountersign() {
        UserTask task = new UserTask();
        task.setId("Node_2");
        task.setName("审批人2");
        ExtensionAttribute attribute = new ExtensionAttribute("assigneeType", "initiatorSelect");
        attribute.setNamespace(InitiatorSelectedApproverSupport.FLOWABLE_NS);
        attribute.setNamespacePrefix("flowable");
        task.addAttribute(attribute);

        Process process = new Process();
        process.setId("Process_1");
        process.addFlowElement(task);
        BpmnModel model = new BpmnModel();
        model.addProcess(process);

        Map<String, Object> variables = new HashMap<>();
        variables.put("PROCESS_START_USER", Map.of("Node_2", "2090"));
        InitiatorSelectedApproverSupport.validateAndNormalize(model, variables);
        assertEquals("2090", variables.get("INITIATOR_SELECT_Node_2"));
        assertEquals(List.of("2090"), ((Map<?, ?>) variables.get("PROCESS_START_USER")).get("Node_2"));
    }

    @Test
    void splitsCommaJoinedSelectionsIntoSeparateApprovers() {
        assertEquals(List.of("1", "70"), InitiatorSelectedApproverSupport.readUserIds("1, 70,1"));
        assertEquals(List.of("5"), InitiatorSelectedApproverSupport.readUserIds(List.of("5", "5")));
        assertEquals(List.of(), InitiatorSelectedApproverSupport.readUserIds(" "));
    }
}
