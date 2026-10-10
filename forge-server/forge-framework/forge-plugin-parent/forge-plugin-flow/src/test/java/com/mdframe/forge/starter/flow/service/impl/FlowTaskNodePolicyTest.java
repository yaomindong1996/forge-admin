package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.helper.FlowNodePolicyParser;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowTaskNodePolicyTest {

    private FlowModelService flowModelService;
    private FlowNodeConfigService flowNodeConfigService;
    private FlowTaskNodePolicy policy;

    private RepositoryService repositoryService;

    @BeforeEach
    void setUp() {
        flowModelService = mock(FlowModelService.class);
        flowNodeConfigService = mock(FlowNodeConfigService.class);
        repositoryService = mock(RepositoryService.class);
        policy = new FlowTaskNodePolicy(
                repositoryService,
                mock(HistoryService.class),
                mock(TaskService.class),
                flowModelService,
                flowNodeConfigService,
                ignored -> "purchase-order");
    }

    @Test
    void nodeConfigurationMustOverrideBpmnActionPolicy() {
        Task task = task();
        UserTask flowNode = new UserTask();
        addFlowableAttribute(flowNode, "allowApprove", "false");
        addFlowableAttribute(flowNode, "allowReturn", "true");
        addFlowableAttribute(flowNode, "requireComment", "true");

        FlowModel model = new FlowModel();
        model.setId("model-1");
        model.setAllowMultiReturn(true);
        FlowNodeConfig nodeConfig = new FlowNodeConfig();
        nodeConfig.setAllowApprove(true);
        nodeConfig.setAllowReturn(false);
        nodeConfig.setRequireComment(false);
        when(flowModelService.getModelByKey("purchase-order")).thenReturn(model);
        when(flowNodeConfigService.getByModelAndNode("model-1", "review")).thenReturn(nodeConfig);

        TaskFormInfo formInfo = new TaskFormInfo();
        policy.applyApprovalPolicy(formInfo, task, null, flowNode);

        assertTrue(formInfo.getAllowApprove());
        assertFalse(formInfo.getAllowReturn());
        assertTrue(formInfo.getAllowMultiReturn());
        assertFalse(formInfo.getRequireComment());
    }

    @Test
    void requiredVariablesMustUseConfiguredMessage() {
        UserTask flowNode = new UserTask();
        addFlowableAttribute(flowNode, "requiredVariables", "amount; supplierId");
        addFlowableAttribute(flowNode, "requiredMessage", "请补齐采购单关键字段");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> policy.validateRequiredVariables(Map.of("amount", 100), flowNode));

        assertTrue(exception.getMessage().contains("请补齐采购单关键字段"));
        assertDoesNotThrow(() -> policy.validateRequiredVariables(
                Map.of("amount", 100, "supplierId", "supplier-1"), flowNode));
    }

    @Test
    void defaultApprovalMustStillRequireComment() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> policy.validateTaskAction(task(), "approve", "", null, new UserTask()));

        assertTrue(exception.getMessage().contains("审批意见"));
    }

    @Test
    void unconfiguredNodeMustAllowAddSign() {
        Task task = task();
        TaskFormInfo formInfo = new TaskFormInfo();

        policy.applyApprovalPolicy(formInfo, task, null, new UserTask());

        assertTrue(formInfo.getAllowAddSign());
        assertDoesNotThrow(() -> policy.validateAddSign(task));
    }

    @Test
    void nodeConfigColumnDefaultMustNotDisableAddSign() {
        Task task = task();
        FlowModel model = new FlowModel();
        model.setId("model-1");
        FlowNodeConfig nodeConfig = new FlowNodeConfig();
        nodeConfig.setAllowAddSign(false);
        when(flowModelService.getModelByKey("purchase-order")).thenReturn(model);
        when(flowNodeConfigService.getByModelAndNode("model-1", "review")).thenReturn(nodeConfig);

        TaskFormInfo formInfo = new TaskFormInfo();
        policy.applyApprovalPolicy(formInfo, task, null, new UserTask());

        assertTrue(formInfo.getAllowAddSign());
        assertDoesNotThrow(() -> policy.validateAddSign(task));
    }

    @Test
    void explicitBpmnFalseMustHideAndRejectAddSign() {
        Task task = task();
        UserTask flowNode = new UserTask();
        flowNode.setId("review");
        addFlowableAttribute(flowNode, "allowAddSign", "false");
        Process process = new Process();
        process.addFlowElement(flowNode);
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);
        when(repositoryService.getBpmnModel("purchase-order:1:definition")).thenReturn(bpmnModel);

        TaskFormInfo formInfo = new TaskFormInfo();
        policy.applyApprovalPolicy(formInfo, task, null, flowNode);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> policy.validateAddSign(task));

        assertFalse(formInfo.getAllowAddSign());
        assertEquals("当前节点不允许加签", exception.getMessage());
    }

    @Test
    void addSignPolicyMustOnlyGuardAddSignPath() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        int addStart = service.indexOf("private FlowTaskDynamicSignCoordinator addSignCoordinator()");
        int addEnd = service.indexOf("private FlowTaskDynamicSignCoordinator dynamicSignCoordinator(BiConsumer");
        int reduceStart = service.indexOf("public void reduceSign(String taskId, String userId, String targetUserId,"
                + " String reason, String signMode,");

        assertTrue(addStart > 0 && addEnd > addStart);
        assertTrue(service.substring(addStart, addEnd).contains("taskNodePolicy().validateAddSign(task)"));
        assertTrue(service.substring(reduceStart, reduceStart + 400).contains("dynamicSignCoordinator().mutate("));
        assertEquals(2, countOccurrences(service, "addSignCoordinator().mutate("));
    }

    private int countOccurrences(String source, String token) {
        int count = 0;
        for (int index = source.indexOf(token); index >= 0; index = source.indexOf(token, index + token.length())) {
            count++;
        }
        return count;
    }

    private Task task() {
        Task task = mock(Task.class);
        when(task.getProcessDefinitionId()).thenReturn("purchase-order:1:definition");
        when(task.getTaskDefinitionKey()).thenReturn("review");
        return task;
    }

    private void addFlowableAttribute(UserTask task, String name, String value) {
        ExtensionAttribute attribute = new ExtensionAttribute(name, value);
        attribute.setNamespace(FlowNodePolicyParser.FLOWABLE_NS);
        attribute.setNamespacePrefix("flowable");
        task.addAttribute(attribute);
    }
}
