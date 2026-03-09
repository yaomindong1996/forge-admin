package com.mdframe.forge.starter.flow.template;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程模板工厂
 * 提供常用 OA 流程模板
 */
@Component
public class FlowTemplateFactory {

    /**
     * 获取所有流程模板
     */
    public List<FlowTemplate> getAllTemplates() {
        List<FlowTemplate> templates = new ArrayList<>();
        templates.add(createLeaveTemplate());
        templates.add(createExpenseTemplate());
        templates.add(createApprovalTemplate());
        templates.add(createPurchaseTemplate());
        return templates;
    }

    /**
     * 请假流程模板
     */
    public FlowTemplate createLeaveTemplate() {
        return new FlowTemplate()
                .setTemplateKey("leave")
                .setTemplateName("请假流程")
                .setCategory("leave")
                .setDescription("员工请假申请审批流程，支持年假、病假、事假等")
                .setTitleTemplate("${startUserName}的请假申请")
                .setFormType("dynamic")
                .setBpmnXml(generateLeaveBpmnXml())
                .setVariables("{\"startUserName\":\"String\",\"leaveType\":\"String\",\"days\":\"Integer\",\"reason\":\"String\"}");
    }

    /**
     * 报销流程模板
     */
    public FlowTemplate createExpenseTemplate() {
        return new FlowTemplate()
                .setTemplateKey("expense")
                .setTemplateName("报销流程")
                .setCategory("expense")
                .setDescription("费用报销申请审批流程，支持差旅、招待、办公等费用")
                .setTitleTemplate("${startUserName}的报销申请")
                .setFormType("dynamic")
                .setBpmnXml(generateExpenseBpmnXml())
                .setVariables("{\"startUserName\":\"String\",\"expenseType\":\"String\",\"amount\":\"Double\",\"reason\":\"String\"}");
    }

    /**
     * 通用审批流程模板
     */
    public FlowTemplate createApprovalTemplate() {
        return new FlowTemplate()
                .setTemplateKey("approval")
                .setTemplateName("通用审批流程")
                .setCategory("approval")
                .setDescription("适用于各类行政审批事项")
                .setTitleTemplate("${startUserName}的审批申请")
                .setFormType("dynamic")
                .setBpmnXml(generateApprovalBpmnXml())
                .setVariables("{\"startUserName\":\"String\",\"title\":\"String\",\"content\":\"String\"}");
    }

    /**
     * 采购流程模板
     */
    public FlowTemplate createPurchaseTemplate() {
        return new FlowTemplate()
                .setTemplateKey("purchase")
                .setTemplateName("采购流程")
                .setCategory("purchase")
                .setDescription("采购申请审批流程")
                .setTitleTemplate("${startUserName}的采购申请")
                .setFormType("dynamic")
                .setBpmnXml(generatePurchaseBpmnXml())
                .setVariables("{\"startUserName\":\"String\",\"itemName\":\"String\",\"quantity\":\"Integer\",\"budget\":\"Double\"}");
    }

