package com.mdframe.forge.plugin.capability.highrisk.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.highrisk.service.HighRiskApprovalSubmissionService;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessCodeFormProvider;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class HighRiskApprovalCodeFormProvider implements BusinessCodeFormProvider {

    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "token", "authorization", "header", "password", "secret", "apikey", "api_key");

    private final CapabilityApprovalMapper approvalMapper;
    private final HighRiskApprovalSubmissionService submissionService;
    private final ObjectMapper objectMapper;

    @Override
    public String providerKey() {
        return HighRiskApprovalFlowDefinition.PROVIDER_KEY;
    }

    @Override
    public String providerName() {
        return "高风险能力审批只读表单";
    }

    @Override
    public List<Map<String, Object>> formAssets(String objectCode) {
        if (objectCode != null && !objectCode.isBlank()
                && !HighRiskApprovalFlowDefinition.OBJECT_CODE.equals(objectCode)) {
            return List.of();
        }
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("objectCode", HighRiskApprovalFlowDefinition.OBJECT_CODE);
        asset.put("objectName", "高风险能力审批");
        asset.put("formKey", HighRiskApprovalFlowDefinition.FORM_KEY);
        asset.put("formName", "高风险能力审批表单");
        asset.put("formMode", "BUSINESS_CODE_FORM");
        asset.put("providerKey", providerKey());
        asset.put("fields", HighRiskApprovalFlowDefinition.fields());
        asset.put("fieldCatalog", HighRiskApprovalFlowDefinition.fields());
        asset.put("supportsSave", false);
        asset.put("description", "只读展示高风险动作及其受控变更摘要");
        return List.of(Map.copyOf(asset));
    }

    @Override
    public Map<Long, String> buildSummaries(String objectCode, Collection<Long> recordIds) {
        return Map.of();
    }

    @Override
    public BusinessTaskFormContextVO buildContext(
            BusinessTaskFormContextQueryDTO query,
            Map<String, Object> formRef,
            List<Map<String, Object>> fieldPermissions) {
        Long approvalId = approvalId(query == null ? null : query.getBusinessKey(),
                query == null ? null : query.getRecordId());
        AiCapabilityApproval approval = approvalMapper.selectTenantById(resolveTenantId(), approvalId);
        if (approval == null) {
            throw new BusinessException("高风险审批请求不存在");
        }
        Map<String, Object> input = submissionService.decryptAndVerify(approval);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("approvalRequestId", String.valueOf(approval.getId()));
        record.put("capabilityName", approval.getCapabilityCode());
        record.put("capabilityCode", approval.getCapabilityCode());
        record.put("capabilityVersion", approval.getCapabilityVersion());
        record.put("riskLevel", "HIGH");
        record.put("applicant", "用户 " + approval.getActorUserId());
        if (input.get("recordId") != null) {
            record.put("targetRecordId", String.valueOf(input.get("recordId")));
        }
        record.put("changeSummary", writeJson(sanitize(input.get("arguments"))));
        record.put("submittedAt", approval.getCreateTime());
        record.put("expiresAt", approval.getExpiresAt());

        BusinessTaskFormContextVO vo = new BusinessTaskFormContextVO();
        vo.setConfigured(true);
        vo.setFormType("business-code");
        vo.setTaskId(query == null ? null : query.getTaskId());
        vo.setBusinessKey(query == null ? null : query.getBusinessKey());
        vo.setProcessInstanceId(query == null ? null : query.getProcessInstanceId());
        vo.setProcessDefKey(query == null ? null : query.getProcessDefKey());
        vo.setTaskDefKey(query == null ? null : query.getTaskDefKey());
        vo.setObjectCode(HighRiskApprovalFlowDefinition.OBJECT_CODE);
        vo.setBusinessObjectName("高风险能力审批");
        vo.setBusinessSummary(approval.getCapabilityCode() + " / HIGH");
        vo.setRecordId(approval.getId());
        vo.setFormKey(HighRiskApprovalFlowDefinition.FORM_KEY);
        vo.setFormName("高风险能力审批表单");
        vo.setProviderKey(providerKey());
        vo.setFormRef(formRef == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formRef));
        vo.setFields(HighRiskApprovalFlowDefinition.fields());
        vo.setFieldPermissions(readonlyPermissions(fieldPermissions));
        vo.setRecordData(record);
        vo.setEditMode("readonly");
        return vo;
    }

    private List<Map<String, Object>> readonlyPermissions(List<Map<String, Object>> source) {
        if (source == null || source.isEmpty()) {
            return HighRiskApprovalFlowDefinition.fields().stream().map(field -> {
                Map<String, Object> permission = new LinkedHashMap<>();
                permission.put("field", field.get("field"));
                permission.put("label", field.get("label"));
                permission.put("readable", true);
                permission.put("writable", false);
                permission.put("required", false);
                return Map.copyOf(permission);
            }).toList();
        }
        return source.stream().map(item -> {
            Map<String, Object> permission = new LinkedHashMap<>(item);
            permission.put("writable", false);
            permission.put("required", false);
            return Map.copyOf(permission);
        }).toList();
    }

    private Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                String name = String.valueOf(key);
                String normalized = name.toLowerCase(Locale.ROOT);
                boolean sensitive = SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
                safe.put(name, sensitive ? "[已隐藏]" : sanitize(item));
            });
            return safe;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::sanitize).toList();
        }
        return value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        }
        catch (Exception exception) {
            throw new BusinessException("APPROVAL_PAYLOAD_INVALID");
        }
    }

    private Long approvalId(String businessKey, Long recordId) {
        if (businessKey != null && businessKey.startsWith("capability-approval:")) {
            try {
                return Long.valueOf(businessKey.substring("capability-approval:".length()));
            }
            catch (NumberFormatException ignored) {
                throw new BusinessException("高风险审批业务Key无效");
            }
        }
        if (recordId == null || recordId <= 0) {
            throw new BusinessException("高风险审批请求ID不能为空");
        }
        return recordId;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = SessionHelper.getTenantId();
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("高风险审批缺少有效租户");
        }
        return tenantId;
    }
}
