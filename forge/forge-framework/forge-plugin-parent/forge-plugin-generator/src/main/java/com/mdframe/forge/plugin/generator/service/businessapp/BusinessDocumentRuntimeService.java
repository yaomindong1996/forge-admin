package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentRuntimeVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务单据运行态服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessDocumentRuntimeService {

    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessFlowInstanceLinkMapper flowInstanceLinkMapper;
    private final BusinessPermissionService permissionService;
    private final DynamicCrudService dynamicCrudService;

    public BusinessDocumentRuntimeVO getRuntime(String objectCode, Long recordId) {
        BusinessDocumentRuntimeVO vo = new BusinessDocumentRuntimeVO();
        vo.setDocumentEnabled(false);
        if (StringUtils.isBlank(objectCode)) {
            vo.setMessage("业务对象编码不能为空");
            return vo;
        }
        String businessKey = buildBusinessKey(objectCode, recordId);
        vo.setBusinessKey(businessKey);

        AiBusinessDocumentConfig config = documentConfigService.selectEnabledByObjectCode(objectCode);
        if (config == null) {
            vo.setMessage("当前对象未启用单据模式");
            return vo;
        }
        vo.setDocumentEnabled(true);

        Map<String, Object> recordData = loadRecordData(config, recordId);
        if (recordData == null) {
            vo.setMessage("记录不存在或无权限访问");
            vo.setNextAction("SAVE_RECORD");
            return vo;
        }

        String documentStatus = text(firstPresent(recordData, config.getStatusField(), snakeToCamel(config.getStatusField())));
        vo.setDocumentStatus(documentStatus);
        vo.setDocumentStatusLabel(resolveStatusLabel(config, documentStatus));

        AiBusinessFlowInstanceLink link = flowInstanceLinkMapper.selectLatestByBusinessKey(resolveTenantId(), businessKey);
        if (link != null) {
            vo.setFlowStatus(link.getFlowStatus());
            vo.setProcessInstanceId(link.getProcessInstanceId());
        }

        List<String> actions = permissionService.resolveAvailableActions(objectCode, recordId, recordData);
        vo.setAvailableActions(actions);
        fillNextAction(vo, config, link, actions);
        fillRuntimeActions(vo, config, link, actions);
        return vo;
    }

    public void validateStartAllowed(String objectCode, Long recordId, boolean checkPermission) {
        BusinessDocumentRuntimeVO runtime = getRuntime(objectCode, recordId);
        if (!Boolean.TRUE.equals(runtime.getDocumentEnabled())) {
            throw new BusinessException(StringUtils.defaultIfBlank(runtime.getMessage(), "当前对象未启用单据模式"));
        }
        if (StringUtils.isBlank(runtime.getBusinessKey()) || recordId == null) {
            throw new BusinessException("请先保存记录后再发起主流程");
        }
        if (checkPermission && (runtime.getAvailableActions() == null
                || !runtime.getAvailableActions().contains("START_FLOW"))) {
            throw new BusinessException("缺少发起主流程权限");
        }
        if (!checkPermission) {
            if ("CONFIG_FLOW".equals(runtime.getNextAction())) {
                throw new BusinessException(StringUtils.defaultIfBlank(runtime.getMessage(), "请先配置主流程"));
            }
            if ("VIEW_FLOW".equals(runtime.getNextAction())) {
                throw new BusinessException("当前单据已有流转中的流程");
            }
            if ("WAIT_STATUS".equals(runtime.getNextAction())) {
                throw new BusinessException(StringUtils.defaultIfBlank(runtime.getMessage(), "当前单据状态不可发起主流程"));
            }
            return;
        }
        BusinessDocumentRuntimeVO.RuntimeActionVO startAction = findRuntimeAction(runtime, "START_FLOW");
        if (startAction == null || !Boolean.TRUE.equals(startAction.getVisible())) {
            throw new BusinessException(StringUtils.defaultIfBlank(runtime.getMessage(), "当前单据不可发起主流程"));
        }
        if (Boolean.TRUE.equals(startAction.getDisabled())) {
            throw new BusinessException(StringUtils.defaultIfBlank(startAction.getDisabledReason(), "当前单据不可发起主流程"));
        }
    }

    private Map<String, Object> loadRecordData(AiBusinessDocumentConfig config, Long recordId) {
        if (recordId == null || StringUtils.isBlank(config.getConfigKey())) {
            return null;
        }
        return dynamicCrudService.selectById(config.getConfigKey(), recordId);
    }

    private void fillNextAction(BusinessDocumentRuntimeVO vo, AiBusinessDocumentConfig config,
                                AiBusinessFlowInstanceLink link, List<String> actions) {
        BusinessDocumentConfigVO configVO = documentConfigService.toVO(config);
        Map<String, Object> mainFlowSummary = configVO.getMainFlowSummary();
        if (!isMainFlowConfigured(mainFlowSummary)) {
            vo.setNextAction("CONFIG_FLOW");
            vo.setMessage("单据模式已启用，尚未配置默认流程");
            return;
        }
        if (link != null && isRunningFlow(link.getFlowStatus())) {
            vo.setNextAction("VIEW_FLOW");
            vo.setMessage("流程流转中");
            return;
        }
        if (!isManualStartMode(text(mainFlowSummary.get("startMode")))) {
            vo.setNextAction("CONFIG_TRIGGER");
            vo.setMessage("当前主流程配置为触发器自动发起");
            return;
        }
        StatusPolicy statusPolicy = resolveStatusPolicy(configVO, vo.getDocumentStatus());
        if (!statusPolicy.allowStartFlow()) {
            vo.setNextAction("WAIT_STATUS");
            vo.setMessage(StringUtils.defaultIfBlank(statusPolicy.reason(), "当前单据状态不可发起主流程"));
            return;
        }
        if (actions.contains("START_FLOW")) {
            vo.setNextAction("START_FLOW");
            vo.setMessage("可发起主流程");
            return;
        }
        vo.setNextAction("REQUEST_PERMISSION");
        vo.setMessage("缺少可执行的单据动作权限");
    }

    private void fillRuntimeActions(BusinessDocumentRuntimeVO vo, AiBusinessDocumentConfig config,
                                    AiBusinessFlowInstanceLink link, List<String> actions) {
        List<BusinessDocumentRuntimeVO.RuntimeActionVO> runtimeActions = new ArrayList<>();
        BusinessDocumentConfigVO configVO = documentConfigService.toVO(config);
        Map<String, Object> mainFlowSummary = configVO.getMainFlowSummary();
        String startMode = mainFlowSummary == null ? "MANUAL" : text(mainFlowSummary.get("startMode"));
        if (!isManualStartMode(startMode)) {
            vo.setRuntimeActions(runtimeActions);
            return;
        }

        BusinessDocumentRuntimeVO.RuntimeActionVO action = new BusinessDocumentRuntimeVO.RuntimeActionVO();
        action.setKey("START_FLOW");
        action.setLabel("发起主流程");
        action.setType("success");
        action.setActionType("START_FLOW");
        action.setVisible(true);
        action.setDisabled(false);
        action.setObjectCode(config.getObjectCode());
        action.setRecordId(readRecordId(vo));

        if (!isMainFlowConfigured(mainFlowSummary)) {
            action.setDisabled(true);
            action.setDisabledReason("请先配置主流程");
        } else if (link != null && isRunningFlow(link.getFlowStatus())) {
            action.setDisabled(true);
            action.setDisabledReason("当前单据已有流转中的流程");
        } else {
            StatusPolicy statusPolicy = resolveStatusPolicy(configVO, vo.getDocumentStatus());
            if (!statusPolicy.allowStartFlow()) {
                action.setDisabled(true);
                action.setDisabledReason(StringUtils.defaultIfBlank(statusPolicy.reason(), "当前单据状态不可发起主流程"));
            } else if (actions == null || !actions.contains("START_FLOW")) {
                action.setDisabled(true);
                action.setDisabledReason("缺少发起主流程权限");
            }
        }
        runtimeActions.add(action);
        vo.setRuntimeActions(runtimeActions);
    }

    private BusinessDocumentRuntimeVO.RuntimeActionVO findRuntimeAction(BusinessDocumentRuntimeVO runtime,
                                                                        String actionKey) {
        if (runtime == null || runtime.getRuntimeActions() == null) {
            return null;
        }
        return runtime.getRuntimeActions().stream()
                .filter(action -> actionKey.equalsIgnoreCase(action.getKey()))
                .findFirst()
                .orElse(null);
    }

    private boolean isMainFlowConfigured(Map<String, Object> mainFlowSummary) {
        return mainFlowSummary != null && Boolean.TRUE.equals(mainFlowSummary.get("configured"))
                && StringUtils.isNotBlank(text(mainFlowSummary.get("flowModelKey")));
    }

    private boolean isManualStartMode(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        return "MANUAL".equals(normalized)
                || "BOTH".equals(normalized)
                || "MANUAL_AND_TRIGGER".equals(normalized)
                || "MANUAL_TRIGGER".equals(normalized);
    }

    private StatusPolicy resolveStatusPolicy(BusinessDocumentConfigVO configVO, String documentStatus) {
        if (StringUtils.isBlank(documentStatus)) {
            return new StatusPolicy(false, "单据状态为空，不能发起主流程");
        }
        if (configVO.getStatusMappingRows() != null) {
            for (BusinessDocumentConfigVO.StatusMappingRowVO row : configVO.getStatusMappingRows()) {
                if (row == null) {
                    continue;
                }
                boolean matched = documentStatus.equals(row.getStatusValue())
                        || documentStatus.equalsIgnoreCase(StringUtils.defaultString(row.getStandardStatus()));
                if (!matched) {
                    continue;
                }
                if (Boolean.TRUE.equals(row.getAllowStartFlow())) {
                    return new StatusPolicy(true, null);
                }
                String label = StringUtils.firstNonBlank(row.getDisplayName(), row.getStandardLabel(), documentStatus);
                return new StatusPolicy(false, "当前状态「" + label + "」不可发起主流程");
            }
        }
        if ("DRAFT".equalsIgnoreCase(documentStatus) || "SUBMITTED".equalsIgnoreCase(documentStatus)) {
            return new StatusPolicy(true, null);
        }
        return new StatusPolicy(false, "当前状态「" + documentStatus + "」不可发起主流程");
    }

    private Long readRecordId(BusinessDocumentRuntimeVO vo) {
        String businessKey = vo.getBusinessKey();
        if (StringUtils.isBlank(businessKey) || !businessKey.contains(":")) {
            return null;
        }
        String idText = StringUtils.substringAfter(businessKey, ":");
        try {
            return Long.valueOf(idText);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isRunningFlow(String flowStatus) {
        return "STARTED".equalsIgnoreCase(flowStatus) || "RUNNING".equalsIgnoreCase(flowStatus);
    }

    private String resolveStatusLabel(AiBusinessDocumentConfig config, String documentStatus) {
        if (StringUtils.isBlank(documentStatus)) {
            return null;
        }
        Map<String, String> mapping = documentConfigService.toVO(config).getStatusMapping();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (documentStatus.equals(entry.getValue())) {
                return switch (entry.getKey()) {
                    case "DRAFT" -> "草稿";
                    case "SUBMITTED" -> "已提交";
                    case "IN_PROCESS" -> "流程中";
                    case "APPROVED" -> "已通过";
                    case "REJECTED" -> "已驳回";
                    case "CANCELED" -> "已撤回";
                    case "CLOSED" -> "已关闭";
                    default -> entry.getKey();
                };
            }
        }
        return documentStatus;
    }

    private Object firstPresent(Map<String, Object> data, String... keys) {
        if (data == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (StringUtils.isNotBlank(key) && data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private String buildBusinessKey(String objectCode, Long recordId) {
        return objectCode + ":" + (recordId == null ? "" : recordId);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }

    private record StatusPolicy(boolean allowStartFlow, String reason) {
    }
}
