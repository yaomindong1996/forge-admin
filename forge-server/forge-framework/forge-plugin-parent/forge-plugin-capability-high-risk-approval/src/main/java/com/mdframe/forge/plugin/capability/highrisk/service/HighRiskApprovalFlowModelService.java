package com.mdframe.forge.plugin.capability.highrisk.service;

import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class HighRiskApprovalFlowModelService {

    private final FlowClient flowClient;

    public void ensureModel(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("高风险审批缺少有效租户");
        }
        String defaultBpmn = HighRiskApprovalFlowDefinition.buildBpmn();
        FlowResult<Map<String, Object>> current = flowClient.getModelByKey(
                HighRiskApprovalFlowDefinition.MODEL_KEY);
        if (current != null && !current.isSuccess()) {
            throw new BusinessException("APPROVAL_FLOW_MODEL_LOOKUP_FAILED");
        }
        Map<String, Object> model = current == null ? null : current.getData();
        boolean shouldDeploy;
        if (model == null || model.get("id") == null) {
            FlowResult<Map<String, Object>> created = flowClient.createModel(
                    HighRiskApprovalFlowDefinition.modelPayload(null, tenantId, defaultBpmn));
            model = requireModel(created, "APPROVAL_FLOW_MODEL_CREATE_FAILED");
            shouldDeploy = true;
        }
        else {
            String existingBpmn = Objects.toString(model.get("bpmnXml"), "");
            shouldDeploy = !Integer.valueOf(1).equals(integer(model.get("status")))
                    || StringUtils.isBlank(Objects.toString(model.get("deploymentId"), null));
            if (StringUtils.isBlank(existingBpmn)) {
                FlowResult<Map<String, Object>> updated = flowClient.updateModel(
                        HighRiskApprovalFlowDefinition.modelPayload(model.get("id"), tenantId, defaultBpmn));
                model = requireModel(updated, "APPROVAL_FLOW_MODEL_UPDATE_FAILED");
                shouldDeploy = true;
            }
        }
        if (shouldDeploy) {
            FlowResult<String> deployed = flowClient.deployModel(Objects.toString(model.get("id"), ""));
            if (deployed == null || !deployed.isSuccess() || StringUtils.isBlank(deployed.getData())) {
                throw new BusinessException("APPROVAL_FLOW_MODEL_DEPLOY_FAILED");
            }
        }
    }

    private Map<String, Object> requireModel(
            FlowResult<Map<String, Object>> result, String errorCode) {
        if (result == null || !result.isSuccess() || result.getData() == null
                || result.getData().get("id") == null) {
            throw new BusinessException(errorCode);
        }
        return result.getData();
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }
}
