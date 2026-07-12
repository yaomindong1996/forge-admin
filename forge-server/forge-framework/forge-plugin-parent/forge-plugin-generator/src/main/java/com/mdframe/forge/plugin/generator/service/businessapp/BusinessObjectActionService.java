package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessReadinessStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectActionDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectDesignVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessReadinessItemVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 业务对象自定义操作和权限流程摘要服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectActionService {

    private static final String DESIGNER_ACTIONS_KEY = "actions";
    private static final Set<String> ACTION_POSITIONS = Set.of("TOOLBAR", "ROW", "DETAIL");
    private static final Set<String> ACTION_TYPES = Set.of("OPEN_PAGE", "CALL_API", "START_FLOW", "START_APPROVAL", "TRIGGER", "OPEN_EXTERNAL", "COMMAND");
    private static final String PERMISSION_PATTERN = "^[A-Za-z0-9:_-]{3,128}$";

    private final ObjectMapper objectMapper;
    private final BusinessObjectDesignerService designerService;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessObjectDesignVersionMapper designVersionMapper;
    private final BusinessPermissionService permissionService;

    public List<BusinessObjectActionVO> listActions(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        return readActions(context.getObject().getDesignerOptions());
    }

    public ResolvedBusinessAction resolveAction(String suiteCode, String objectCode, String actionCode) {
        String normalizedObjectCode = StringUtils.trimToNull(objectCode);
        String normalizedActionCode = normalizeActionCode(actionCode);
        if (StringUtils.isBlank(normalizedObjectCode)) {
            throw new BusinessException("业务对象编码不能为空");
        }
        AiBusinessObject object = StringUtils.isNotBlank(suiteCode)
                ? businessObjectMapper.selectByObjectCode(resolveTenantId(), suiteCode.trim(), normalizedObjectCode)
                : businessObjectMapper.selectFirstByObjectCode(resolveTenantId(), normalizedObjectCode);
        if (object == null) {
            throw new BusinessException("业务对象不存在: " + normalizedObjectCode);
        }
        BusinessObjectActionVO action = readActions(object.getDesignerOptions()).stream()
                .filter(item -> normalizedActionCode.equalsIgnoreCase(item.getActionCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("业务动作不存在: " + normalizedActionCode));
        if (Integer.valueOf(0).equals(action.getStatus())) {
            throw new BusinessException("业务动作已停用: " + action.getActionName());
        }
        return new ResolvedBusinessAction(object, action);
    }

    public ResolvedPublishedBusinessAction resolvePublishedAction(
            String suiteCode,
            String objectCode,
            String actionCode,
            Integer publishVersion) {
        String normalizedObjectCode = StringUtils.trimToNull(objectCode);
        String normalizedActionCode = normalizeActionCode(actionCode);
        if (StringUtils.isBlank(normalizedObjectCode)) {
            throw new BusinessException("业务对象编码不能为空");
        }
        Long tenantId = requireTenantId();
        AiBusinessObject object = StringUtils.isNotBlank(suiteCode)
                ? businessObjectMapper.selectByObjectCode(tenantId, suiteCode.trim(), normalizedObjectCode)
                : businessObjectMapper.selectFirstByObjectCode(tenantId, normalizedObjectCode);
        if (object == null || !Integer.valueOf(1).equals(object.getStatus())) {
            throw new BusinessException("已发布业务对象不存在或已停用: " + normalizedObjectCode);
        }
        AiBusinessObjectDesignVersion version = designVersionMapper.selectPublishedVersion(
                tenantId, object.getId(), publishVersion);
        if (version == null || StringUtils.isBlank(version.getDesignerOptionsSnapshot())) {
            throw new BusinessException("业务对象缺少可执行的发布快照: " + normalizedObjectCode);
        }
        BusinessObjectActionVO action = readActions(version.getDesignerOptionsSnapshot()).stream()
                .filter(item -> normalizedActionCode.equalsIgnoreCase(item.getActionCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("发布版本中不存在业务动作: " + normalizedActionCode));
        if (Integer.valueOf(0).equals(action.getStatus())) {
            throw new BusinessException("发布版本中的业务动作已停用: " + action.getActionName());
        }
        return new ResolvedPublishedBusinessAction(object, action, version);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveActions(Long objectId, List<BusinessObjectActionDTO> actions) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        Map<String, Object> options = readOptions(context.getObject().getDesignerOptions());
        options.put(DESIGNER_ACTIONS_KEY, normalizeActions(actions));
        context.getObject().setDesignerOptions(writeJson(options));
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    public BusinessReadinessItemVO permissionSummary(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        BusinessPermissionSummaryVO actionSummary = permissionService.documentActionSummary(objectId);
        List<String> missingRequiredActions = actionSummary.getActionPermissions().stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequired()) && !Boolean.TRUE.equals(item.getConfigured()))
                .map(BusinessPermissionSummaryVO.ActionPermissionVO::getActionName)
                .toList();
        AiBusinessBinding permissionBinding = bindingMapper.selectBindingByTypeAndCode(
                resolveTenantId(), "OBJECT", context.getObject().getObjectCode(), "PERMISSION");
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("PERMISSION_SUMMARY");
        item.setItemName("对象权限");
        if (!missingRequiredActions.isEmpty()) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("权限缺口");
            item.setMessage("关键单据动作权限未配置: " + String.join("、", missingRequiredActions));
            item.setNextAction("CONFIGURE_PERMISSION");
            item.setNextActionLabel("配置权限");
            item.setNextActionUrl("/system/role");
            return item;
        }
        if (permissionBinding == null) {
            item.setStatus(BusinessReadinessStatus.CONFIGURED);
            item.setStatusLabel("基础权限已就绪");
            item.setMessage("关键单据动作权限已存在，对象权限尚未挂接，发布后将依赖菜单和接口默认权限");
            item.setNextAction("CONFIGURE_PERMISSION");
            item.setNextActionLabel("配置权限");
            item.setNextActionUrl("/system/role");
            return item;
        }
        if (Integer.valueOf(1).equals(permissionBinding.getStatus())) {
            item.setStatus(BusinessReadinessStatus.RUNNABLE);
            item.setStatusLabel("已配置");
            item.setMessage("对象权限已挂接: " + permissionBinding.getBindingName());
        } else {
            item.setStatus(BusinessReadinessStatus.ERROR);
            item.setStatusLabel("已停用");
            item.setMessage("对象权限挂接已停用: " + permissionBinding.getBindingName());
            item.setNextAction("ENABLE_PERMISSION");
            item.setNextActionLabel("启用权限挂接");
        }
        return item;
    }

    private List<BusinessObjectActionVO> readActions(String designerOptions) {
        Object raw = readOptions(designerOptions).get(DESIGNER_ACTIONS_KEY);
        if (!(raw instanceof List<?> list)) {
            return defaultActions();
        }
        List<BusinessObjectActionVO> actions = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                actions.add(toVO(map));
            }
        }
        actions.sort(Comparator.comparing(action -> action.getSortOrder() == null ? Integer.MAX_VALUE : action.getSortOrder()));
        return actions;
    }

    private List<Map<String, Object>> normalizeActions(List<BusinessObjectActionDTO> actions) {
        if (actions == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (BusinessObjectActionDTO dto : actions) {
            if (dto == null) {
                continue;
            }
            BusinessObjectActionVO vo = normalizeAction(dto);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("actionCode", vo.getActionCode());
            item.put("actionName", vo.getActionName());
            item.put("actionPosition", vo.getActionPosition());
            item.put("actionType", vo.getActionType());
            item.put("permission", vo.getPermission());
            item.put("confirmRequired", vo.getConfirmRequired());
            item.put("successMessage", vo.getSuccessMessage());
            item.put("failureMessage", vo.getFailureMessage());
            item.put("status", vo.getStatus());
            item.put("sortOrder", vo.getSortOrder());
            item.put("actionConfig", vo.getActionConfig());
            result.add(item);
        }
        return result;
    }

    private BusinessObjectActionVO normalizeAction(BusinessObjectActionDTO dto) {
        String actionCode = normalizeActionCode(dto.getActionCode());
        String actionName = StringUtils.trimToNull(dto.getActionName());
        if (StringUtils.isBlank(actionName)) {
            throw new BusinessException("操作名称不能为空");
        }
        String position = normalizePosition(dto.getActionPosition());
        String type = normalizeType(dto.getActionType());
        BusinessObjectActionVO vo = new BusinessObjectActionVO();
        vo.setActionCode(actionCode);
        vo.setActionName(actionName);
        vo.setActionPosition(position);
        vo.setActionType(type);
        vo.setPermission(normalizePermission(dto.getPermission(), type));
        vo.setConfirmRequired(Boolean.TRUE.equals(dto.getConfirmRequired()));
        vo.setSuccessMessage(StringUtils.trimToNull(dto.getSuccessMessage()));
        vo.setFailureMessage(StringUtils.trimToNull(dto.getFailureMessage()));
        vo.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        vo.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        vo.setActionConfig(normalizeActionConfig(type, dto.getActionConfig()));
        return vo;
    }

    private BusinessObjectActionVO toVO(Map<?, ?> map) {
        BusinessObjectActionVO vo = new BusinessObjectActionVO();
        vo.setActionCode(text(map.get("actionCode")));
        vo.setActionName(text(map.get("actionName")));
        vo.setActionPosition(text(map.get("actionPosition")));
        vo.setActionType(text(map.get("actionType")));
        vo.setPermission(text(map.get("permission")));
        vo.setConfirmRequired(booleanValue(map.get("confirmRequired")));
        vo.setSuccessMessage(text(map.get("successMessage")));
        vo.setFailureMessage(text(map.get("failureMessage")));
        vo.setStatus(intValue(map.get("status"), 1));
        vo.setSortOrder(intValue(map.get("sortOrder"), 0));
        Object actionConfig = map.get("actionConfig");
        if (actionConfig instanceof Map<?, ?> config) {
            Map<String, Object> typed = new LinkedHashMap<>();
            config.forEach((key, value) -> typed.put(String.valueOf(key), value));
            vo.setActionConfig(typed);
        }
        return vo;
    }

    private List<BusinessObjectActionVO> defaultActions() {
        return List.of();
    }

    private BusinessObjectActionVO defaultAction(String code, String name, String position, Integer sortOrder) {
        BusinessObjectActionVO vo = new BusinessObjectActionVO();
        vo.setActionCode(code);
        vo.setActionName(name);
        vo.setActionPosition(position);
        vo.setActionType("OPEN_PAGE");
        vo.setConfirmRequired("delete".equals(code));
        vo.setStatus(1);
        vo.setSortOrder(sortOrder);
        return vo;
    }

    private Map<String, Object> readOptions(String designerOptions) {
        if (StringUtils.isBlank(designerOptions)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(designerOptions, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("操作配置序列化失败");
        }
    }

    private String normalizePosition(String actionPosition) {
        String position = StringUtils.defaultIfBlank(actionPosition, "ROW")
                .replace("-", "_")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (!ACTION_POSITIONS.contains(position)) {
            throw new BusinessException("操作位置不正确: " + actionPosition);
        }
        return position;
    }

    private String normalizeType(String actionType) {
        String type = StringUtils.defaultIfBlank(actionType, "OPEN_PAGE")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace("-", "_")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (!ACTION_TYPES.contains(type)) {
            throw new BusinessException("操作类型不正确: " + actionType);
        }
        if ("START_APPROVAL".equals(type)) {
            return "START_FLOW";
        }
        return type;
    }

    private String normalizePermission(String permission, String actionType) {
        String actualPermission = StringUtils.trimToNull(permission);
        if (StringUtils.isBlank(actualPermission)) {
            actualPermission = switch (actionType) {
                case "START_FLOW" -> "ai:businessFlow:start";
                case "TRIGGER" -> "ai:businessTrigger:execute";
                case "COMMAND" -> "ai:businessAction:execute";
                default -> null;
            };
        }
        if (StringUtils.isNotBlank(actualPermission) && !actualPermission.matches(PERMISSION_PATTERN)) {
            throw new BusinessException("权限标识格式不正确: " + actualPermission);
        }
        return actualPermission;
    }

    private Map<String, Object> normalizeActionConfig(String actionType, Map<String, Object> config) {
        if (!"START_FLOW".equals(actionType)) {
            return config == null ? new LinkedHashMap<>() : config;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("useMainFlow", true);
        return result;
    }

    private String normalizeActionCode(String actionCode) {
        String normalized = StringUtils.defaultString(actionCode)
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .toLowerCase(Locale.ROOT)
                .replaceAll("^_+|_+$", "");
        if (StringUtils.isBlank(normalized)) {
            throw new BusinessException("操作编码不能为空");
        }
        return StringUtils.left(normalized, 64);
    }

    private String text(Object value) {
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text);
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
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

    private Long requireTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        }
        catch (Exception exception) {
            tenantId = null;
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }

    public record ResolvedBusinessAction(AiBusinessObject object, BusinessObjectActionVO action) {
    }

    public record ResolvedPublishedBusinessAction(
            AiBusinessObject object,
            BusinessObjectActionVO action,
            AiBusinessObjectDesignVersion version) {
    }
}
