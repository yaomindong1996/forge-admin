package com.mdframe.forge.plugin.capability.highrisk.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HighRiskApprovalFlowDefinition {

    public static final String MODEL_KEY = "forge_capability_high_risk_approval";
    public static final String BUSINESS_TYPE = "capability-approval";
    public static final String OBJECT_CODE = "capability-approval";
    public static final String FORM_KEY = "forge_capability_high_risk_approval_form";
    public static final String PROVIDER_KEY = "forgeCapabilityHighRiskApproval";

    private HighRiskApprovalFlowDefinition() {
    }

    public static String buildBpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:flowable="http://flowable.org/bpmn"
                  targetNamespace="http://mdframe.com/forge/capability">
                  <process id="forge_capability_high_risk_approval"
                    name="高风险能力人工审批" isExecutable="true">
                    <startEvent id="start" name="提交审批"/>
                    <sequenceFlow id="flow_start_approval" sourceRef="start" targetRef="manual_approval"/>
                    <userTask id="manual_approval" name="高风险动作审批"
                      flowable:candidateGroups="${approvalCandidateGroup}"
                      flowable:formKey="forge_capability_high_risk_approval_form"
                      flowable:formFieldPermissions='[
                        {"field":"approvalRequestId","label":"审批请求ID","readable":true,"writable":false,"required":false},
                        {"field":"capabilityName","label":"能力名称","readable":true,"writable":false,"required":false},
                        {"field":"capabilityCode","label":"能力编码","readable":true,"writable":false,"required":false},
                        {"field":"capabilityVersion","label":"能力版本","readable":true,"writable":false,"required":false},
                        {"field":"riskLevel","label":"风险等级","readable":true,"writable":false,"required":false},
                        {"field":"applicant","label":"申请人","readable":true,"writable":false,"required":false},
                        {"field":"targetRecordId","label":"目标记录ID","readable":true,"writable":false,"required":false},
                        {"field":"changeSummary","label":"变更内容","readable":true,"writable":false,"required":false},
                        {"field":"submittedAt","label":"提交时间","readable":true,"writable":false,"required":false},
                        {"field":"expiresAt","label":"过期时间","readable":true,"writable":false,"required":false}
                      ]'
                      flowable:allowApprove="true"
                      flowable:allowReject="true"
                      flowable:allowDelegate="false"
                      flowable:allowReturn="false"
                      flowable:allowTerminate="false"
                      flowable:requireComment="true"/>
                    <sequenceFlow id="flow_approval_end" sourceRef="manual_approval" targetRef="end"/>
                    <endEvent id="end" name="审批结束"/>
                  </process>
                </definitions>
                """;
    }

    public static Map<String, Object> modelPayload(Object id, Long tenantId, String bpmnXml) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (id != null) {
            payload.put("id", id);
        }
        payload.put("tenantId", tenantId);
        payload.put("modelKey", MODEL_KEY);
        payload.put("modelName", "高风险能力人工审批");
        payload.put("description", "Forge MCP HIGH 业务动作专用人工审批流程");
        payload.put("category", "capability");
        payload.put("flowType", "capability");
        payload.put("designerType", "approval");
        payload.put("formType", "business");
        payload.put("formId", FORM_KEY);
        payload.put("formJson", formRefJson());
        payload.put("notifyType", "redis");
        payload.put("bpmnXml", bpmnXml);
        return payload;
    }

    public static String formRefJson() {
        return """
                {"type":"BUSINESS_CODE_FORM","formMode":"BUSINESS_CODE_FORM",
                 "objectCode":"capability-approval","objectName":"高风险能力审批",
                 "formKey":"forge_capability_high_risk_approval_form",
                 "formName":"高风险能力审批表单",
                 "providerKey":"forgeCapabilityHighRiskApproval"}
                """;
    }

    public static List<Map<String, Object>> fields() {
        return List.of(
                field("approvalRequestId", "审批请求ID", "input"),
                field("capabilityName", "能力名称", "input"),
                field("capabilityCode", "能力编码", "input"),
                field("capabilityVersion", "能力版本", "input"),
                field("riskLevel", "风险等级", "input"),
                field("applicant", "申请人", "input"),
                field("targetRecordId", "目标记录ID", "input"),
                field("changeSummary", "变更内容", "textarea"),
                field("submittedAt", "提交时间", "datetime"),
                field("expiresAt", "过期时间", "datetime"));
    }

    private static Map<String, Object> field(String code, String label, String componentType) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("field", code);
        field.put("fieldCode", code);
        field.put("label", label);
        field.put("componentType", componentType);
        field.put("readonly", true);
        return Map.copyOf(field);
    }
}
