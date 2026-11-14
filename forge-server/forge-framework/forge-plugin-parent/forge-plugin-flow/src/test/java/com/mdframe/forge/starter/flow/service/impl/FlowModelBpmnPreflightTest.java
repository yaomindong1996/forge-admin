package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowModelBpmnPreflightTest {

    @Test
    void validMinimalProcessPassesAllChecks() {
        String xml = "<bpmn:process id=\"old\">"
                + "<bpmn:startEvent id=\"start\"/>"
                + "<bpmn:userTask id=\"approve\" flowable:candidateGroups=\"managers\"/>"
                + "<bpmn:endEvent id=\"end\"/>"
                + "<bpmn:sequenceFlow id=\"first\" sourceRef=\"start\" targetRef=\"approve\"/>"
                + "<bpmn:sequenceFlow id=\"last\" sourceRef=\"approve\" targetRef=\"end\"/>"
                + "</bpmn:process><bpmndi:BPMNPlane bpmnElement=\"old\"/>";

        assertEquals("old", FlowModelBpmnPreflight.extractProcessKey(xml));
        String replaced = FlowModelBpmnPreflight.replaceProcessId(xml, "current");
        assertEquals("current", FlowModelBpmnPreflight.extractProcessKey(replaced));
        assertTrue(replaced.contains("bpmnElement=\"current\""));
        assertDoesNotThrow(() -> FlowModelBpmnPreflight.validateSequenceFlowRefs(replaced));
        assertDoesNotThrow(() -> FlowModelBpmnPreflight.validateBpmnStructure(replaced));
        assertDoesNotThrow(() -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(replaced));
    }

    @Test
    void missingStartAndEndAreRejected() {
        RuntimeException missingStart = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateBpmnStructure("<bpmn:endEvent id=\"end\"/>"));
        assertTrue(missingStart.getMessage().contains("缺少开始节点"));
        RuntimeException missingEnd = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateBpmnStructure("<bpmn:startEvent id=\"start\"/>"));
        assertTrue(missingEnd.getMessage().contains("缺少结束节点"));
    }

    @Test
    void incompleteAndDanglingSequenceFlowsAreRejected() {
        String incomplete = "<bpmn:sequenceFlow id=\"line\" sourceRef=\"start\"/>";
        RuntimeException missingTarget = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateSequenceFlowRefs(incomplete));
        assertTrue(missingTarget.getMessage().contains("targetRef"));

        String dangling = "<bpmn:startEvent id=\"start\"/><bpmn:endEvent id=\"end\"/>"
                + "<bpmn:sequenceFlow id=\"line\" sourceRef=\"start\" targetRef=\"missing\"/>";
        RuntimeException badReference = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateBpmnStructure(dangling));
        assertTrue(badReference.getMessage().contains("悬空连线"));
    }

    @Test
    void unsupportedOrUnassignedExecutionIsRejected() {
        RuntimeException unassigned = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                        "<bpmn:userTask id=\"approve\"/>"));
        assertTrue(unassigned.getMessage().contains("未配置处理人"));
        RuntimeException script = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                        "<bpmn:scriptTask id=\"script\"/>"));
        assertTrue(script.getMessage().contains("暂不支持的执行类型"));
        RuntimeException delegate = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                        "<bpmn:serviceTask id=\"unsafe\" flowable:type=\"cc\" flowable:class=\"x.Y\"/>"));
        assertTrue(delegate.getMessage().contains("未注册的执行委托"));
        RuntimeException otherDelegate = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                        "<bpmn:serviceTask id=\"cc2\" flowable:type=\"cc\" flowable:delegateExpression=\"${evilBean}\"/>"));
        assertTrue(otherDelegate.getMessage().contains("未注册的执行委托"));
    }

    @Test
    void carbonCopyNodesAreRewrittenToEngineExecutableXml() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
                + " xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"t\">"
                + "<bpmn:process id=\"p\" isExecutable=\"true\">"
                + "<bpmn:startEvent id=\"s\"/>"
                + "<bpmn:serviceTask id=\"cc\" name=\"抄送人\" flowable:type=\"cc\" flowable:candidateUsers=\"1\""
                + " flowable:delegateExpression=\"${flowCcNodeDelegate}\"/>"
                + "<bpmn:endEvent id=\"e\"/>"
                + "<bpmn:sequenceFlow id=\"f1\" sourceRef=\"s\" targetRef=\"cc\"/>"
                + "<bpmn:sequenceFlow id=\"f2\" sourceRef=\"cc\" targetRef=\"e\"/>"
                + "</bpmn:process></bpmn:definitions>";

        String executable = FlowModelBpmnPreflight.toEngineExecutableXml(xml);

        assertFalse(executable.contains("flowable:type=\"cc\""));
        assertTrue(executable.contains("flowable:forgeNodeType=\"cc\""));
        assertTrue(executable.contains("flowable:delegateExpression=\"${flowCcNodeDelegate}\""));
        assertTrue(serviceTaskTypeErrors(xml) > 0, "原始设计器 XML 应触发 Flowable 服务任务类型校验");
        assertEquals(0, serviceTaskTypeErrors(executable), "转换后不应再触发服务任务类型校验");
        assertEquals(xml, FlowModelBpmnPreflight.toEngineExecutableXml(xml).replace("forgeNodeType", "type"));
    }

    @Test
    void designerCarbonCopyNodeWithRegisteredDelegateIsAllowed() {
        // 与前端 json-to-bpmn 生成的抄送节点一致
        assertDoesNotThrow(() -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                "<bpmn:serviceTask id=\"Node_5\" name=\"抄送人\" flowable:type=\"cc\" flowable:ccReceiverType=\"users\""
                        + " flowable:candidateUsers=\"1\" flowable:delegateExpression=\"${flowCcNodeDelegate}\"/>"));
        RuntimeException notCc = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                        "<bpmn:serviceTask id=\"svc\" flowable:delegateExpression=\"${flowCcNodeDelegate}\"/>"));
        assertTrue(notCc.getMessage().contains("flowable:type=cc"));
    }

    @Test
    void gatewayBranchRequiresConditionOrDefault() {
        String xml = "<bpmn:exclusiveGateway id=\"decision\"></bpmn:exclusiveGateway>"
                + "<bpmn:sequenceFlow id=\"branch\" sourceRef=\"decision\" targetRef=\"end\"/>";
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(xml));
        assertTrue(exception.getMessage().contains("缺少条件表达式或默认分支"));
        assertDoesNotThrow(() -> FlowModelBpmnPreflight.validateExecutableNodesAndGatewayConditions(
                xml.replace("id=\"decision\"", "id=\"decision\" default=\"branch\"")));
    }

    private static long serviceTaskTypeErrors(String xml) {
        var model = new org.flowable.bpmn.converter.BpmnXMLConverter().convertToBpmnModel(
                new org.flowable.common.engine.impl.util.io.StringStreamSource(xml), false, false);
        return new org.flowable.validation.ProcessValidatorFactory().createDefaultProcessValidator().validate(model)
                .stream().filter(error -> "flowable-servicetask-invalid-type".equals(error.getProblem())).count();
    }
}
