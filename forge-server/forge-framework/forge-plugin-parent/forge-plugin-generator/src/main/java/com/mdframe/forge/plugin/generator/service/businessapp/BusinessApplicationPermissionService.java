package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDataScopeAdapterDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRolePermissionDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.service.ApplicationPermissionAdapter;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePolicyService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPermissionWorkspaceVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationRolePermissionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 聚合应用页面、业务对象与系统角色授权。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationPermissionService {

    private static final String DATA_SCOPE_FOLLOW_SYSTEM = "FOLLOW_SYSTEM";
    private static final String APPLICATION_PAGE_PERMISSION_PREFIX = "ai:business:application:";
    private static final String BUSINESS_APP_ENTRY_PERMISSION_PREFIX = "ai:businessApp:open:";
    private static final Map<String, String> OBJECT_ACTION_NAMES;

    static {
        Map<String, String> actionNames = new LinkedHashMap<>();
        actionNames.put("list", "查看列表");
        actionNames.put("query", "查看详情");
        actionNames.put("add", "新增");
        actionNames.put("edit", "编辑");
        actionNames.put("delete", "删除");
        actionNames.put("export", "导出");
        actionNames.put("import", "导入");
        OBJECT_ACTION_NAMES = Collections.unmodifiableMap(actionNames);
    }

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessAppService businessAppService;
    private final ApplicationPermissionAdapter permissionAdapter;
    private final BusinessObjectDesignerService designerService;
    private final LowcodePolicyService lowcodePolicyService;
    private final ObjectMapper objectMapper;

    public BusinessApplicationPermissionWorkspaceVO workspace(String applicationCode) {
        return buildCatalog(applicationCode).workspace();
    }

    public BusinessApplicationRolePermissionVO rolePermission(String applicationCode, Long roleId) {
        PermissionCatalog catalog = buildCatalog(applicationCode);
        ApplicationPermissionAdapter.RoleGrant grant = permissionAdapter.loadRoleGrant(
                roleId, catalog.resourceIds(), catalog.configurableModuleCodes());
        return toRolePermission(grant, catalog.implicitResourceIds());
    }

    public BusinessApplicationRolePermissionVO saveRolePermission(String applicationCode,
                                                                  Long roleId,
                                                                  BusinessApplicationRolePermissionDTO request) {
        if (request == null) {
            throw new BusinessException("应用角色权限不能为空");
        }
        PermissionCatalog catalog = buildCatalog(applicationCode);
        Set<Long> requestedResourceIds = request.getResourceIds() == null
                ? Set.of() : new LinkedHashSet<>(request.getResourceIds());
        if (!catalog.resourceIds().containsAll(requestedResourceIds)) {
            throw new BusinessException("权限范围包含不属于当前应用的资源");
        }

        Map<String, Integer> requestedModuleScopes = normalizeModuleScopes(request.getModuleScopes());
        if (!catalog.configurableModuleCodes().containsAll(requestedModuleScopes.keySet())) {
            throw new BusinessException("数据权限包含未完成适配或不属于当前应用的对象");
        }

        Set<Long> selectedResourceIds = new LinkedHashSet<>(requestedResourceIds);
        if (catalog.pageResourceIds().stream().anyMatch(selectedResourceIds::contains)
                && catalog.rootResourceId() != null) {
            selectedResourceIds.add(catalog.rootResourceId());
        }
        permissionAdapter.saveRoleGrant(roleId,
                catalog.resourceIds(), selectedResourceIds,
                catalog.moduleCodes(), requestedModuleScopes);
        return rolePermission(applicationCode, roleId);
    }

    public BusinessApplicationPermissionWorkspaceVO.ObjectPermission saveDataScopeAdapter(
            String applicationCode,
            Long objectId,
            BusinessApplicationDataScopeAdapterDTO request) {
        if (objectId == null || request == null) {
            throw new BusinessException("对象数据范围适配参数不能为空");
        }
        BusinessApplicationVO application = applicationService.detailByCode(applicationCode);
        BusinessApplicationObjectVO applicationObject = applicationObjectService.list(application.getId()).stream()
                .filter(item -> objectId.equals(item.getObjectId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("业务对象不属于当前应用"));

        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        LowcodeModelSchema modelSchema = context.getModelSchema();
        if (modelSchema == null) {
            throw new BusinessException("业务对象模型不存在，不能配置数据范围适配");
        }
        LowcodePolicySchema policies = modelSchema.getPolicies() == null
                ? new LowcodePolicySchema() : modelSchema.getPolicies();
        String dataScope = lowcodePolicyService.normalizeDataScope(request.getDataScope());
        if (!LowcodePolicyService.DATA_SCOPE_TENANT.equals(dataScope)
                && !LowcodePolicyService.DATA_SCOPE_FOLLOW_SYSTEM.equals(dataScope)) {
            throw new BusinessException("不支持的数据范围策略：" + dataScope);
        }
        policies.setDataScope(dataScope);
        if (LowcodePolicyService.DATA_SCOPE_FOLLOW_SYSTEM.equals(dataScope)) {
            applyRequiredField(modelSchema, request.getUserField(), "本人字段",
                    policies::setUserField, policies::setUserColumn);
            applyRequiredField(modelSchema, request.getOrgField(), "组织字段",
                    policies::setOrgField, policies::setOrgColumn);
            applyOptionalField(modelSchema, request.getRegionField(), "区划字段",
                    policies::setRegionField, policies::setRegionColumn);
            policies.setFlowRelatedVisible(!Boolean.FALSE.equals(request.getFlowRelatedVisible()));
        } else {
            policies.setFlowRelatedVisible(null);
        }
        modelSchema.setPolicies(lowcodePolicyService.normalizePolicies(modelSchema, policies));
        context.setModelSchema(modelSchema);
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED.getCode());

        applicationObject.setModelSchema(writeModelSchema(modelSchema));
        Set<String> actionPermissions = OBJECT_ACTION_NAMES.keySet().stream()
                .map(action -> objectPermission(applicationObject.getObjectCode(), action))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return toObjectPermission(applicationObject,
                permissionAdapter.findResourcesByPermissions(actionPermissions));
    }

    private PermissionCatalog buildCatalog(String applicationCode) {
        BusinessApplicationVO application = applicationService.detailByCode(applicationCode);
        List<BusinessApplicationObjectVO> objects = applicationObjectService.list(application.getId());
        BusinessAppQueryDTO entryQuery = new BusinessAppQueryDTO();
        entryQuery.setApplicationId(application.getId());
        List<BusinessAppVO> entries = businessAppService.list(entryQuery);
        Map<String, Object> builder = inAppBuilder(application.getOptions());
        List<Map<String, Object>> visibleNodes = maps(builder.get("nodes")).stream()
                .filter(this::systemMenuVisible)
                .filter(node -> StringUtils.isNotBlank(string(node.get("id"))))
                .toList();
        List<Map<String, Object>> pageNodes = visibleNodes.stream()
                .filter(node -> "page".equalsIgnoreCase(string(node.get("type"))))
                .sorted(Comparator.comparingInt(node -> integer(node.get("sort"))))
                .toList();

        String rootPermission = pagePermission(application.getApplicationCode(), "root");
        Set<String> permissionCodes = new LinkedHashSet<>();
        permissionCodes.add(rootPermission);
        entries.stream().map(this::entryPermission).forEach(permissionCodes::add);
        visibleNodes.forEach(node -> permissionCodes.add(pagePermission(
                application.getApplicationCode(), string(node.get("id")))));
        objects.stream().map(BusinessApplicationObjectVO::getObjectCode)
                .filter(StringUtils::isNotBlank)
                .forEach(objectCode -> OBJECT_ACTION_NAMES.keySet().forEach(action ->
                        permissionCodes.add(objectPermission(objectCode, action))));

        Map<String, ApplicationPermissionAdapter.ResourceInfo> resources =
                permissionAdapter.findResourcesByPermissions(permissionCodes);
        BusinessApplicationPermissionWorkspaceVO workspace = new BusinessApplicationPermissionWorkspaceVO();
        workspace.setApplicationId(application.getId());
        workspace.setApplicationCode(application.getApplicationCode());
        workspace.setApplicationName(application.getApplicationName());
        workspace.setPublishVersion(application.getLastPublishVersion());
        workspace.setRoles(permissionAdapter.listAssignableRoles().stream().map(this::toRoleOption).toList());

        Set<Long> resourceIds = new LinkedHashSet<>();
        Set<Long> pageResourceIds = new LinkedHashSet<>();
        Set<Long> implicitResourceIds = new LinkedHashSet<>();
        ApplicationPermissionAdapter.ResourceInfo rootResource = resources.get(rootPermission);
        if (rootResource != null) {
            resourceIds.add(rootResource.resourceId());
            implicitResourceIds.add(rootResource.resourceId());
        }
        for (BusinessAppVO entry : entries) {
            String permissionCode = entryPermission(entry);
            ApplicationPermissionAdapter.ResourceInfo resource = resources.get(permissionCode);
            BusinessApplicationPermissionWorkspaceVO.PagePermission page =
                    new BusinessApplicationPermissionWorkspaceVO.PagePermission();
            page.setPageId("entry:" + entry.getId());
            page.setPageName(StringUtils.defaultIfBlank(entry.getAppName(), entry.getAppCode()));
            page.setPageType("ENTRY");
            page.setSort(entry.getSortOrder());
            page.setPermissionCode(permissionCode);
            page.setResourceId(resource == null ? null : resource.resourceId());
            page.setRegistered(resource != null);
            page.setPendingLabel(Boolean.TRUE.equals(entry.getAdminMenuSyncEnabled()) ? "待发布" : "未同步菜单");
            workspace.getPages().add(page);
            if (resource != null) {
                resourceIds.add(resource.resourceId());
            }
        }
        visibleNodes.stream()
                .filter(node -> "group".equalsIgnoreCase(string(node.get("type"))))
                .map(node -> resources.get(pagePermission(
                        application.getApplicationCode(), string(node.get("id")))))
                .filter(java.util.Objects::nonNull)
                .map(ApplicationPermissionAdapter.ResourceInfo::resourceId)
                .forEach(resourceId -> {
                    resourceIds.add(resourceId);
                    implicitResourceIds.add(resourceId);
                });

        for (Map<String, Object> node : pageNodes) {
            String pageId = string(node.get("id"));
            String permissionCode = pagePermission(application.getApplicationCode(), pageId);
            ApplicationPermissionAdapter.ResourceInfo resource = resources.get(permissionCode);
            BusinessApplicationPermissionWorkspaceVO.PagePermission page =
                    new BusinessApplicationPermissionWorkspaceVO.PagePermission();
            page.setPageId(pageId);
            page.setPageName(StringUtils.defaultIfBlank(string(node.get("title")), "未命名页面"));
            page.setPageType("IN_APP");
            page.setSort(integer(node.get("sort")));
            page.setPermissionCode(permissionCode);
            page.setResourceId(resource == null ? null : resource.resourceId());
            page.setRegistered(resource != null);
            page.setPendingLabel("待发布");
            workspace.getPages().add(page);
            if (resource != null) {
                resourceIds.add(resource.resourceId());
                pageResourceIds.add(resource.resourceId());
            }
        }

        Set<String> moduleCodes = new LinkedHashSet<>();
        Set<String> configurableModuleCodes = new LinkedHashSet<>();
        for (BusinessApplicationObjectVO object : objects) {
            BusinessApplicationPermissionWorkspaceVO.ObjectPermission item = toObjectPermission(object, resources);
            workspace.getObjects().add(item);
            moduleCodes.add(item.getModuleCode());
            if (item.isDataScopeReady()) {
                configurableModuleCodes.add(item.getModuleCode());
            }
            item.getActions().stream().map(BusinessApplicationPermissionWorkspaceVO.ActionPermission::getResourceId)
                    .filter(java.util.Objects::nonNull).forEach(resourceIds::add);
        }
        return new PermissionCatalog(workspace, resourceIds, pageResourceIds, moduleCodes,
                configurableModuleCodes, implicitResourceIds,
                rootResource == null ? null : rootResource.resourceId());
    }

    private BusinessApplicationPermissionWorkspaceVO.ObjectPermission toObjectPermission(
            BusinessApplicationObjectVO object,
            Map<String, ApplicationPermissionAdapter.ResourceInfo> resources) {
        BusinessApplicationPermissionWorkspaceVO.ObjectPermission item =
                new BusinessApplicationPermissionWorkspaceVO.ObjectPermission();
        item.setObjectId(object.getObjectId());
        item.setObjectCode(object.getObjectCode());
        item.setObjectName(StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode()));
        item.setModuleCode(objectModuleCode(object.getObjectCode()));
        BusinessApplicationPermissionWorkspaceVO.DataScopeAdapter adapter =
                toDataScopeAdapter(object.getModelSchema());
        item.setDataScopeMode(adapter.getDataScope());
        item.setDataScopeReady(DATA_SCOPE_FOLLOW_SYSTEM.equals(adapter.getDataScope()));
        item.setSharedApplicationCount(object.getSharedApplicationCount());
        item.setDataScopeAdapter(adapter);
        OBJECT_ACTION_NAMES.forEach((actionCode, actionName) -> {
            String permissionCode = objectPermission(object.getObjectCode(), actionCode);
            ApplicationPermissionAdapter.ResourceInfo resource = resources.get(permissionCode);
            BusinessApplicationPermissionWorkspaceVO.ActionPermission action =
                    new BusinessApplicationPermissionWorkspaceVO.ActionPermission();
            action.setActionCode(actionCode);
            action.setActionName(actionName);
            action.setPermissionCode(permissionCode);
            action.setResourceId(resource == null ? null : resource.resourceId());
            action.setRegistered(resource != null);
            item.getActions().add(action);
        });
        return item;
    }

    private BusinessApplicationPermissionWorkspaceVO.RoleOption toRoleOption(
            ApplicationPermissionAdapter.RoleInfo role) {
        BusinessApplicationPermissionWorkspaceVO.RoleOption option =
                new BusinessApplicationPermissionWorkspaceVO.RoleOption();
        option.setRoleId(role.roleId());
        option.setRoleName(role.roleName());
        option.setRoleKey(role.roleKey());
        option.setRoleType(role.roleType());
        option.setDefaultDataScope(role.defaultDataScope());
        return option;
    }

    private BusinessApplicationRolePermissionVO toRolePermission(ApplicationPermissionAdapter.RoleGrant grant,
                                                                 Set<Long> implicitResourceIds) {
        BusinessApplicationRolePermissionVO result = new BusinessApplicationRolePermissionVO();
        result.setRoleId(grant.role().roleId());
        result.setRoleName(grant.role().roleName());
        result.setRoleKey(grant.role().roleKey());
        result.setDefaultDataScope(grant.role().defaultDataScope());
        Set<Long> editableResourceIds = new LinkedHashSet<>(grant.resourceIds());
        editableResourceIds.removeAll(implicitResourceIds);
        result.setResourceIds(editableResourceIds);
        result.setModuleScopes(new LinkedHashMap<>(grant.moduleScopes()));
        return result;
    }

    private BusinessApplicationPermissionWorkspaceVO.DataScopeAdapter toDataScopeAdapter(String modelSchemaJson) {
        LowcodeModelSchema modelSchema = readModelSchema(modelSchemaJson);
        LowcodePolicySchema policies = lowcodePolicyService.normalizeModelSchema(modelSchema);
        BusinessApplicationPermissionWorkspaceVO.DataScopeAdapter adapter =
                new BusinessApplicationPermissionWorkspaceVO.DataScopeAdapter();
        adapter.setDataScope(policies.getDataScope());
        adapter.setUserField(policies.getUserField());
        adapter.setOrgField(policies.getOrgField());
        adapter.setRegionField(policies.getRegionField());
        adapter.setFlowRelatedVisible(lowcodePolicyService.isFollowSystem(policies)
                && lowcodePolicyService.isFlowRelatedVisible(policies));
        if (modelSchema.getFields() != null) {
            modelSchema.getFields().stream()
                    .filter(java.util.Objects::nonNull)
                    .filter(field -> StringUtils.isNotBlank(field.getField()))
                    .filter(field -> !"DISABLED".equalsIgnoreCase(field.getFieldStatus()))
                    .map(this::toFieldOption)
                    .forEach(adapter.getFields()::add);
        }
        return adapter;
    }

    private BusinessApplicationPermissionWorkspaceVO.FieldOption toFieldOption(LowcodeFieldSchema field) {
        BusinessApplicationPermissionWorkspaceVO.FieldOption option =
                new BusinessApplicationPermissionWorkspaceVO.FieldOption();
        option.setField(field.getField());
        option.setColumnName(field.getColumnName());
        option.setLabel(StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
        return option;
    }

    private LowcodeModelSchema readModelSchema(String modelSchemaJson) {
        if (StringUtils.isBlank(modelSchemaJson)) {
            return new LowcodeModelSchema();
        }
        try {
            return objectMapper.readValue(modelSchemaJson, LowcodeModelSchema.class);
        } catch (Exception ignored) {
            return new LowcodeModelSchema();
        }
    }

    private String writeModelSchema(LowcodeModelSchema modelSchema) {
        try {
            return objectMapper.writeValueAsString(modelSchema);
        } catch (Exception exception) {
            throw new BusinessException("对象数据范围适配保存失败");
        }
    }

    private void applyRequiredField(LowcodeModelSchema modelSchema,
                                    String fieldName,
                                    String label,
                                    java.util.function.Consumer<String> fieldSetter,
                                    java.util.function.Consumer<String> columnSetter) {
        LowcodeFieldSchema field = resolveAdapterField(modelSchema, fieldName, label, true);
        fieldSetter.accept(field.getField());
        columnSetter.accept(field.getColumnName());
    }

    private void applyOptionalField(LowcodeModelSchema modelSchema,
                                    String fieldName,
                                    String label,
                                    java.util.function.Consumer<String> fieldSetter,
                                    java.util.function.Consumer<String> columnSetter) {
        LowcodeFieldSchema field = resolveAdapterField(modelSchema, fieldName, label, false);
        fieldSetter.accept(field == null ? null : field.getField());
        columnSetter.accept(field == null ? null : field.getColumnName());
    }

    private LowcodeFieldSchema resolveAdapterField(LowcodeModelSchema modelSchema,
                                                   String fieldName,
                                                   String label,
                                                   boolean required) {
        String normalized = StringUtils.trimToNull(fieldName);
        if (normalized == null) {
            if (required) {
                throw new BusinessException("跟随系统数据权限时必须配置" + label);
            }
            return null;
        }
        LowcodeFieldSchema field = modelSchema.getFields() == null ? null : modelSchema.getFields().stream()
                .filter(java.util.Objects::nonNull)
                .filter(item -> normalized.equals(item.getField()) || normalized.equals(item.getColumnName()))
                .findFirst()
                .orElse(null);
        if (field == null || "DISABLED".equalsIgnoreCase(field.getFieldStatus())) {
            throw new BusinessException(label + "不存在或已停用：" + normalized);
        }
        return field;
    }

    private Map<String, Integer> normalizeModuleScopes(
            List<BusinessApplicationRolePermissionDTO.ModuleScope> scopes) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (scopes == null) {
            return result;
        }
        for (BusinessApplicationRolePermissionDTO.ModuleScope scope : scopes) {
            if (scope == null || StringUtils.isBlank(scope.getModuleCode())) {
                throw new BusinessException("对象数据权限模块编码不能为空");
            }
            String moduleCode = scope.getModuleCode().trim();
            if (result.containsKey(moduleCode)) {
                throw new BusinessException("对象数据权限重复配置: " + moduleCode);
            }
            if (scope.getDataScope() != null) {
                result.put(moduleCode, scope.getDataScope());
            }
        }
        return result;
    }

    private Map<String, Object> inAppBuilder(String optionsJson) {
        if (StringUtils.isBlank(optionsJson)) {
            return Map.of();
        }
        try {
            Map<String, Object> options = objectMapper.readValue(optionsJson, new TypeReference<>() { });
            return map(options.get("inAppBuilder"));
        } catch (Exception exception) {
            throw new BusinessException("应用页面配置格式不正确");
        }
    }

    private String pagePermission(String applicationCode, String pageId) {
        return APPLICATION_PAGE_PERMISSION_PREFIX + applicationCode + ":page:" + pageId;
    }

    private String entryPermission(BusinessAppVO entry) {
        String entryCode = StringUtils.defaultIfBlank(entry.getAppCode(), String.valueOf(entry.getId()));
        return BUSINESS_APP_ENTRY_PERMISSION_PREFIX + StringUtils.lowerCase(entryCode);
    }

    private String objectPermission(String objectCode, String actionCode) {
        return objectModuleCode(objectCode) + ":" + actionCode;
    }

    private String objectModuleCode(String objectCode) {
        return "ai:business:" + objectCode;
    }

    private boolean systemMenuVisible(Map<String, Object> node) {
        Object value = node.get("systemMenuVisible");
        if (value == null) {
            value = map(node.get("settings")).get("systemMenuVisible");
        }
        return Boolean.TRUE.equals(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw
                ? new LinkedHashMap<>((Map<String, Object>) raw) : Map.of();
    }

    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        collection.stream().filter(Map.class::isInstance).map(this::map).forEach(result::add);
        return result;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int integer(Object value) {
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record PermissionCatalog(BusinessApplicationPermissionWorkspaceVO workspace,
                                     Set<Long> resourceIds,
                                     Set<Long> pageResourceIds,
                                     Set<String> moduleCodes,
                                     Set<String> configurableModuleCodes,
                                     Set<Long> implicitResourceIds,
                                     Long rootResourceId) {
    }
}
