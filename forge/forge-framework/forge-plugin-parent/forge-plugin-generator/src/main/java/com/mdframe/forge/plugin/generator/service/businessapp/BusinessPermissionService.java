package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessPermissionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 业务对象权限集成服务。
 * <p>
 * 提供业务对象的权限状态查询、权限码生成建议等便捷功能。
 * 实际权限拦截由 DynamicDataScopeService + DataScopeInterceptor 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPermissionService {

    private final IDataScopeService dataScopeService;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessPermissionMapper permissionMapper;

    /**
     * 查询当前用户对指定业务对象的权限概览
     */
    public Map<String, Object> getPermissionOverview(String objectCode) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 当前用户的数据权限范围
        DataScopeContext context = dataScopeService.getCurrentUserDataScope();
        if (context != null) {
            DataScopeType scopeType = DataScopeType.getByRoleDataScope(
                    context.getMinDataScope(),
                    context.getCustomOrgIds() != null && !context.getCustomOrgIds().isEmpty());
            result.put("dataScopeType", scopeType != null ? scopeType.name() : "UNKNOWN");
            result.put("dataScopeLabel", scopeType != null ? scopeType.getDescription() : "未知");
        } else {
            result.put("dataScopeType", "NONE");
            result.put("dataScopeLabel", "未登录");
        }

        // 2. 权限绑定配置状态
        Long tenantId = SessionHelper.getTenantId();
        AiBusinessBinding permBinding = bindingMapper.selectBindingByTypeAndCode(
                tenantId, "OBJECT", objectCode, "PERMISSION");
        result.put("hasPermissionBinding", permBinding != null);
        if (permBinding != null) {
            result.put("permissionBindingConfig", permBinding.getBindingConfig());
            result.put("permissionBindingStatus", permBinding.getStatus());
        }

        // 3. 生成建议的权限码
        result.put("suggestedPermissions", buildSuggestedPermissions(objectCode));

        return result;
    }

    /**
     * 解析当前用户可执行的单据动作。
     */
    public List<String> resolveAvailableActions(String objectCode,
                                                Long recordId,
                                                Map<String, Object> recordData) {
        List<String> actions = new ArrayList<>();
        if (objectCode == null || objectCode.isBlank() || recordId == null || recordData == null) {
            return actions;
        }
        if (hasDocumentActionPermission(objectCode, "VIEW")) {
            actions.add("VIEW");
        }
        if (hasDocumentActionPermission(objectCode, "SAVE")) {
            actions.add("SAVE");
        }
        if (hasDocumentActionPermission(objectCode, "SUBMIT")) {
            actions.add("SUBMIT");
        }
        if (hasDocumentActionPermission(objectCode, "DELETE")) {
            actions.add("DELETE");
        }
        if (hasDocumentActionPermission(objectCode, "START_FLOW")) {
            actions.add("START_FLOW");
        }
        if (hasDocumentActionPermission(objectCode, "VIEW_FLOW")) {
            actions.add("VIEW_FLOW");
        }
        if (hasDocumentActionPermission(objectCode, "WITHDRAW")) {
            actions.add("WITHDRAW");
        }
        if (hasDocumentActionPermission(objectCode, "TRIGGER")) {
            actions.add("TRIGGER");
        }
        if (hasDocumentActionPermission(objectCode, "VIEW_STATS")) {
            actions.add("VIEW_STATS");
        }
        return actions;
    }

    public boolean hasDocumentActionPermission(String objectCode, String actionCode) {
        if (objectCode == null || objectCode.isBlank() || actionCode == null || actionCode.isBlank()) {
            return false;
        }
        ActionPermissionDefinition definition = actionDefinition(objectCode, actionCode);
        return definition != null && hasAnyPermission(definition.permissionCodes().toArray(String[]::new));
    }

    public BusinessPermissionSummaryVO documentActionSummary(Long objectId) {
        AiBusinessObject object = businessObjectMapper.selectById(objectId);
        if (object == null) {
            throw new BusinessException("业务对象不存在");
        }
        List<ActionPermissionDefinition> definitions = actionDefinitions(object.getObjectCode());
        List<String> allPermissions = definitions.stream()
                .flatMap(definition -> definition.permissionCodes().stream())
                .distinct()
                .toList();
        Set<String> existingPermissions = allPermissions.isEmpty()
                ? Set.of()
                : new HashSet<>(permissionMapper.selectExistingPermissions(resolveTenantId(), allPermissions));

        BusinessPermissionSummaryVO summary = new BusinessPermissionSummaryVO();
        summary.setObjectId(object.getId());
        summary.setObjectCode(object.getObjectCode());
        summary.setObjectName(object.getObjectName());
        boolean allRequiredConfigured = true;
        for (ActionPermissionDefinition definition : definitions) {
            BusinessPermissionSummaryVO.ActionPermissionVO item = new BusinessPermissionSummaryVO.ActionPermissionVO();
            item.setActionCode(definition.actionCode());
            item.setActionName(definition.actionName());
            item.setPermissionCodes(definition.permissionCodes());
            item.setRequired(definition.required());
            boolean configured = definition.permissionCodes().stream().anyMatch(existingPermissions::contains);
            item.setConfigured(configured);
            item.setGranted(hasAnyPermission(definition.permissionCodes().toArray(String[]::new)));
            summary.getActionPermissions().add(item);
            if (definition.required() && !configured) {
                allRequiredConfigured = false;
            }
        }
        summary.setAllRequiredConfigured(allRequiredConfigured);
        return summary;
    }

    /**
     * 为业务对象生成建议的权限码列表
     */
    private List<Map<String, String>> buildSuggestedPermissions(String objectCode) {
        String prefix = "ai:business:" + objectCode;
        List<Map<String, String>> permissions = new ArrayList<>();
        permissions.add(Map.of("code", prefix + ":list", "name", "查看列表"));
        permissions.add(Map.of("code", prefix + ":query", "name", "查看详情"));
        permissions.add(Map.of("code", prefix + ":add", "name", "新增"));
        permissions.add(Map.of("code", prefix + ":edit", "name", "编辑"));
        permissions.add(Map.of("code", prefix + ":delete", "name", "删除"));
        permissions.add(Map.of("code", prefix + ":export", "name", "导出"));
        permissions.add(Map.of("code", prefix + ":import", "name", "导入"));
        permissions.add(Map.of("code", "ai:businessDocument:save", "name", "保存单据"));
        permissions.add(Map.of("code", "ai:businessDocument:submit", "name", "提交单据"));
        permissions.add(Map.of("code", "ai:businessDocument:withdraw", "name", "撤回单据"));
        permissions.add(Map.of("code", "ai:businessFlow:start", "name", "发起主流程"));
        permissions.add(Map.of("code", "ai:businessFlow:view", "name", "查看流程"));
        permissions.add(Map.of("code", "ai:businessFlow:callback", "name", "流程回调"));
        permissions.add(Map.of("code", "ai:businessDocument:view", "name", "查看单据状态"));
        permissions.add(Map.of("code", "ai:businessTrigger:execute", "name", "执行触发器"));
        permissions.add(Map.of("code", "ai:businessStats:view", "name", "查看报表"));
        return permissions;
    }

    private List<ActionPermissionDefinition> actionDefinitions(String objectCode) {
        String prefix = "ai:business:" + objectCode;
        return List.of(
                new ActionPermissionDefinition("VIEW", "查看", List.of("ai:businessDocument:view", prefix + ":query", prefix + ":list"), true),
                new ActionPermissionDefinition("SAVE", "保存", List.of("ai:businessDocument:save", prefix + ":add", prefix + ":edit"), true),
                new ActionPermissionDefinition("DELETE", "删除", List.of(prefix + ":delete"), false),
                new ActionPermissionDefinition("SUBMIT", "提交", List.of("ai:businessDocument:submit", prefix + ":edit"), true),
                new ActionPermissionDefinition("WITHDRAW", "撤回", List.of("ai:businessDocument:withdraw", prefix + ":edit"), false),
                new ActionPermissionDefinition("START_FLOW", "发起主流程", List.of("ai:businessFlow:start"), true),
                new ActionPermissionDefinition("VIEW_FLOW", "查看流程", List.of("ai:businessFlow:view"), true),
                new ActionPermissionDefinition("TRIGGER", "执行触发器", List.of("ai:businessTrigger:execute"), false),
                new ActionPermissionDefinition("VIEW_STATS", "查看报表", List.of("ai:businessStats:view"), false)
        );
    }

    private ActionPermissionDefinition actionDefinition(String objectCode, String actionCode) {
        return actionDefinitions(objectCode).stream()
                .filter(definition -> definition.actionCode().equalsIgnoreCase(actionCode))
                .findFirst()
                .orElse(null);
    }

    private boolean hasAnyPermission(String... permissions) {
        try {
            return SessionHelper.hasAnyPermission(permissions);
        } catch (Exception e) {
            return false;
        }
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId == null ? 1L : tenantId;
    }

    private record ActionPermissionDefinition(String actionCode,
                                              String actionName,
                                              List<String> permissionCodes,
                                              boolean required) {
    }
}
