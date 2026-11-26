package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessPublishCheckLevel;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignVersionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectPublishDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageModelRef;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePublishDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerMapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePublishService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.formula.CrossObjectRecomputeTaskService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaObjectDependencyAnalyzer;
import com.mdframe.forge.plugin.generator.service.formula.FormulaPublishValidator;
import com.mdframe.forge.plugin.generator.service.formula.FormulaValidationResult;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignVersionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckItemVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务对象发布检查和发布门面服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectPublishService {

    private static final String FORM_DESIGNER_SCHEMA_OPTION_KEY = "formDesignerSchema";
    private static final String DESIGNER_ACTIONS_KEY = "actions";
    private static final BusinessObjectFormPublishValidator FORM_PUBLISH_VALIDATOR =
            new BusinessObjectFormPublishValidator();

    private final BusinessObjectDesignerService designerService;
    private final BusinessObjectDesignVersionService designVersionService;
    private final LowcodePublishService lowcodePublishService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final LowcodeSchemaValidator schemaValidator;
    private final FormulaPublishValidator formulaPublishValidator;
    private final CrossObjectRecomputeTaskService crossObjectRecomputeTaskService;
    private final LowcodeDdlService ddlService;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessAppMapper businessAppMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessTriggerMapper triggerMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessPermissionService permissionService;
    private final MenuRegisterAdapter menuRegisterAdapter;
    private final ObjectMapper objectMapper;

    public BusinessPublishCheckVO publishCheck(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        return publishCheck(context, null);
    }

    public BusinessPublishCheckVO publishCheck(Long objectId, BusinessPermissionSummaryVO permissionSummary) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        return publishCheck(context, permissionSummary);
    }

    /**
     * 应用协调发布预检只加载设计上下文，不做对象级页面/公式/单据全量校验。
     * 上下文供后续 OBJECTS 步骤复用，避免同一请求内二次 loadContext。
     */
    public BusinessObjectDesignerService.DesignerContext loadContextForApplicationPublish(Long objectId) {
        return designerService.loadContext(objectId);
    }

    /**
     * 应用协调发布在已有对象发布版本时只收敛设计状态，不重跑 lowcode 发布。
     * 已是 PUBLISHED 的对象直接跳过，避免逐条 selectById。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markDesignPublished(List<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return;
        }
        List<Long> ids = objectIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        businessObjectMapper.markDesignPublished(resolveTenantId(null), ids,
                BusinessObjectDesignStatus.PUBLISHED.getCode());
    }

    /**
     * 执行发布检查并返回加载的设计上下文，供对象级发布预检使用。
     * 应用协调发布请用 {@link #loadContextForApplicationPublish(Long)}，不要走本方法的全量校验。
     */
    public ResolvedObjectCheck publishCheckResolved(Long objectId, BusinessPermissionSummaryVO permissionSummary) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        return new ResolvedObjectCheck(publishCheck(context, permissionSummary), context);
    }

    /** 发布检查结果与其加载的设计上下文。 */
    public record ResolvedObjectCheck(BusinessPublishCheckVO check,
                                      BusinessObjectDesignerService.DesignerContext context) {
    }

    private BusinessPublishCheckVO publishCheck(BusinessObjectDesignerService.DesignerContext context,
                                                BusinessPermissionSummaryVO permissionSummary) {
        designerService.compileFormFirstRuntimeSchema(context);
        designerService.applyRelationsToModel(context);
        List<BusinessPublishCheckItemVO> items = new ArrayList<>();
        checkFields(context.getModelSchema(), items);
        pagePublishValidator().validate(context, items);
        checkFormFirstSchemas(context, items);
        designPublishValidator().validateSchema(context, items);
        deploymentPublishValidator().validateEntry(context, items);
        designPublishValidator().validateGovernance(context, permissionSummary, items);
        deploymentPublishValidator().validateStorage(context.getModelSchema(), items);
        checkFormula(context, items);
        return buildResult(items);
    }

    /** 应用协调发布预检已通过且子表关系未变时，复用预检结论，避免二次全量编译校验。 */
    private BusinessPublishCheckVO trustPreloadedPublishCheck() {
        BusinessPublishCheckVO check = new BusinessPublishCheckVO();
        check.setPublishable(true);
        check.setItems(List.of());
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long objectId, BusinessObjectPublishDTO dto) {
        return publish(objectId, dto, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long objectId,
                        BusinessObjectPublishDTO dto,
                        BusinessPermissionSummaryVO permissionSummary) {
        return publish(objectId, dto, permissionSummary, null);
    }

    /**
     * 发布业务对象。preloadedContext 非空时复用同一请求内预检阶段加载的设计上下文，
     * 避免重复执行 loadContext 的整组查询；仅限加载后无外部变更的编排链路（应用协调发布）传入。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long objectId,
                        BusinessObjectPublishDTO dto,
                        BusinessPermissionSummaryVO permissionSummary,
                        BusinessObjectDesignerService.DesignerContext preloadedContext) {
        BusinessObjectDesignerService.DesignerContext context = preloadedContext != null
                ? preloadedContext
                : designerService.loadContext(objectId);
        if (dto != null && dto.getModelSchema() != null) {
            context.setModelSchema(dto.getModelSchema());
        }
        if (dto != null && dto.getPageSchema() != null) {
            context.setPageSchema(dto.getPageSchema());
        }
        // DETAIL 在 prepare 阶段 persistChildRelations=false，必须在最终发布前再同步子表关系；
        // PRIMARY 已在 prepare 同步过时通常无写入（短回路），代价可接受。
        boolean relationsChanged = designerService.synchronizeFormChildRelations(context);
        designerService.applyRelationsToModel(context);
        context = designerService.saveDraft(context, BusinessObjectDesignStatus.READY.getCode());
        // 应用协调发布已在 readiness 做过 publishCheckResolved；关系未变则跳过二次编译校验。
        BusinessPublishCheckVO check = (preloadedContext != null && !relationsChanged)
                ? trustPreloadedPublishCheck()
                : publishCheck(context, permissionSummary);
        boolean force = dto != null && Boolean.TRUE.equals(dto.getForce());
        if (Boolean.FALSE.equals(check.getPublishable())
                && (!force || containsNonForceableCommandBlock(check))) {
            throw new BusinessException(buildPublishBlockedMessage(check));
        }

        LowcodePublishDTO publishDTO = buildPublishDTO(context, dto);
        Long crudConfigVersionId = lowcodePublishService.publish(context.getConfig().getId(), publishDTO);
        AiCrudConfig publishedConfig = crudConfigMapper.selectById(context.getConfig().getId());
        if (publishedConfig == null) {
            throw new BusinessException("发布后运行配置不存在");
        }
        AiBusinessObject object = businessObjectMapper.selectById(objectId);
        if (object == null) {
            throw new BusinessException("发布后业务对象不存在");
        }
        object.setConfigKey(publishedConfig.getConfigKey());
        object.setModelCode(publishedConfig.getObjectCode());
        object.setDesignStatus(BusinessObjectDesignStatus.PUBLISHED.getCode());
        object.setLastPublishVersion(publishedConfig.getPublishedVersion());
        object.setLastPublishTime(publishedConfig.getPublishTime());
        if (businessObjectMapper.updateById(object) == 0) {
            throw new BusinessException("业务对象发布状态更新失败");
        }

        BusinessObjectDesignVersionDTO versionDTO = new BusinessObjectDesignVersionDTO();
        versionDTO.setObjectId(objectId);
        versionDTO.setSuiteCode(object.getSuiteCode());
        versionDTO.setObjectCode(object.getObjectCode());
        versionDTO.setConfigId(publishedConfig.getId());
        versionDTO.setConfigKey(publishedConfig.getConfigKey());
        versionDTO.setCrudConfigVersionId(crudConfigVersionId);
        versionDTO.setVersionType("publish");
        versionDTO.setModelSnapshot(context.getModelSchema());
        versionDTO.setPageSnapshot(context.getPageSchema());
        versionDTO.setRelationSnapshot(context.getRelations());
        versionDTO.setDesignerOptionsSnapshot(readDesignerOptions(context));
        versionDTO.setPublishStatus("PUBLISHED");
        versionDTO.setPublishVersion(publishedConfig.getPublishedVersion());
        versionDTO.setRemark(dto == null ? null : dto.getRemark());
        Long versionId = designVersionService.createVersion(versionDTO);
        menuRegisterAdapter.syncBusinessObjectActionPermissions(
                object.getObjectCode(), object.getObjectName(), publishedConfig.getConfigKey());
        enqueueCrossObjectRecompute(context);
        return versionId;
    }

    private String buildPublishBlockedMessage(BusinessPublishCheckVO check) {
        List<BusinessPublishCheckItemVO> blockItems = check == null || check.getBlockItems() == null
                ? List.of()
                : check.getBlockItems();
        if (blockItems.isEmpty()) {
            return "发布检查存在阻断项，请先修复后再发布";
        }
        String summary = blockItems.stream()
                .limit(3)
                .map(this::formatBlockItem)
                .filter(StringUtils::isNotBlank)
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
        int remain = Math.max(blockItems.size() - 3, 0);
        String suffix = remain > 0 ? "；另有 " + remain + " 项" : "";
        return "发布检查存在 " + blockItems.size() + " 个阻断项：" + summary + suffix;
    }

    private boolean containsNonForceableCommandBlock(BusinessPublishCheckVO check) {
        return check != null && check.getBlockItems() != null
                && check.getBlockItems().stream().anyMatch(item -> "COMMAND".equals(item.getCategory()));
    }

    private String formatBlockItem(BusinessPublishCheckItemVO item) {
        if (item == null) {
            return "";
        }
        String title = StringUtils.defaultIfBlank(item.getTitle(), item.getItemCode());
        String target = StringUtils.defaultIfBlank(item.getZoneKey(), item.getFieldCode());
        if (StringUtils.isNotBlank(target)) {
            return title + "（" + target + "）";
        }
        return title;
    }

    @Transactional(rollbackFor = Exception.class)
    public void rollback(Long objectId, Long versionId) {
        rollbackInternal(objectId, versionId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long rollbackForApplication(Long objectId, Long versionId) {
        return rollbackInternal(objectId, versionId, false);
    }

    private Long rollbackInternal(Long objectId, Long versionId, boolean syncMenu) {
        BusinessObjectDesignVersionVO version = designVersionService.detail(objectId, versionId);
        if (version.getConfigId() != null && version.getCrudConfigVersionId() != null) {
            lowcodePublishService.rollback(version.getConfigId(), version.getCrudConfigVersionId(), syncMenu);
        }
        designerService.rollbackDesignVersion(objectId, versionId);
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        BusinessObjectDesignVersionDTO rollbackVersion = new BusinessObjectDesignVersionDTO();
        rollbackVersion.setObjectId(objectId);
        rollbackVersion.setSuiteCode(context.getObject().getSuiteCode());
        rollbackVersion.setObjectCode(context.getObject().getObjectCode());
        rollbackVersion.setConfigId(context.getConfig() == null ? null : context.getConfig().getId());
        rollbackVersion.setConfigKey(context.getConfig() == null ? null : context.getConfig().getConfigKey());
        rollbackVersion.setVersionType("rollback");
        rollbackVersion.setModelSnapshot(context.getModelSchema());
        rollbackVersion.setPageSnapshot(context.getPageSchema());
        rollbackVersion.setRelationSnapshot(context.getRelations());
        rollbackVersion.setDesignerOptionsSnapshot(readDesignerOptions(context));
        rollbackVersion.setPublishStatus(context.getConfig() == null ? "DRAFT" : context.getConfig().getPublishStatus());
        rollbackVersion.setPublishVersion(context.getConfig() == null ? null : context.getConfig().getPublishedVersion());
        rollbackVersion.setRemark("回滚到设计版本 " + version.getVersionNo());
        Long rollbackVersionId = designVersionService.createVersion(rollbackVersion);
        AiBusinessObject object = businessObjectMapper.selectById(objectId);
        if (object == null) {
            throw new BusinessException("业务对象不存在");
        }
        object.setDesignStatus(BusinessObjectDesignStatus.PUBLISHED.getCode());
        if (context.getConfig() != null) {
            object.setLastPublishVersion(context.getConfig().getPublishedVersion());
            object.setLastPublishTime(context.getConfig().getPublishTime());
        }
        businessObjectMapper.updateById(object);
        return rollbackVersionId;
    }

    private LowcodePublishDTO buildPublishDTO(BusinessObjectDesignerService.DesignerContext context,
                                              BusinessObjectPublishDTO dto) {
        LowcodePublishDTO publishDTO = new LowcodePublishDTO();
        boolean syncTable = dto != null && Boolean.TRUE.equals(dto.getSyncTable());
        publishDTO.setDeployMode(syncTable ? "ONLINE_CREATE_TABLE" : "SKIP_DDL");
        publishDTO.setConfirmOnlineDdl(syncTable);
        publishDTO.setMenuName(context.getObject().getObjectName());
        publishDTO.setMenuSort(context.getConfig() == null ? context.getObject().getSortOrder() : context.getConfig().getMenuSort());
        publishDTO.setSyncMenu(dto == null ? null : dto.getSyncMenu());
        publishDTO.setBusinessSuiteCode(context.getObject().getSuiteCode());
        publishDTO.setBusinessObjectCode(context.getObject().getObjectCode());
        publishDTO.setBusinessObjectName(context.getObject().getObjectName());
        publishDTO.setObjectCode(context.getObject().getObjectCode());
        publishDTO.setObjectName(context.getObject().getObjectName());
        publishDTO.setRemark(dto == null ? null : dto.getRemark());
        publishDTO.setModelSchema(context.getModelSchema());
        publishDTO.setPageSchema(buildPublishPageSchema(context));
        return publishDTO;
    }

    private LowcodePageSchema buildPublishPageSchema(BusinessObjectDesignerService.DesignerContext context) {
        LowcodePageSchema pageSchema = context.getPageSchema();
        List<Map<String, Object>> runtimeActions = buildRuntimeCustomActions(context, readDesignerOptions(context));
        if (pageSchema == null || runtimeActions.isEmpty()) {
            return pageSchema;
        }
        LowcodePageSchema next = objectMapper.convertValue(pageSchema, LowcodePageSchema.class);
        List<Map<String, Object>> pageActions = runtimeActions.stream()
                .filter(action -> !"childRow".equals(action.get("position")))
                .toList();
        if (!pageActions.isEmpty()) {
            LowcodePageZone tableZone = findOrCreateZone(next, "table");
            Map<String, Object> props = tableZone.getProps() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(tableZone.getProps());
            props.put("customActions", pageActions);
            tableZone.setProps(props);
        }
        applyChildRowActions(next, runtimeActions.stream()
                .filter(action -> "childRow".equals(action.get("position")))
                .toList());
        return next;
    }

    private void applyChildRowActions(LowcodePageSchema pageSchema, List<Map<String, Object>> childActions) {
        if (pageSchema == null || pageSchema.getModelRefs() == null || childActions.isEmpty()) {
            return;
        }
        for (LowcodePageModelRef ref : pageSchema.getModelRefs()) {
            if (ref == null || Boolean.TRUE.equals(ref.getPrimary()) || StringUtils.isBlank(ref.getModelCode())) {
                continue;
            }
            Map<String, Object> props = ref.getProps() == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(ref.getProps());
            String relationKey = StringUtils.defaultIfBlank(text(props.get("relationKey")), ref.getModelCode());
            List<Map<String, Object>> matched = childActions.stream()
                    .filter(action -> relationKey.equals(text(action.get("relationKey"))))
                    .<Map<String, Object>>map(action -> new LinkedHashMap<>(action))
                    .toList();
            if (!matched.isEmpty()) {
                props.put("relationKey", relationKey);
                props.put("rowActions", matched);
                ref.setProps(props);
            }
        }
    }

    private LowcodePageZone findOrCreateZone(LowcodePageSchema pageSchema, String zoneKey) {
        if (pageSchema.getZones() == null) {
            pageSchema.setZones(new ArrayList<>());
        }
        for (LowcodePageZone zone : pageSchema.getZones()) {
            if (zone != null && zoneKey.equalsIgnoreCase(zone.getZoneKey())) {
                return zone;
            }
        }
        LowcodePageZone zone = new LowcodePageZone();
        zone.setZoneKey(zoneKey);
        zone.setComponentKey(zoneKey);
        zone.setEnabled(true);
        pageSchema.getZones().add(zone);
        return zone;
    }

    private List<Map<String, Object>> buildRuntimeCustomActions(BusinessObjectDesignerService.DesignerContext context,
                                                                Map<String, Object> designerOptions) {
        List<Map<String, Object>> actions = listOfMap(designerOptions.get(DESIGNER_ACTIONS_KEY));
        if (actions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            if (!Integer.valueOf(1).equals(intValue(action.get("status"), 1))) {
                continue;
            }
            String actionCode = text(action.get("actionCode"));
            String actionName = text(action.get("actionName"));
            String actionType = StringUtils.defaultIfBlank(text(action.get("actionType")), "OPEN_PAGE").toUpperCase();
            String position = normalizeActionPosition(action.get("actionPosition"));
            Map<String, Object> config = mapValue(action.get("actionConfig"));
            if (isBusinessAutomationAction(actionType, config) && !"childRow".equals(position)) {
                continue;
            }
            if (isBuiltinCrudAction(position, actionCode, actionName, actionType, config)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", StringUtils.defaultIfBlank(actionCode, "custom_" + result.size()));
            item.put("actionCode", StringUtils.defaultIfBlank(actionCode, "custom_" + result.size()));
            item.put("label", StringUtils.defaultIfBlank(actionName, "自定义操作"));
            item.put("position", position);
            item.put("type", resolveActionButtonType(actionType));
            item.put("actionType", resolveRuntimeActionType(actionType, config));
            item.put("visible", !Boolean.FALSE.equals(booleanValue(action.get("visible"))));
            putIfNotBlank(item, "relationKey", text(config.get("relationKey")));
            AiBusinessObject object = context == null ? null : context.getObject();
            if (object != null) {
                putIfNotBlank(item, "suiteCode", object.getSuiteCode());
                putIfNotBlank(item, "objectCode", object.getObjectCode());
                putIfNotBlank(item, "businessObjectCode", object.getObjectCode());
                putIfNotBlank(item, "targetObjectCode", object.getObjectCode());
            }
            putIfNotBlank(item, "routePath", resolveActionRoutePath(actionType, config));
            // 应用内页面依赖 targetPageKey；不能只保留 routePath，否则发布后按钮丢目标或无法跳转
            putIfNotBlank(item, "targetPageKey", text(config.get("targetPageKey")));
            putIfNotBlank(item, "targetFormKey", text(config.get("targetFormKey")));
            putIfNotBlank(item, "openTarget", StringUtils.defaultIfBlank(text(config.get("openTarget")), "_self"));
            String permissionKey = StringUtils.firstNonBlank(
                    text(action.get("permissionKey")),
                    text(action.get("permissionCode")),
                    text(action.get("permission")));
            putIfNotBlank(item, "permissionKey", permissionKey);
            putIfNotBlank(item, "permissionCode", permissionKey);
            putIfNotBlank(item, "permissionStrategy", text(action.get("permissionStrategy")));
            putIfNotBlank(item, "displayCondition", StringUtils.firstNonBlank(
                    text(action.get("displayCondition")), text(config.get("displayCondition"))));
            putIfNotBlank(item, "successMessage", text(action.get("successMessage")));
            putIfNotBlank(item, "failureMessage", text(action.get("failureMessage")));
            putIfNotBlank(item, "successBehavior", text(config.get("successBehavior")));
            List<Map<String, Object>> params = listOfMap(config.get("params"));
            if (!params.isEmpty()) {
                item.put("params", params);
            }
            if (("CALL_API".equals(actionType) || "COMMAND".equals(actionType) || "OPEN_PAGE".equals(actionType))
                    && !config.isEmpty()) {
                item.put("actionConfig", new LinkedHashMap<>(config));
            }
            if (Boolean.TRUE.equals(booleanValue(action.get("confirmRequired")))) {
                item.put("confirmText", "确认执行“" + StringUtils.defaultIfBlank(actionName, "该操作") + "”？");
            }
            result.add(item);
        }
        return result;
    }

    private boolean isBusinessAutomationAction(String actionType, Map<String, Object> config) {
        if ("COMMAND".equals(actionType)) {
            return hasActionSteps(config);
        }
        return false;
    }

    private boolean hasActionSteps(Map<String, Object> config) {
        return config != null && (config.get("steps") instanceof List<?> || config.get("stepList") instanceof List<?>);
    }

    private boolean isBuiltinCrudAction(String position, String actionCode, String actionName,
                                        String actionType, Map<String, Object> config) {
        if (!"OPEN_PAGE".equals(actionType)
                || StringUtils.isNotBlank(resolveActionRoutePath(actionType, config))
                || StringUtils.isNotBlank(text(config == null ? null : config.get("targetPageKey")))) {
            return false;
        }
        if ("toolbar".equals(position)) {
            return matchesActionIdentity(actionCode, actionName, Set.of("add", "create", "new", "新增", "新建"));
        }
        if ("row".equals(position)) {
            return matchesActionIdentity(actionCode, actionName, Set.of("edit", "detail", "delete", "addchild",
                    "编辑", "查看详情", "详情", "删除", "添加下级"));
        }
        return false;
    }

    private boolean matchesActionIdentity(String actionCode, String actionName, Set<String> candidates) {
        String code = normalizeActionIdentity(actionCode);
        String name = normalizeActionIdentity(actionName);
        return candidates.contains(code) || candidates.contains(name);
    }

    private String normalizeActionIdentity(String value) {
        return StringUtils.lowerCase(StringUtils.defaultString(value))
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .trim();
    }

    private String normalizeActionPosition(Object value) {
        String position = StringUtils.defaultIfBlank(text(value), "ROW").trim().toUpperCase();
        if ("TOOLBAR".equals(position)) {
            return "toolbar";
        }
        if ("DETAIL".equals(position)) {
            return "detail";
        }
        if ("CHILD_ROW".equals(position)) {
            return "childRow";
        }
        return "row";
    }

    private String resolveRuntimeActionType(String actionType) {
        return resolveRuntimeActionType(actionType, Map.of());
    }

    private String resolveRuntimeActionType(String actionType, Map<String, Object> config) {
        return switch (actionType) {
            case "START_FLOW" -> "START_FLOW";
            case "START_APPROVAL" -> "START_FLOW";
            case "COMMAND" -> "COMMAND";
            case "OPEN_EXTERNAL" -> "external";
            case "TRIGGER" -> "TRIGGER";
            case "CALL_API" -> "CALL_API";
            case "REFRESH" -> "refresh";
            // 应用内页面：保留 page，避免发布后只剩 route 且丢 targetPageKey 语义
            case "OPEN_PAGE" -> "page".equalsIgnoreCase(text(config == null ? null : config.get("navigationMode")))
                    ? "page"
                    : "route";
            default -> "route";
        };
    }

    private String resolveActionButtonType(String actionType) {
        return switch (actionType) {
            case "START_FLOW" -> "success";
            case "OPEN_EXTERNAL" -> "info";
            case "TRIGGER", "CALL_API" -> "warning";
            default -> "primary";
        };
    }

    private String resolveActionRoutePath(String actionType, Map<String, Object> config) {
        if ("OPEN_EXTERNAL".equals(actionType)) {
            return text(config.get("url"));
        }
        if ("OPEN_PAGE".equals(actionType)) {
            return StringUtils.firstNonBlank(text(config.get("targetPath")), text(config.get("routePath")));
        }
        return null;
    }

    private void checkFields(LowcodeModelSchema modelSchema, List<BusinessPublishCheckItemVO> items) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            add(items, "FIELD_EMPTY", "FIELD", BusinessPublishCheckLevel.BLOCK,
                    "字段为空", "业务对象至少需要一个业务字段", null, null, "ADD_FIELD", "添加字段", "fields", 10);
            return;
        }
        Map<String, Integer> fieldCount = new LinkedHashMap<>();
        int businessFieldCount = 0;
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null || Boolean.TRUE.equals(field.getSystemField())) {
                continue;
            }
            String fieldStatus = StringUtils.defaultString(field.getFieldStatus());
            if ("HIDDEN".equalsIgnoreCase(fieldStatus)) {
                continue;
            }
            businessFieldCount++;
            if (StringUtils.isBlank(field.getLabel())) {
                add(items, "FIELD_LABEL_EMPTY", "FIELD", BusinessPublishCheckLevel.BLOCK,
                        "字段名称为空", "字段缺少展示名称", field.getField(), null,
                        "EDIT_FIELD", "编辑字段", "fields", 20);
            }
            if (StringUtils.isBlank(field.getField())) {
                add(items, "FIELD_CODE_EMPTY", "FIELD", BusinessPublishCheckLevel.BLOCK,
                        "字段编码为空", "字段缺少稳定编码", null, null,
                        "EDIT_FIELD", "编辑字段", "fields", 30);
            } else {
                fieldCount.merge(field.getField(), 1, Integer::sum);
            }
            if ("DISABLED".equalsIgnoreCase(fieldStatus)) {
                add(items, "FIELD_DISABLED", "FIELD", BusinessPublishCheckLevel.WARN,
                        "字段已停用", "停用字段不会进入默认表单和列表: " + field.getLabel(), field.getField(), null,
                        "EDIT_FIELD", "检查字段", "fields", 40);
            }
        }
        if (businessFieldCount == 0) {
            add(items, "FIELD_EMPTY", "FIELD", BusinessPublishCheckLevel.BLOCK,
                    "字段为空", "业务对象至少需要一个业务字段", null, null, "ADD_FIELD", "添加字段", "fields", 50);
        }
        fieldCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .forEach(entry -> add(items, "FIELD_DUPLICATE", "FIELD", BusinessPublishCheckLevel.BLOCK,
                        "字段编码重复", "字段编码重复: " + entry.getKey(), entry.getKey(), null,
                        "EDIT_FIELD", "修复字段编码", "fields", 60));
        if (items.stream().noneMatch(item -> "FIELD".equals(item.getCategory()))) {
            add(items, "FIELD_PASS", "FIELD", BusinessPublishCheckLevel.PASS,
                    "字段检查通过", "业务字段命名、编码和数量满足发布要求", null, null, null, null, "fields", 90);
        }
    }

    private BusinessObjectPagePublishValidator pagePublishValidator() {
        return new BusinessObjectPagePublishValidator(objectMapper, schemaValidator);
    }

    private BusinessObjectDesignPublishValidator designPublishValidator() {
        return new BusinessObjectDesignPublishValidator(
                objectMapper, designerService, businessObjectMapper, runtimeConfigBuilder,
                documentConfigService, triggerMapper, permissionService);
    }

    private BusinessObjectDeploymentPublishValidator deploymentPublishValidator() {
        return new BusinessObjectDeploymentPublishValidator(
                objectMapper, businessAppMapper, runtimeDataSourceResolver, ddlService);
    }
    private void checkFormFirstSchemas(BusinessObjectDesignerService.DesignerContext context,
                                       List<BusinessPublishCheckItemVO> items) {
        FORM_PUBLISH_VALIDATOR.validate(
                readDesignerOptions(context),
                collectPageFields(context.getModelSchema(), context.getPageSchema()),
                hasBusinessFields(context.getModelSchema()),
                items);
    }

    private BusinessPublishCheckVO buildResult(List<BusinessPublishCheckItemVO> items) {
        items.sort(Comparator.comparing(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder()));
        BusinessPublishCheckVO vo = new BusinessPublishCheckVO();
        vo.setItems(items);
        vo.setPassItems(items.stream().filter(item -> BusinessPublishCheckLevel.PASS.equals(item.getLevel())).toList());
        vo.setWarnItems(items.stream().filter(item -> BusinessPublishCheckLevel.WARN.equals(item.getLevel())).toList());
        vo.setBlockItems(items.stream().filter(item -> BusinessPublishCheckLevel.BLOCK.equals(item.getLevel())).toList());
        vo.setPassCount(vo.getPassItems().size());
        vo.setWarnCount(vo.getWarnItems().size());
        vo.setBlockCount(vo.getBlockItems().size());
        vo.setPublishable(vo.getBlockCount() == 0);
        vo.setOverallStatus(vo.getBlockCount() > 0 ? BusinessPublishCheckLevel.BLOCK
                : vo.getWarnCount() > 0 ? BusinessPublishCheckLevel.WARN : BusinessPublishCheckLevel.PASS);
        return vo;
    }

    private void add(List<BusinessPublishCheckItemVO> items, String code, String category, String level,
                     String title, String message, String fieldCode, String zoneKey, String fixAction,
                     String fixActionLabel, String fixTarget, Integer sortOrder) {
        BusinessPublishCheckCollector.add(items, code, category, level, title, message, fieldCode, zoneKey,
                fixAction, fixActionLabel, fixTarget, sortOrder);
    }

    private Map<String, Object> readDesignerOptions(BusinessObjectDesignerService.DesignerContext context) {
        if (context == null || context.getObject() == null
                || StringUtils.isBlank(context.getObject().getDesignerOptions())) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(context.getObject().getDesignerOptions(), new TypeReference<>() {});
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private boolean hasBusinessFields(LowcodeModelSchema modelSchema) {
        return modelSchema != null && modelSchema.getFields() != null
                && modelSchema.getFields().stream()
                .anyMatch(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()));
    }

    private Set<String> collectFields(LowcodeModelSchema modelSchema) {
        Set<String> fields = new LinkedHashSet<>();
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field != null && StringUtils.isNotBlank(field.getField())) {
                    fields.add(field.getField());
                }
            }
        }
        return fields;
    }

    private Set<String> collectPageFields(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        Set<String> fields = collectFields(modelSchema);
        if (pageSchema == null || pageSchema.getModelRefs() == null) {
            return fields;
        }
        pageSchema.getModelRefs().stream()
                .filter(ref -> ref != null && ref.getFields() != null)
                .forEach(ref -> {
                    String modelCode = StringUtils.trimToEmpty(ref.getModelCode());
                    for (Map<String, Object> field : ref.getFields()) {
                        String sourceField = text(field.get("sourceField"));
                        if (StringUtils.isBlank(sourceField)) {
                            sourceField = text(field.get("field"));
                        }
                        String fieldRef = text(field.get("fieldRef"));
                        String columnName = text(field.get("columnName"));
                        if (StringUtils.isNotBlank(fieldRef)) {
                            fields.add(fieldRef);
                        }
                        if (StringUtils.isNotBlank(sourceField)) {
                            fields.add(sourceField);
                        }
                        if (StringUtils.isNotBlank(columnName)) {
                            fields.add(columnName);
                        }
                        if (StringUtils.isNotBlank(modelCode) && StringUtils.isNotBlank(sourceField)) {
                            fields.add(modelCode + "." + sourceField);
                            fields.add(modelCode + "__" + sourceField);
                        }
                    }
                });
        return fields;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
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

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(this::mapValue)
                .toList();
    }

    private void checkFormula(BusinessObjectDesignerService.DesignerContext context,
                              List<BusinessPublishCheckItemVO> items) {
        if (context == null || context.getModelSchema() == null) {
            return;
        }
        FormulaValidationResult result = formulaPublishValidator.validate(
                context.getModelSchema(), buildFormulaObjectContexts(context));
        if (result == null) {
            return;
        }
        if (result.isValid() && !result.hasWarnings()) {
            add(items, "FORMULA_PASS", "FORMULA", BusinessPublishCheckLevel.PASS,
                    "Formula check passed", "All formula configurations passed validation",
                    null, null, null, null, null, 410);
            return;
        }
        if (result.hasErrors()) {
            for (FormulaValidationResult.FormulaError error : result.getErrors()) {
                add(items, "FORMULA_ERROR", "FORMULA", BusinessPublishCheckLevel.BLOCK,
                        "Formula configuration error",
                        "[" + error.getCategory() + "] " + error.getFieldName() + ": " + error.getMessage(),
                        error.getFieldName(), null,
                        "EDIT_FIELD", "Edit field formula",
                        "fields", 420);
            }
        }
        if (result.hasWarnings()) {
            for (String warning : result.getWarnings()) {
                add(items, "FORMULA_WARN", "FORMULA", BusinessPublishCheckLevel.WARN,
                        "Formula warning", warning,
                        null, null, null, null, null, 430);
            }
        }
        if (!result.hasErrors() && result.hasWarnings()) {
            add(items, "FORMULA_PASS", "FORMULA", BusinessPublishCheckLevel.PASS,
                    "Formula check passed (with warnings)", "Formula validation passed with warnings",
                    null, null, null, null, null, 440);
        }
    }

    private List<FormulaObjectDependencyAnalyzer.ObjectContext> buildFormulaObjectContexts(
            BusinessObjectDesignerService.DesignerContext context) {
        // 只校验当前对象公式；禁止按套件全量 loadContext（小应用也会放大成数十次编译读库）。
        List<FormulaObjectDependencyAnalyzer.ObjectContext> contexts = new ArrayList<>();
        addFormulaObjectContext(contexts, context);
        return contexts;
    }

    private void addFormulaObjectContext(List<FormulaObjectDependencyAnalyzer.ObjectContext> contexts,
                                         BusinessObjectDesignerService.DesignerContext context) {
        if (context == null || context.getObject() == null || StringUtils.isBlank(context.getObject().getObjectCode())) {
            return;
        }
        contexts.add(formulaPublishValidator.buildObjectContext(
                context.getObject().getObjectCode(),
                context.getModelSchema(),
                toFormulaObjectRelations(context.getRelations())));
    }

    private List<FormulaObjectDependencyAnalyzer.ObjectRelation> toFormulaObjectRelations(
            List<BusinessObjectRelationVO> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        List<FormulaObjectDependencyAnalyzer.ObjectRelation> result = new ArrayList<>();
        for (BusinessObjectRelationVO relation : relations) {
            if (relation == null) {
                continue;
            }
            result.add(new FormulaObjectDependencyAnalyzer.ObjectRelation(
                    relation.getId() == null ? null : String.valueOf(relation.getId()),
                    relation.getRelationName(),
                    relation.getSourceObjectCode(),
                    relation.getTargetObjectCode(),
                    relation.getSourceFieldCode(),
                    relation.getTargetFieldCode()));
        }
        return result;
    }

    private void enqueueCrossObjectRecompute(BusinessObjectDesignerService.DesignerContext context) {
        if (context == null || context.getObject() == null || context.getModelSchema() == null) {
            return;
        }
        crossObjectRecomputeTaskService.enqueueForPublish(formulaPublishValidator.buildObjectContext(
                context.getObject().getObjectCode(),
                context.getModelSchema(),
                toFormulaObjectRelations(context.getRelations())));
    }

    private Long resolveTenantId(BusinessObjectDesignerService.DesignerContext context) {
        if (context != null && context.getObject() != null && context.getObject().getTenantId() != null) {
            return context.getObject().getTenantId();
        }
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
