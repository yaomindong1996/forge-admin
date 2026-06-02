package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessFlowInstanceLink;
import com.mdframe.forge.plugin.generator.mapper.BusinessFlowInstanceLinkMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentRuntimeVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
        return vo;
    }

    public void validateStartAllowed(String objectCode, Long recordId, boolean checkPermission) {
        BusinessDocumentRuntimeVO runtime = getRuntime(objectCode, recordId);
        if (!Boolean.TRUE.equals(runtime.getDocumentEnabled())) {
            throw new BusinessException(StringUtils.defaultIfBlank(runtime.getMessage(), "当前对象未启用单据模式"));
        }
        if (StringUtils.isBlank(runtime.getBusinessKey()) || recordId == null) {
            throw new BusinessException("请先保存记录后再发起流程");
        }
        if (checkPermission && (runtime.getAvailableActions() == null
                || !runtime.getAvailableActions().contains("START_FLOW"))) {
            throw new BusinessException("缺少发起流程权限");
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
        if (StringUtils.isBlank(config.getDefaultFlowKey())) {
            vo.setNextAction("CONFIG_FLOW");
            vo.setMessage("单据模式已启用，尚未配置默认流程");
            return;
        }
        if (link != null && isRunningFlow(link.getFlowStatus())) {
            vo.setNextAction("VIEW_FLOW");
            vo.setMessage("流程流转中");
            return;
        }
        if (actions.contains("START_FLOW")) {
            vo.setNextAction("START_FLOW");
            vo.setMessage("可发起流程");
            return;
        }
        vo.setNextAction("REQUEST_PERMISSION");
        vo.setMessage("缺少可执行的单据动作权限");
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
}