    /**
     * 生成请假流程 BPMN XML
     */
    private String generateLeaveBpmnXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:flowable=\"http://flowable.org/bpmn\" " +
                "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" " +
                "xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\" " +
                "xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\" " +
                "typeLanguage=\"http://www.w3.org/2001/XMLSchema\" " +
                "expressionLanguage=\"http://www.w3.org/1999/XPath\" " +
                "targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"leave\" name=\"请假流程\" isExecutable=\"true\">\n" +
                "    <documentation>员工请假申请审批流程</documentation>\n" +
                "    <startEvent id=\"startEvent\" name=\"开始\" flowable:initiator=\"initiator\"/>\n" +
                "    <userTask id=\"deptManagerTask\" name=\"部门经理审批\" flowable:assignee=\"${initiator}\">\n" +
                "      <documentation>部门经理审批请假申请</documentation>\n" +
                "    </userTask>\n" +
                "    <exclusiveGateway id=\"deptDecision\" name=\"部门审批结果\"/>\n" +
                "    <userTask id=\"hrTask\" name=\"HR审批\" flowable:assignee=\"hr\">\n" +
                "      <documentation>HR审批请假申请</documentation>\n" +
                "    </userTask>\n" +
                "    <exclusiveGateway id=\"hrDecision\" name=\"HR审批结果\"/>\n" +
                "    <endEvent id=\"endEvent\" name=\"结束\"/>\n" +
                "    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"deptManagerTask\"/>\n" +
                "    <sequenceFlow id=\"flow2\" sourceRef=\"deptManagerTask\" targetRef=\"deptDecision\"/>\n" +
                "    <sequenceFlow id=\"flow3\" sourceRef=\"deptDecision\" targetRef=\"hrTask\" name=\"通过\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${approved == 'true'}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "    <sequenceFlow id=\"flow4\" sourceRef=\"deptDecision\" targetRef=\"endEvent\" name=\"驳回\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${approved == 'false'}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "    <sequenceFlow id=\"flow5\" sourceRef=\"hrTask\" targetRef=\"hrDecision\"/>\n" +
                "    <sequenceFlow id=\"flow6\" sourceRef=\"hrDecision\" targetRef=\"endEvent\" name=\"通过\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${approved == 'true'}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "    <sequenceFlow id=\"flow7\" sourceRef=\"hrDecision\" targetRef=\"endEvent\" name=\"驳回\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${approved == 'false'}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "  </process>\n" +
                "</definitions>";
    }

    /**
     * 生成报销流程 BPMN XML
     */
    private String generateExpenseBpmnXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:flowable=\"http://flowable.org/bpmn\" " +
                "targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"expense\" name=\"报销流程\" isExecutable=\"true\">\n" +
                "    <startEvent id=\"startEvent\" name=\"开始\" flowable:initiator=\"initiator\"/>\n" +
                "    <userTask id=\"directManagerTask\" name=\"直接主管审批\" flowable:assignee=\"${initiator}\"/>\n" +
                "    <exclusiveGateway id=\"amountDecision\" name=\"金额判断\"/>\n" +
                "    <userTask id=\"deptManagerTask\" name=\"部门经理审批\"/>\n" +
                "    <userTask id=\"financeTask\" name=\"财务审批\" flowable:assignee=\"finance\"/>\n" +
                "    <endEvent id=\"endEvent\" name=\"结束\"/>\n" +
                "    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"directManagerTask\"/>\n" +
                "    <sequenceFlow id=\"flow2\" sourceRef=\"directManagerTask\" targetRef=\"amountDecision\"/>\n" +
                "    <sequenceFlow id=\"flow3\" sourceRef=\"amountDecision\" targetRef=\"deptManagerTask\" name=\"大于5000\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${amount > 5000}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "    <sequenceFlow id=\"flow4\" sourceRef=\"amountDecision\" targetRef=\"financeTask\" name=\"小于等于5000\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${amount <= 5000}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "    <sequenceFlow id=\"flow5\" sourceRef=\"deptManagerTask\" targetRef=\"financeTask\"/>\n" +
                "    <sequenceFlow id=\"flow6\" sourceRef=\"financeTask\" targetRef=\"endEvent\"/>\n" +
                "  </process>\n" +
                "</definitions>";
    }

    /**
     * 生成通用审批流程 BPMN XML
     */
    private String generateApprovalBpmnXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:flowable=\"http://flowable.org/bpmn\" " +
                "targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"approval\" name=\"通用审批流程\" isExecutable=\"true\">\n" +
                "    <startEvent id=\"startEvent\" name=\"开始\" flowable:initiator=\"initiator\"/>\n" +
                "    <userTask id=\"approveTask\" name=\"审批\" flowable:assignee=\"${initiator}\"/>\n" +
                "    <exclusiveGateway id=\"decision\" name=\"审批结果\"/>\n" +
                "    <endEvent id=\"endEvent\" name=\"结束\"/>\n" +
                "    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"approveTask\"/>\n" +
                "    <sequenceFlow id=\"flow2\" sourceRef=\"approveTask\" targetRef=\"decision\"/>\n" +
                "    <sequenceFlow id=\"flow3\" sourceRef=\"decision\" targetRef=\"endEvent\" name=\"通过\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${approved == 'true'}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "    <sequenceFlow id=\"flow4\" sourceRef=\"decision\" targetRef=\"endEvent\" name=\"驳回\">\n" +
                "      <conditionExpression xsi:type=\"tFormalExpression\">${approved == 'false'}</conditionExpression>\n" +
                "    </sequenceFlow>\n" +
                "  </process>\n" +
                "</definitions>";
    }

    /**
     * 生成采购流程 BPMN XML
     */
    private String generatePurchaseBpmnXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:flowable=\"http://flowable.org/bpmn\" " +
                "targetNamespace=\"http://www.flowable.org/processdef\">\n" +
                "  <process id=\"purchase\" name=\"采购流程\" isExecutable=\"true\">\n" +
                "    <startEvent id=\"startEvent\" name=\"开始\" flowable:initiator=\"initiator\"/>\n" +
                "    <userTask id=\"requesterTask\" name=\"申请人确认\"/>\n" +
                "    <userTask id=\"deptTask\" name=\"部门审批\"/>\n" +
                "    <userTask id=\"purchaseTask\" name=\"采购部审批\"/>\n" +
                "    <userTask id=\"financeTask\" name=\"财务审批\"/>\n" +
                "    <endEvent id=\"endEvent\" name=\"结束\"/>\n" +
                "    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent\" targetRef=\"requesterTask\"/>\n" +
                "    <sequenceFlow id=\"flow2\" sourceRef=\"requesterTask\" targetRef=\"deptTask\"/>\n" +
                "    <sequenceFlow id=\"flow3\" sourceRef=\"deptTask\" targetRef=\"purchaseTask\"/>\n" +
                "    <sequenceFlow id=\"flow4\" sourceRef=\"purchaseTask\" targetRef=\"financeTask\"/>\n" +
                "    <sequenceFlow id=\"flow5\" sourceRef=\"financeTask\" targetRef=\"endEvent\"/>\n" +
                "  </process>\n" +
                "</definitions>";
    }
}