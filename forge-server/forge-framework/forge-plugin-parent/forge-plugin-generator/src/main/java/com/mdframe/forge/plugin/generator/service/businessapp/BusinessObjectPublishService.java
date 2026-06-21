package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessPublishCheckLevel;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignVersionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectPublishDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePublishDTO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePublishService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.service.formula.CrossObjectRecomputeTaskService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaObjectDependencyAnalyzer;
import com.mdframe.forge.plugin.generator.service.formula.FormulaPublishValidator;
import com.mdframe.forge.plugin.generator.service.formula.FormulaValidationResult;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignVersionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckItemVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDdlPreviewVO;
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

    private static final String DDL_PERMISSION = "ai:lowcode:deploy-ddl";
    private static final String FORM_DESIGNER_SCHEMA_OPTION_KEY = "formDesignerSchema";
    private static final String VIEW_SCHEMA_OPTION_KEY = "viewSchema";
    private static final String LINKAGE_SCHEMA_OPTION_KEY = "linkageSchema";
    private static final String DESIGNER_ACTIONS_KEY = "actions";
    private static final Set<String> RUNTIME_OPEN_MODES = Set.of("LIST", "CREATE_FORM", "DETAIL");
    private static final Set<String> APP_ENTRY_TYPES = Set.of(
            "OBJECT_LIST", "CREATE_FORM", "DETAIL_PAGE", "APPROVAL_TODO", "REPORT_DASHBOARD", "EXTERNAL_OR_API");
    private static final Set<String> GENERIC_FORM_COMPONENT_ID_SUFFIXES = Set.of(
            "input", "textarea", "number", "inputnumber", "integer", "money", "date", "datetime", "time",
            "switch", "select", "radio", "checkbox", "dictselect", "cascader", "regiontreeselect",
            "orgtreeselect", "orgselect", "departmentselect", "departmenttreeselect", "deptselect",
            "depttreeselect", "eltreeselect", "orgname", "deptname", "userselect", "userpicker",
            "username", "fileupload", "imageupload", "upload", "objectreference", "fcrow", "row",
            "col", "elcard", "card", "eltabs", "tabs", "eltabpane", "tabpane", "elcollapse",
            "collapse", "elcollapseitem", "collapseitem", "fctable", "table", "fctablegrid",
            "tablegrid", "eldivider", "divider", "fctitle", "title", "text", "html", "space",
            "elalert", "alert", "elbutton", "button", "eltag", "tag", "elimage", "image");
    private static final Set<String> REQUIRED_BLOCK_ITEM_CODES = Set.of(
            "FIELD_EMPTY", "FIELD_LABEL_EMPTY", "FIELD_CODE_EMPTY", "FIELD_DUPLICATE",
            "PAGE_EMPTY", "PAGE_REF_MISSING", "PAGE_SCHEMA_INVALID", "PAGE_TARGET_MISSING",
            "FORM_TARGET_MISSING", "FORM_COMPONENT_EMPTY", "FORM_COMPONENT_DUPLICATE",
            "FORM_FIELD_BINDING_EMPTY", "FORM_FIELD_MISSING", "FORM_FIELD_COMPONENT_EMPTY",
            "VIEW_FIELD_MISSING", "RUNTIME_INVALID", "APP_ENTRY_PAGE_MISSING", "APP_ENTRY_FORM_MISSING",
            "TABLE_NAME_EMPTY", "TABLE_MISSING", "TABLE_PK_MISSING", "TABLE_COLUMN_MISSING",
            "TABLE_COLUMN_CHANGED", "TABLE_INDEX_MISSING"
    );

    private final BusinessObjectDesignerService designerService;
    private final BusinessObjectDesignVersionService designVersionService;
    private final LowcodePublishService lowcodePublishService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final LowcodeSchemaValidator schemaValidator;
    private final FormulaPublishValidator formulaPublishValidator;
    private final CrossObjectRecomputeTaskService crossObjectRecomputeTaskService;
    private final LowcodeDdlService ddlService;
    private final AiCrudConfigMapper crudConfigMapper;
    private final BusinessAppMapper businessAppMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessTriggerMapper triggerMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessPermissionService permissionService;
    private final ObjectMapper objectMapper;

    public BusinessPublishCheckVO publishCheck(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        designerService.compileFormFirstRuntimeSchema(context);
        designerService.applyRelationsToModel(context);
        List<BusinessPublishCheckItemVO> items = new ArrayList<>();
        checkFields(context.getModelSchema(), items);
        checkPage(context, items);
        checkFormFirstSchemas(context, items);
        checkRelations(context, items);
        checkLinkage(context, items);
        checkRuntimeConfig(context, items);
        checkAppEntry(context, items);
        checkDocumentConfig(context, items);
        checkPermissionSummary(context, items);
        checkTable(context.getModelSchema(), items);
        checkFormula(context, items);
        return buildResult(items);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long objectId, BusinessObjectPublishDTO dto) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        if (dto != null && dto.getModelSchema() != null) {
            context.setModelSchema(dto.getModelSchema());
        }
        if (dto != null && dto.getPageSchema() != null) {
            context.setPageSchema(dto.getPageSchema());
        }
        designerService.applyRelationsToModel(context);
        context = designerService.saveDraft(context, BusinessObjectDesignStatus.READY);
        BusinessPublishCheckVO check = publishCheck(objectId);
        if (Boolean.FALSE.equals(check.getPublishable()) && (dto == null || !Boolean.TRUE.equals(dto.getForce()))) {
            throw new BusinessException(buildPublishBlockedMessage(check));
        }

        LowcodePublishDTO publishDTO = buildPublishDTO(context, dto);
        Long crudConfigVersionId = lowcodePublishService.publish(context.getConfig().getId(), publishDTO);
        AiCrudConfig publishedConfig = crudConfigMapper.selectById(context.getConfig().getId());
        if (publishedConfig == null) {
            throw new BusinessException("发布后运行配置不存在");
        }
        AiBusinessObject object = businessObjectMapper.selectById(objectId);
        object.setConfigKey(publishedConfig.getConfigKey());
        object.setModelCode(publishedConfig.getObjectCode());
        object.setDesignStatus(BusinessObjectDesignStatus.PUBLISHED);
        object.setLastPublishVersion(publishedConfig.getPublishedVersion());
        object.setLastPublishTime(publishedConfig.getPublishTime());
        businessObjectMapper.updateById(object);

        BusinessObjectDesignVersionDTO versionDTO = new BusinessObjectDesignVersionDTO();
        versionDTO.setObjectId(objectId);
        versionDTO.setSuiteCode(object.getSuiteCode());
        versionDTO.setObjectCode(object.getObjectCode());
        versionDTO.setConfigId(publishedConfig.getId());
        versionDTO.setConfigKey(publishedConfig.getConfigKey());
        versionDTO.setCrudConfigVersionId(crudConfigVersionId);
        versionDTO.setVersionNo(publishedConfig.getPublishedVersion());
        versionDTO.setVersionType("publish");
        versionDTO.setModelSnapshot(context.getModelSchema());
        versionDTO.setPageSnapshot(context.getPageSchema());
        versionDTO.setRelationSnapshot(context.getRelations());
        versionDTO.setDesignerOptionsSnapshot(readDesignerOptions(context));
        versionDTO.setPublishStatus("PUBLISHED");
        versionDTO.setPublishVersion(publishedConfig.getPublishedVersion());
        versionDTO.setRemark(dto == null ? null : dto.getRemark());
        Long versionId = designVersionService.createVersion(versionDTO);
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
        BusinessObjectDesignVersionVO version = designVersionService.detail(objectId, versionId);
        if (version.getConfigId() != null && version.getCrudConfigVersionId() != null) {
            lowcodePublishService.rollback(version.getConfigId(), version.getCrudConfigVersionId());
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
        designVersionService.createVersion(rollbackVersion);
    }

    private LowcodePublishDTO buildPublishDTO(BusinessObjectDesignerService.DesignerContext context,
                                              BusinessObjectPublishDTO dto) {
        LowcodePublishDTO publishDTO = new LowcodePublishDTO();
        boolean syncTable = dto != null && Boolean.TRUE.equals(dto.getSyncTable());
        publishDTO.setDeployMode(syncTable ? "ONLINE_CREATE_TABLE" : "SKIP_DDL");
        publishDTO.setConfirmOnlineDdl(syncTable);
        publishDTO.setMenuName(context.getObject().getObjectName());
        publishDTO.setMenuSort(context.getConfig() == null ? context.getObject().getSortOrder() : context.getConfig().getMenuSort());
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
        List<Map<String, Object>> customActions = buildRuntimeCustomActions(readDesignerOptions(context));
        if (pageSchema == null || customActions.isEmpty()) {
            return pageSchema;
        }
        LowcodePageSchema next = objectMapper.convertValue(pageSchema, LowcodePageSchema.class);
        LowcodePageZone tableZone = findOrCreateZone(next, "table");
        Map<String, Object> props = tableZone.getProps() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tableZone.getProps());
        props.put("customActions", customActions);
        tableZone.setProps(props);
        return next;
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

    private List<Map<String, Object>> buildRuntimeCustomActions(Map<String, Object> designerOptions) {
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
            if (isBuiltinCrudAction(position, actionCode, actionName, actionType, config)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", StringUtils.defaultIfBlank(actionCode, "custom_" + result.size()));
            item.put("label", StringUtils.defaultIfBlank(actionName, "自定义操作"));
            item.put("position", position);
            item.put("type", resolveActionButtonType(actionType));
            item.put("actionType", resolveRuntimeActionType(actionType));
            putIfNotBlank(item, "routePath", resolveActionRoutePath(actionType, config));
            putIfNotBlank(item, "targetFormKey", text(config.get("targetFormKey")));
            putIfNotBlank(item, "openTarget", StringUtils.defaultIfBlank(text(config.get("openTarget")), "_self"));
            if (Boolean.TRUE.equals(booleanValue(action.get("confirmRequired")))) {
                item.put("confirmText", "确认执行“" + StringUtils.defaultIfBlank(actionName, "该操作") + "”？");
            }
            result.add(item);
        }
        return result;
    }

    private boolean isBuiltinCrudAction(String position, String actionCode, String actionName,
                                        String actionType, Map<String, Object> config) {
        if (!"OPEN_PAGE".equals(actionType) || StringUtils.isNotBlank(resolveActionRoutePath(actionType, config))) {
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
        return "row";
    }

    private String resolveRuntimeActionType(String actionType) {
        return switch (actionType) {
            case "START_FLOW" -> "START_FLOW";
            case "OPEN_EXTERNAL" -> "external";
            case "TRIGGER" -> "TRIGGER";
            case "CALL_API" -> "CALL_API";
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

    private void checkPage(BusinessObjectDesignerService.DesignerContext context,
                           List<BusinessPublishCheckItemVO> items) {
        LowcodeModelSchema modelSchema = context.getModelSchema();
        LowcodePageSchema pageSchema = context.getPageSchema();
        if (pageSchema == null) {
            add(items, "PAGE_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "页面布局为空", "请先配置表单、列表或详情布局", null, null,
                    "CONFIG_LAYOUT", "配置布局", "form", 100);
            return;
        }
        Set<String> modelFields = collectPageFields(modelSchema, pageSchema);
        if (pageSchema.getZones() != null) {
            for (LowcodePageZone zone : pageSchema.getZones()) {
                if (zone == null || zone.getFieldRefs() == null) {
                    continue;
                }
                for (String ref : zone.getFieldRefs()) {
                    if (StringUtils.isNotBlank(ref) && !modelFields.contains(ref)) {
                        add(items, "PAGE_REF_MISSING", "PAGE", BusinessPublishCheckLevel.BLOCK,
                                "页面引用了不存在的字段", "区域 " + zone.getZoneKey() + " 引用了不存在字段: " + ref,
                                ref, zone.getZoneKey(), "REMOVE_FIELD_REF", "移除脏引用", zone.getZoneKey(), 110);
                    }
                }
            }
        }
        try {
            schemaValidator.validatePage(pageSchema, modelSchema);
            add(items, "PAGE_PASS", "PAGE", BusinessPublishCheckLevel.PASS,
                    "页面检查通过", "表单、列表和详情布局字段引用有效", null, null, null, null, "form", 190);
        } catch (BusinessException e) {
            add(items, "PAGE_SCHEMA_INVALID", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "页面协议校验失败", e.getMessage(), null, null,
                    "FIX_LAYOUT", "修复布局", "form", 120);
        }
        checkPageTargetReferences(pageSchema, collectFormKeys(readDesignerOptions(context)), items);
        checkPageActionCompleteness(pageSchema, collectFields(modelSchema), items);
    }

    private void checkPageTargetReferences(LowcodePageSchema pageSchema, Set<String> formKeys,
                                           List<BusinessPublishCheckItemVO> items) {
        Set<String> pageKeys = collectPageKeys(pageSchema);
        List<Map<String, Object>> targets = collectPageTargetConfigs(pageSchema);
        int index = 0;
        for (Map<String, Object> target : targets) {
            index++;
            String source = StringUtils.defaultIfBlank(text(target.get("_source")), "页面动作");
            String targetPageKey = text(target.get("targetPageKey"));
            if (StringUtils.isNotBlank(targetPageKey) && !pageKeys.contains(targetPageKey)) {
                add(items, "PAGE_TARGET_MISSING", "PAGE", BusinessPublishCheckLevel.BLOCK,
                        "目标页面不存在", source + " 引用了不存在的页面: " + targetPageKey,
                        targetPageKey, source, "FIX_PAGE_TARGET", "修复页面跳转", "list", 121 + index);
            }
            String targetFormKey = text(target.get("targetFormKey"));
            if (StringUtils.isNotBlank(targetFormKey) && !formKeys.contains(targetFormKey)) {
                add(items, "FORM_TARGET_MISSING", "PAGE", BusinessPublishCheckLevel.BLOCK,
                        "目标表单不存在", source + " 引用了不存在的表单: " + targetFormKey,
                        targetFormKey, source, "FIX_FORM_TARGET", "修复表单跳转", "list", 141 + index);
            }
        }
    }

    private Set<String> collectPageKeys(LowcodePageSchema pageSchema) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add("list");
        keys.add("detail");
        if (pageSchema != null && pageSchema.getPages() != null) {
            pageSchema.getPages().forEach(page -> {
                String key = text(page.get("pageKey"));
                if (StringUtils.isNotBlank(key)) {
                    keys.add(key);
                }
            });
        }
        return keys;
    }

    private Set<String> collectFormKeys(Map<String, Object> designerOptions) {
        Set<String> keys = new LinkedHashSet<>();
        Map<String, Object> formSchema = mapValue(designerOptions.get(FORM_DESIGNER_SCHEMA_OPTION_KEY));
        addIfNotBlank(keys, text(formSchema.get("formKey")));
        addIfNotBlank(keys, text(formSchema.get("defaultFormKey")));
        listOfMap(formSchema.get("forms")).forEach(form -> {
            addIfNotBlank(keys, text(form.get("formKey")));
            addIfNotBlank(keys, text(mapValue(form.get("schema")).get("formKey")));
        });
        listOfMap(mapValue(formSchema.get("settings")).get("formAssets")).forEach(asset -> {
            addIfNotBlank(keys, text(asset.get("formKey")));
            addIfNotBlank(keys, text(mapValue(asset.get("schema")).get("formKey")));
        });
        return keys;
    }

    private void addIfNotBlank(Set<String> keys, String value) {
        if (StringUtils.isNotBlank(value)) {
            keys.add(value);
        }
    }

    private List<Map<String, Object>> collectPageTargetConfigs(LowcodePageSchema pageSchema) {
        if (pageSchema == null) {
            return List.of();
        }
        List<Map<String, Object>> targets = new ArrayList<>();
        collectLayoutTargetConfigs(pageSchema.getListGridLayout(), "列表页", targets);
        if (pageSchema.getPages() != null) {
            pageSchema.getPages().forEach(page -> collectLayoutTargetConfigs(
                    mapValue(page.get("gridLayout")),
                    StringUtils.defaultIfBlank(text(page.get("pageName")), text(page.get("pageKey"))),
                    targets));
        }
        return targets;
    }

    private void collectLayoutTargetConfigs(Map<String, Object> layout, String pageName,
                                            List<Map<String, Object>> targets) {
        for (Map<String, Object> block : listOfMap(layout.get("items"))) {
            Map<String, Object> props = mapValue(block.get("props"));
            collectTargetConfig(props, pageName + "/" + StringUtils.defaultIfBlank(text(block.get("blockType")), "区块"), targets);
            listOfMap(props.get("events")).forEach(event -> collectTargetConfig(event, pageName + "/事件", targets));
            listOfMap(props.get("customActions")).forEach(action -> collectTargetConfig(action, pageName + "/自定义操作", targets));
            mapValue(props.get("fieldSettings")).forEach((field, setting) ->
                    collectTargetConfig(mapValue(setting), pageName + "/字段 " + field, targets));
        }
    }

    private void collectTargetConfig(Map<String, Object> source, String sourceName,
                                     List<Map<String, Object>> targets) {
        String targetPageKey = text(source.get("targetPageKey"));
        String targetFormKey = text(source.get("targetFormKey"));
        if (StringUtils.isBlank(targetPageKey) && StringUtils.isBlank(targetFormKey)) {
            return;
        }
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("_source", sourceName);
        putIfNotBlank(target, "targetPageKey", targetPageKey);
        putIfNotBlank(target, "targetFormKey", targetFormKey);
        targets.add(target);
    }

    private void checkPageActionCompleteness(LowcodePageSchema pageSchema, Set<String> modelFields,
                                             List<BusinessPublishCheckItemVO> items) {
        if (pageSchema == null) {
            return;
        }
        List<Map<String, Object>> layouts = new ArrayList<>();
        layouts.add(pageSchema.getListGridLayout());
        if (pageSchema.getPages() != null) {
            pageSchema.getPages().forEach(page -> layouts.add(mapValue(page.get("gridLayout"))));
        }
        int index = 0;
        for (Map<String, Object> layout : layouts) {
            for (Map<String, Object> block : listOfMap(layout.get("items"))) {
                index++;
                String blockType = text(block.get("blockType"));
                Map<String, Object> props = mapValue(block.get("props"));
                String blockName = StringUtils.defaultIfBlank(text(props.get("title")), blockType);
                checkPreviewCompleteness(blockType, props, blockName, items, index);
                if ("action-button".equals(blockType) && listOfMap(props.get("events")).isEmpty()) {
                    add(items, "PAGE_BUTTON_ACTION_EMPTY", "PAGE", BusinessPublishCheckLevel.WARN,
                            "按钮未配置点击动作", "按钮「" + blockName + "」没有配置事件回调，运行态点击后不会执行业务动作",
                            text(block.get("id")), blockName, "CONFIG_BUTTON_ACTION", "配置按钮动作", "list", 151 + index);
                }
                int eventSortOrder = 170 + index;
                int actionSortOrder = 190 + index;
                listOfMap(props.get("events")).forEach(event ->
                        checkEventActionCompleteness(event, StringUtils.defaultIfBlank(blockName, "区块") + "/事件", modelFields, items, eventSortOrder));
                listOfMap(props.get("customActions")).forEach(action ->
                        checkCustomActionCompleteness(action, StringUtils.defaultIfBlank(blockName, "区块") + "/自定义操作", modelFields, items, actionSortOrder));
            }
        }
    }

    private void checkPreviewCompleteness(String blockType, Map<String, Object> props, String blockName,
                                          List<BusinessPublishCheckItemVO> items, int index) {
        if (!"AiCrudPage".equals(blockType) || !Boolean.TRUE.equals(props.get("previewLiveData"))) {
            return;
        }
        String status = text(props.get("lastPreviewStatus"));
        String error = text(props.get("lastPreviewError"));
        String mode = StringUtils.defaultIfBlank(text(props.get("previewMode")), "realList");
        if ("error".equalsIgnoreCase(status) || StringUtils.isNotBlank(error)) {
            add(items, "PAGE_PREVIEW_API_ERROR", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "真实接口预览失败", "组件「" + blockName + "」的 " + mode + " 预览失败: " + StringUtils.defaultIfBlank(error, "未知错误"),
                    null, blockName, "FIX_PREVIEW_API", "修复接口预览", "list", 160 + index);
        } else if (!"success".equalsIgnoreCase(status)) {
            add(items, "PAGE_PREVIEW_API_NOT_VERIFIED", "PAGE", BusinessPublishCheckLevel.WARN,
                    "真实接口预览未验证通过", "组件「" + blockName + "」已开启真实接口预览，但还没有成功请求记录",
                    null, blockName, "CHECK_PREVIEW_API", "执行接口预览", "list", 161 + index);
        }
    }

    private void checkEventActionCompleteness(Map<String, Object> event, String source,
                                              Set<String> modelFields, List<BusinessPublishCheckItemVO> items, int sortOrder) {
        String action = StringUtils.defaultIfBlank(text(event.get("action")), "none");
        if ("none".equalsIgnoreCase(action)) {
            add(items, "PAGE_EVENT_ACTION_NONE", "PAGE", BusinessPublishCheckLevel.WARN,
                    "事件未配置动作", source + " 选择了无动作，触发后不会产生业务行为",
                    null, source, "CONFIG_EVENT_ACTION", "配置事件动作", "list", sortOrder);
            return;
        }
        if ("navigate".equalsIgnoreCase(action) && StringUtils.isBlank(text(event.get("targetPageKey")))) {
            add(items, "PAGE_EVENT_TARGET_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "跳转事件缺少目标页面", source + " 是页面跳转，但未选择目标页面",
                    null, source, "CONFIG_EVENT_TARGET", "配置目标页面", "list", sortOrder + 1);
        }
        if (Set.of("refreshBlock", "filterBlock").contains(action) && StringUtils.isBlank(text(event.get("targetBlockId")))) {
            add(items, "PAGE_EVENT_BLOCK_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "组件事件缺少目标组件", source + " 需要选择目标组件",
                    null, source, "CONFIG_EVENT_TARGET", "配置目标组件", "list", sortOrder + 2);
        }
        if ("request".equalsIgnoreCase(action) && StringUtils.isBlank(text(event.get("requestUrl")))) {
            add(items, "PAGE_EVENT_API_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "接口事件缺少地址", source + " 是接口请求，但未配置请求地址",
                    null, source, "CONFIG_EVENT_API", "配置接口地址", "list", sortOrder + 3);
        }
        checkActionParams(event.get("params"), source + "/参数", modelFields, items, sortOrder + 4);
    }

    private void checkCustomActionCompleteness(Map<String, Object> action, String source,
                                               Set<String> modelFields, List<BusinessPublishCheckItemVO> items, int sortOrder) {
        String actionType = StringUtils.defaultIfBlank(text(action.get("actionType")), "route");
        if ("refresh".equalsIgnoreCase(actionType)) {
            return;
        }
        if (StringUtils.isBlank(text(action.get("routePath")))) {
            add(items, "PAGE_CUSTOM_ACTION_TARGET_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                    "自定义操作缺少目标地址", source + " 是" + ("external".equalsIgnoreCase(actionType) ? "外部链接" : "站内跳转") + "，但未配置目标地址",
                    text(action.get("key")), source, "CONFIG_CUSTOM_ACTION", "配置自定义操作", "list", sortOrder);
        }
        checkActionParams(action.get("params"), source + "/参数", modelFields, items, sortOrder + 1);
    }

    private void checkActionParams(Object paramsValue, String source, Set<String> modelFields,
                                   List<BusinessPublishCheckItemVO> items, int sortOrder) {
        if (!(paramsValue instanceof List<?> params) || params.isEmpty()) {
            return;
        }
        int index = 0;
        for (Object item : params) {
            index++;
            if (!(item instanceof Map<?, ?> param)) {
                continue;
            }
            String name = text(param.get("name"));
            String sourceType = StringUtils.defaultIfBlank(text(param.get("sourceType")), "static");
            String sourceField = text(param.get("sourceField"));
            String value = text(param.get("value"));
            if (StringUtils.isBlank(name)) {
                add(items, "PAGE_ACTION_PARAM_NAME_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                        "动作参数缺少参数名", source + " 第 " + index + " 行未填写参数名",
                        null, source, "CONFIG_ACTION_PARAM", "配置参数映射", "list", sortOrder + index);
            }
            if ("rowField".equalsIgnoreCase(sourceType)) {
                if (StringUtils.isBlank(sourceField)) {
                    add(items, "PAGE_ACTION_PARAM_FIELD_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                            "动作参数缺少来源字段", source + " 参数「" + name + "」选择了当前行字段，但未选择字段",
                            name, source, "CONFIG_ACTION_PARAM", "配置参数映射", "list", sortOrder + index + 10);
                } else if (!modelFields.contains(sourceField)) {
                    add(items, "PAGE_ACTION_PARAM_FIELD_MISSING", "PAGE", BusinessPublishCheckLevel.BLOCK,
                            "动作参数引用字段不存在", source + " 参数「" + name + "」引用了不存在的字段: " + sourceField,
                            sourceField, source, "CONFIG_ACTION_PARAM", "修复参数映射", "list", sortOrder + index + 20);
                }
            } else if (Set.of("routeQuery", "system").contains(sourceType) && StringUtils.isBlank(sourceField)) {
                add(items, "PAGE_ACTION_PARAM_SOURCE_EMPTY", "PAGE", BusinessPublishCheckLevel.BLOCK,
                        "动作参数缺少来源", source + " 参数「" + name + "」选择了" + sourceType + "，但未选择具体来源",
                        name, source, "CONFIG_ACTION_PARAM", "配置参数映射", "list", sortOrder + index + 25);
            } else if (!Set.of("static", "routeQuery", "system").contains(sourceType) && StringUtils.isNotBlank(sourceType)) {
                add(items, "PAGE_ACTION_PARAM_SOURCE_INVALID", "PAGE", BusinessPublishCheckLevel.BLOCK,
                        "动作参数来源无效", source + " 参数「" + name + "」使用了不支持的来源类型: " + sourceType,
                        name, source, "CONFIG_ACTION_PARAM", "修复参数映射", "list", sortOrder + index + 30);
            }
            if ("static".equalsIgnoreCase(sourceType) && StringUtils.isBlank(value)) {
                add(items, "PAGE_ACTION_PARAM_VALUE_EMPTY", "PAGE", BusinessPublishCheckLevel.WARN,
                        "动作参数固定值为空", source + " 参数「" + name + "」没有固定值，运行态会传空",
                        name, source, "CONFIG_ACTION_PARAM", "检查参数映射", "list", sortOrder + index + 40);
            }
        }
    }

    private void checkFormFirstSchemas(BusinessObjectDesignerService.DesignerContext context,
                                       List<BusinessPublishCheckItemVO> items) {
        Set<String> modelFields = collectFields(context.getModelSchema());
        Map<String, Object> designerOptions = readDesignerOptions(context);
        Map<String, Object> formSchema = mapValue(designerOptions.get(FORM_DESIGNER_SCHEMA_OPTION_KEY));
        Map<String, Object> viewSchema = sanitizeViewSchemaFieldRefs(
                mapValue(designerOptions.get(VIEW_SCHEMA_OPTION_KEY)), modelFields);
        boolean checked = false;
        if (!formSchema.isEmpty()) {
            checked = true;
            checkFormDesignerSchema(formSchema, modelFields, items);
        } else if (hasBusinessFields(context.getModelSchema())) {
            add(items, "FORM_SCHEMA_DEFAULT", "FORM", BusinessPublishCheckLevel.PASS,
                    "表单检查通过", "未保存独立表单 Schema，将按字段注册表生成默认表单", null, null,
                    null, null, "form", 130);
        }
        if (!viewSchema.isEmpty()) {
            checked = true;
            checkViewSchema(viewSchema, modelFields, items);
        }
        if (checked && items.stream().noneMatch(item -> Set.of("FORM", "VIEW").contains(item.getCategory())
                && BusinessPublishCheckLevel.BLOCK.equals(item.getLevel()))) {
            add(items, "FORM_FIRST_PASS", "FORM", BusinessPublishCheckLevel.PASS,
                    "表单优先检查通过", "表单组件、视图投影和字段注册表引用一致", null, null,
                    null, null, "form", 180);
        }
    }

    private void checkFormDesignerSchema(Map<String, Object> formSchema, Set<String> modelFields,
                                         List<BusinessPublishCheckItemVO> items) {
        List<Map<String, Object>> components = listOfMap(formSchema.get("components"));
        if (components.isEmpty()) {
            add(items, "FORM_COMPONENT_EMPTY", "FORM", BusinessPublishCheckLevel.BLOCK,
                    "表单组件为空", "请先在表单设计中放入至少一个业务字段组件", null, null,
                    "CONFIG_FORM", "设计表单", "form", 131);
            return;
        }
        int fieldComponentCount = 0;
        Set<String> componentIds = new LinkedHashSet<>();
        for (int index = 0; index < components.size(); index++) {
            Map<String, Object> component = components.get(index);
            String componentId = text(component.get("id"));
            String componentKey = text(component.get("componentKey"));
            if (StringUtils.isNotBlank(componentId) && !isGenericFormComponentId(componentId, componentKey)
                    && !componentIds.add(componentId)) {
                add(items, "FORM_COMPONENT_DUPLICATE", "FORM", BusinessPublishCheckLevel.BLOCK,
                        "表单组件重复", "组件 ID 重复: " + componentId, null, null,
                        "FIX_FORM", "修复表单", "form", 132);
            }
            Map<String, Object> binding = mapValue(component.get("fieldBinding"));
            if (!"field".equals(StringUtils.defaultIfBlank(text(binding.get("mode")), "field"))) {
                continue;
            }
            String fieldCode = text(binding.get("fieldCode"));
            if (StringUtils.isBlank(fieldCode)) {
                add(items, "FORM_FIELD_BINDING_EMPTY", "FORM", BusinessPublishCheckLevel.BLOCK,
                        "表单字段未绑定", "组件 " + StringUtils.defaultIfBlank(componentId, String.valueOf(index + 1)) + " 未绑定业务字段",
                        null, null, "BIND_FIELD", "绑定字段", "form", 133);
                continue;
            }
            fieldComponentCount++;
            if (!modelFields.contains(fieldCode)) {
                add(items, "FORM_FIELD_MISSING", "FORM", BusinessPublishCheckLevel.BLOCK,
                        "表单引用字段不存在", "表单组件引用了不存在字段: " + fieldCode, fieldCode, null,
                        "FIX_FORM", "修复表单", "form", 134);
            }
        }
        if (fieldComponentCount == 0) {
            add(items, "FORM_FIELD_COMPONENT_EMPTY", "FORM", BusinessPublishCheckLevel.BLOCK,
                    "表单缺少业务字段", "表单中没有绑定业务字段的组件", null, null,
                    "CONFIG_FORM", "设计表单", "form", 135);
        }
        checkFormGovernance(formSchema, modelFields, items);
    }

    private void checkFormGovernance(Map<String, Object> formSchema, Set<String> modelFields,
                                     List<BusinessPublishCheckItemVO> items) {
        Map<String, Object> governance = mapValue(mapValue(formSchema.get("settings")).get("governance"));
        if (governance.isEmpty()) {
            return;
        }
        int index = 0;
        for (Map<String, Object> rule : listOfMap(governance.get("fieldRules"))) {
            index++;
            String field = text(rule.get("field"));
            if (StringUtils.isBlank(field)) {
                add(items, "FORM_RULE_FIELD_EMPTY", "FORM", BusinessPublishCheckLevel.WARN,
                        "表单字段规则未选择字段", "第 " + index + " 条字段覆盖规则没有选择字段", null, null,
                        "CONFIG_FORM_RULE", "配置字段规则", "form", 136 + index);
            } else if (!modelFields.contains(field)) {
                add(items, "FORM_RULE_FIELD_MISSING", "FORM", BusinessPublishCheckLevel.BLOCK,
                        "表单字段规则引用不存在字段", "字段覆盖规则引用了不存在字段: " + field, field, null,
                        "CONFIG_FORM_RULE", "修复字段规则", "form", 146 + index);
            }
        }
        index = 0;
        for (Map<String, Object> event : listOfMap(governance.get("events"))) {
            index++;
            String action = StringUtils.defaultIfBlank(text(event.get("action")), "customScript");
            String handler = text(event.get("handler"));
            if (!"none".equalsIgnoreCase(action) && StringUtils.isBlank(handler)) {
                add(items, "FORM_EVENT_HANDLER_EMPTY", "FORM", BusinessPublishCheckLevel.WARN,
                        "表单事件缺少处理器", "第 " + index + " 个表单事件没有配置脚本名、接口地址或动作编码",
                        null, null, "CONFIG_FORM_EVENT", "配置表单事件", "form", 156 + index);
            } else if ("customScript".equalsIgnoreCase(action) && StringUtils.isNotBlank(handler)
                    && !Set.of("noop", "fillCurrentDate", "fillCurrentTime").contains(handler)) {
                add(items, "FORM_EVENT_SCRIPT_NOT_ALLOWED", "FORM", BusinessPublishCheckLevel.BLOCK,
                        "表单事件脚本未登记", "脚本「" + handler + "」不在运行态白名单内，可用: noop、fillCurrentDate、fillCurrentTime",
                        handler, null, "CONFIG_FORM_EVENT", "修复表单事件", "form", 166 + index);
            }
        }
    }

    private boolean isGenericFormComponentId(String componentId, String componentKey) {
        String id = StringUtils.trimToEmpty(componentId);
        if (!StringUtils.startsWithIgnoreCase(id, "cmp_")) {
            return false;
        }
        String suffix = id.substring(4).trim().toLowerCase();
        if (StringUtils.isBlank(suffix)) {
            return true;
        }
        String key = StringUtils.trimToEmpty(componentKey).toLowerCase();
        return suffix.equals(key) || GENERIC_FORM_COMPONENT_ID_SUFFIXES.contains(suffix);
    }

    private void checkViewSchema(Map<String, Object> viewSchema, Set<String> modelFields,
                                 List<BusinessPublishCheckItemVO> items) {
        checkViewFieldRefs(listOfMap(mapValue(viewSchema.get("search")).get("fields")),
                modelFields, "查询条件", "search", items, 141);
        checkViewFieldRefs(listOfMap(mapValue(viewSchema.get("list")).get("columns")),
                modelFields, "数据列表", "list", items, 142);
        List<Map<String, Object>> sections = listOfMap(mapValue(viewSchema.get("detail")).get("sections"));
        for (Map<String, Object> section : sections) {
            String sectionKey = StringUtils.defaultIfBlank(text(section.get("sectionKey")), text(section.get("key")));
            checkViewFieldRefs(listOfMap(section.get("fields")), modelFields,
                    "详情视图", StringUtils.defaultIfBlank(sectionKey, "detail"), items, 143);
        }
    }

    private void checkViewFieldRefs(List<Map<String, Object>> refs, Set<String> modelFields, String viewName,
                                    String fixTarget, List<BusinessPublishCheckItemVO> items, int sortOrder) {
        for (Map<String, Object> ref : refs) {
            String fieldCode = StringUtils.defaultIfBlank(text(ref.get("fieldCode")), text(ref.get("field")));
            if (StringUtils.isNotBlank(fieldCode) && !modelFields.contains(fieldCode)) {
                add(items, "VIEW_FIELD_MISSING", "VIEW", BusinessPublishCheckLevel.BLOCK,
                        viewName + "引用字段不存在", viewName + "引用了不存在字段: " + fieldCode,
                        fieldCode, null, "FIX_VIEW", "修复视图", fixTarget, sortOrder);
            }
        }
    }

    private Map<String, Object> sanitizeViewSchemaFieldRefs(Map<String, Object> viewSchema, Set<String> modelFields) {
        if (viewSchema.isEmpty() || modelFields == null || modelFields.isEmpty()) {
            return viewSchema;
        }
        Map<String, Object> next = new LinkedHashMap<>(viewSchema);
        Map<String, Object> search = new LinkedHashMap<>(mapValue(next.get("search")));
        search.put("fields", filterViewFieldRefs(listOfMap(search.get("fields")), modelFields));
        next.put("search", search);

        Map<String, Object> list = new LinkedHashMap<>(mapValue(next.get("list")));
        list.put("columns", filterViewFieldRefs(listOfMap(list.get("columns")), modelFields));
        next.put("list", list);

        Map<String, Object> detail = new LinkedHashMap<>(mapValue(next.get("detail")));
        detail.put("sections", listOfMap(detail.get("sections")).stream()
                .map(section -> {
                    Map<String, Object> cleanSection = new LinkedHashMap<>(section);
                    cleanSection.put("fields", filterViewFieldRefs(listOfMap(section.get("fields")), modelFields));
                    return cleanSection;
                })
                .toList());
        next.put("detail", detail);
        return next;
    }

    private List<Map<String, Object>> filterViewFieldRefs(List<Map<String, Object>> refs, Set<String> modelFields) {
        return refs.stream()
                .filter(item -> modelFields.contains(viewFieldCode(item)))
                .toList();
    }

    private String viewFieldCode(Map<String, Object> item) {
        return StringUtils.defaultIfBlank(text(item.get("fieldCode")), text(item.get("field")));
    }

    private void checkRelations(BusinessObjectDesignerService.DesignerContext context,
                                List<BusinessPublishCheckItemVO> items) {
        List<BusinessObjectRelationVO> relations = context.getRelations();
        if (relations == null || relations.isEmpty()) {
            add(items, "RELATION_EMPTY", "RELATION", BusinessPublishCheckLevel.WARN,
                    "未配置对象关系", "对象可以先以单表发布，后续再补充关系", null, null,
                    "CONFIG_RELATION", "配置关系", "relations", 200);
            return;
        }
        Set<String> currentFields = collectFields(context.getModelSchema());
        for (BusinessObjectRelationVO relation : relations) {
            if (relation == null || !StringUtils.equals(context.getObject().getObjectCode(), relation.getSourceObjectCode())) {
                continue;
            }
            if (StringUtils.isBlank(relation.getTargetObjectCode())) {
                add(items, "RELATION_TARGET_EMPTY", "RELATION", BusinessPublishCheckLevel.BLOCK,
                        "关系目标为空", "关系缺少目标业务对象: " + relation.getRelationName(), null, null,
                        "EDIT_RELATION", "编辑关系", "relations", 210);
                continue;
            }
            AiBusinessObject target = businessObjectMapper.selectByObjectCode(
                    resolveTenantId(context), context.getObject().getSuiteCode(), relation.getTargetObjectCode());
            if (target == null) {
                add(items, "RELATION_TARGET_MISSING", "RELATION", BusinessPublishCheckLevel.BLOCK,
                        "关系目标不存在", "目标业务对象不存在: " + relation.getTargetObjectCode(), null, null,
                        "EDIT_RELATION", "编辑关系", "relations", 220);
                continue;
            }
            if (StringUtils.isBlank(relation.getSourceFieldCode())) {
                add(items, "RELATION_SOURCE_FIELD_EMPTY", "RELATION", BusinessPublishCheckLevel.BLOCK,
                        "当前对象字段为空", "关系缺少当前对象字段: " + relation.getRelationName(),
                        null, null, "EDIT_RELATION", "编辑关系", "relations", 225);
            } else if (!currentFields.contains(relation.getSourceFieldCode())) {
                add(items, "RELATION_SOURCE_FIELD_MISSING", "RELATION", BusinessPublishCheckLevel.BLOCK,
                        "关系字段不存在", "当前对象关系字段不存在: " + relation.getSourceFieldCode(),
                        relation.getSourceFieldCode(), null, "EDIT_RELATION", "编辑关系", "relations", 230);
            }
            Set<String> targetFields = collectFields(designerService.loadContext(target.getId()).getModelSchema());
            if (StringUtils.isBlank(relation.getTargetFieldCode())) {
                add(items, "RELATION_TARGET_FIELD_EMPTY", "RELATION", BusinessPublishCheckLevel.BLOCK,
                        "目标对象字段为空", "关系缺少目标对象字段: " + relation.getRelationName(),
                        null, null, "EDIT_RELATION", "编辑关系", "relations", 235);
            } else if (!targetFields.contains(relation.getTargetFieldCode())) {
                add(items, "RELATION_TARGET_FIELD_MISSING", "RELATION", BusinessPublishCheckLevel.BLOCK,
                        "目标字段不存在", "目标对象字段不存在: " + relation.getTargetFieldCode(),
                        relation.getTargetFieldCode(), null, "EDIT_RELATION", "编辑关系", "relations", 240);
            }
        }
        if (items.stream().noneMatch(item -> "RELATION".equals(item.getCategory())
                && BusinessPublishCheckLevel.BLOCK.equals(item.getLevel()))) {
            add(items, "RELATION_PASS", "RELATION", BusinessPublishCheckLevel.PASS,
                    "关系检查通过", "对象关系目标和当前对象字段有效", null, null, null, null, "relations", 290);
        }
    }

    private void checkLinkage(BusinessObjectDesignerService.DesignerContext context,
                              List<BusinessPublishCheckItemVO> items) {
        List<Map<String, Object>> rules = resolveLinkageRules(context);
        if (rules.isEmpty()) {
            add(items, "LINKAGE_PASS", "LINKAGE", BusinessPublishCheckLevel.PASS,
                    "级联检查通过", "未配置字段级联规则", null, null, null, null, "relations", 295);
            return;
        }
        Map<String, LowcodeFieldSchema> fieldMap = collectFieldMap(context.getModelSchema());
        Set<String> ruleIds = new LinkedHashSet<>();
        for (int index = 0; index < rules.size(); index++) {
            Map<String, Object> rule = rules.get(index);
            String ruleId = StringUtils.defaultIfBlank(text(rule.get("ruleId")), "第 " + (index + 1) + " 条规则");
            if (!ruleIds.add(ruleId)) {
                add(items, "LINKAGE_RULE_DUPLICATE", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                        "级联规则重复", "级联规则 ID 重复: " + ruleId, null, null,
                        "EDIT_LINKAGE", "编辑级联", "relations", 296);
            }
            if (isFalse(rule.get("enabled"))) {
                continue;
            }
            String sourceField = text(rule.get("sourceField"));
            String targetField = text(rule.get("targetField"));
            if (StringUtils.isBlank(sourceField)) {
                add(items, "LINKAGE_SOURCE_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                        "级联上级字段为空", ruleId + " 缺少上级字段", null, null,
                        "EDIT_LINKAGE", "编辑级联", "relations", 297);
            } else if (!fieldMap.containsKey(sourceField)) {
                add(items, "LINKAGE_SOURCE_MISSING", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                        "级联上级字段不存在", ruleId + " 引用了不存在的上级字段: " + sourceField, sourceField, null,
                        "EDIT_LINKAGE", "编辑级联", "relations", 298);
            }
            LowcodeFieldSchema target = null;
            if (StringUtils.isBlank(targetField)) {
                add(items, "LINKAGE_TARGET_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                        "级联目标字段为空", ruleId + " 缺少目标字段", null, null,
                        "EDIT_LINKAGE", "编辑级联", "relations", 299);
            } else {
                target = fieldMap.get(targetField);
                if (target == null) {
                    add(items, "LINKAGE_TARGET_MISSING", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                            "级联目标字段不存在", ruleId + " 引用了不存在的目标字段: " + targetField, targetField, null,
                            "EDIT_LINKAGE", "编辑级联", "relations", 300);
                }
            }
            checkLinkageRuleConfig(items, ruleId, rule, target);
        }
        if (items.stream().noneMatch(item -> "LINKAGE".equals(item.getCategory())
                && BusinessPublishCheckLevel.BLOCK.equals(item.getLevel()))) {
            add(items, "LINKAGE_PASS", "LINKAGE", BusinessPublishCheckLevel.PASS,
                    "级联检查通过", "字段级联规则引用和参数完整", null, null, null, null, "relations", 305);
        }
    }

    private void checkLinkageRuleConfig(List<BusinessPublishCheckItemVO> items, String ruleId,
                                        Map<String, Object> rule, LowcodeFieldSchema target) {
        String type = StringUtils.defaultIfBlank(text(rule.get("type")), text(rule.get("matchMode")));
        String dataSourceType = StringUtils.defaultIfBlank(text(rule.get("dataSourceType")), resolveLinkageDataSourceType(type));
        Map<String, Object> dictConfig = mapValue(rule.get("dictConfig"));
        Map<String, Object> remoteConfig = mapValue(rule.get("remoteConfig"));
        Map<String, Object> objectConfig = mapValue(rule.get("objectConfig"));
        if ("dict".equals(dataSourceType)) {
            String targetDictType = StringUtils.defaultIfBlank(text(dictConfig.get("targetDictType")),
                    target == null ? null : target.getDictType());
            if (StringUtils.isBlank(targetDictType)) {
                add(items, "LINKAGE_DICT_TARGET_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                        "目标字典类型为空", ruleId + " 缺少目标字典类型", target == null ? null : target.getField(), null,
                        "EDIT_LINKAGE", "编辑级联", "relations", 301);
            }
            if ("linkedDict".equals(type)) {
                String linkedDictType = StringUtils.defaultIfBlank(text(dictConfig.get("linkedDictType")),
                        text(dictConfig.get("sourceDictType")));
                if (StringUtils.isBlank(linkedDictType)) {
                    add(items, "LINKAGE_DICT_LINKED_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                            "关联字典类型为空", ruleId + " 缺少 linked_dict_type 匹配值", target == null ? null : target.getField(), null,
                            "EDIT_LINKAGE", "编辑级联", "relations", 302);
                }
            }
            return;
        }
        String paramName = StringUtils.defaultIfBlank(text(remoteConfig.get("paramName")), text(rule.get("paramName")));
        if (StringUtils.isBlank(paramName)) {
            add(items, "LINKAGE_REMOTE_PARAM_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                    "远程参数为空", ruleId + " 缺少请求参数名", target == null ? null : target.getField(), null,
                    "EDIT_LINKAGE", "编辑级联", "relations", 303);
        }
        if ("remote".equals(dataSourceType) && "remoteParam".equals(type)
                && StringUtils.isBlank(text(remoteConfig.get("url")))) {
            add(items, "LINKAGE_REMOTE_URL_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                    "远程接口为空", ruleId + " 缺少远程选项接口", target == null ? null : target.getField(), null,
                    "EDIT_LINKAGE", "编辑级联", "relations", 304);
        }
        if ("object".equals(dataSourceType)) {
            String targetObjectCode = StringUtils.defaultIfBlank(text(objectConfig.get("targetObjectCode")),
                    target == null ? null : target.getReferenceObjectCode());
            if (StringUtils.isBlank(targetObjectCode)) {
                add(items, "LINKAGE_OBJECT_TARGET_EMPTY", "LINKAGE", BusinessPublishCheckLevel.BLOCK,
                        "目标对象为空", ruleId + " 缺少引用目标对象", target == null ? null : target.getField(), null,
                        "EDIT_LINKAGE", "编辑级联", "relations", 306);
            }
        }
    }

    private void checkRuntimeConfig(BusinessObjectDesignerService.DesignerContext context,
                                    List<BusinessPublishCheckItemVO> items) {
        try {
            runtimeConfigBuilder.buildRuntimeConfig(resolveConfigKey(context),
                    context.getModelSchema(), context.getPageSchema());
            add(items, "RUNTIME_PASS", "RUNTIME", BusinessPublishCheckLevel.PASS,
                    "运行配置可生成", "字段和页面配置可以转换为 AiCrudPage 运行配置", null, null,
                    null, null, "publish", 300);
        } catch (Exception e) {
            add(items, "RUNTIME_INVALID", "RUNTIME", BusinessPublishCheckLevel.BLOCK,
                    "运行配置生成失败", e.getMessage(), null, null,
                    "FIX_SCHEMA", "修复配置", "publish", 310);
        }
    }

    private void checkAppEntry(BusinessObjectDesignerService.DesignerContext context,
                               List<BusinessPublishCheckItemVO> items) {
        Long tenantId = resolveTenantId(context);
        AiBusinessObject object = context.getObject();
        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());
        if (app == null) {
            add(items, "APP_ENTRY_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "应用入口未创建", "发布后需要配置业务应用入口和菜单挂载，否则用户无法从菜单进入填报页", null, null,
                    "CONFIG_APP_ENTRY", "配置入口", "publish", 320);
            return;
        }
        if (Integer.valueOf(0).equals(app.getStatus())) {
            add(items, "APP_ENTRY_DISABLED", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "应用入口已停用", "当前业务应用入口已停用，菜单点击后不会进入运行态", null, null,
                    "ENABLE_APP_ENTRY", "启用入口", "publish", 321);
        }
        if (!"RUNTIME".equalsIgnoreCase(StringUtils.defaultString(app.getEntryMode()))) {
            add(items, "APP_ENTRY_MODE_INVALID", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口打开模式不是运行态", "业务单据挂载建议使用 RUNTIME 模式，直接打开填报/列表页面", null, null,
                    "CONFIG_APP_ENTRY", "调整入口", "publish", 322);
        }
        if (StringUtils.isBlank(app.getConfigKey())) {
            add(items, "APP_ENTRY_CONFIG_EMPTY", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口缺少运行配置", "应用入口没有绑定 configKey，无法稳定打开动态填报页面", null, null,
                    "CONFIG_APP_ENTRY", "绑定运行配置", "publish", 323);
        }
        Map<String, Object> options = readAppOptions(app.getOptions());
        String entryType = StringUtils.defaultIfBlank(text(options.get("entryType")), "OBJECT_LIST").toUpperCase();
        if (!APP_ENTRY_TYPES.contains(entryType)) {
            add(items, "APP_ENTRY_TYPE_INVALID", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口类型不合法", "入口类型仅支持对象列表、新增表单、详情、审批/待办、报表/看板、外链/API", entryType, null,
                    "CONFIG_APP_ENTRY", "调整入口类型", "publish", 323);
        }
        String permissionCode = text(options.get("permissionCode"));
        if (StringUtils.isBlank(permissionCode)) {
            add(items, "APP_ENTRY_PERMISSION_EMPTY", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "入口权限码未配置", "建议为每个入口配置独立权限码，方便多个入口指向同一业务对象时分开授权", null, null,
                    "CONFIG_APP_ENTRY", "配置入口权限", "publish", 323);
        }
        Set<String> pageKeys = collectPageKeys(context.getPageSchema());
        Set<String> formKeys = collectFormKeys(readDesignerOptions(context));
        String targetPageKey = text(options.get("targetPageKey"));
        if (StringUtils.isNotBlank(targetPageKey) && !pageKeys.contains(targetPageKey)) {
            add(items, "APP_ENTRY_PAGE_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.BLOCK,
                    "入口目标页面不存在", "访问入口引用了不存在的页面: " + targetPageKey, targetPageKey, null,
                    "CONFIG_APP_ENTRY", "修复入口页面", "publish", 323);
        }
        String targetFormKey = text(options.get("targetFormKey"));
        if (StringUtils.isNotBlank(targetFormKey) && !formKeys.contains(targetFormKey)) {
            add(items, "APP_ENTRY_FORM_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.BLOCK,
                    "入口目标表单不存在", "访问入口引用了不存在的表单: " + targetFormKey, targetFormKey, null,
                    "CONFIG_APP_ENTRY", "修复入口表单", "publish", 324);
        }
        Map<String, Object> adminMenu = mapValue(options.get("adminMenu"));
        Object menuResourceId = firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId"));
        String mountTarget = StringUtils.defaultIfBlank(text(options.get("mountTarget")), "ADMIN");
        boolean syncEnabled = !isFalse(firstNonNull(adminMenu.get("syncEnabled"), options.get("adminMenuSyncEnabled")));
        if ("ADMIN".equalsIgnoreCase(mountTarget) && syncEnabled
                && (menuResourceId == null || StringUtils.isBlank(String.valueOf(menuResourceId)))) {
            add(items, "APP_MENU_MISSING", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "菜单资源未同步", "未发现应用入口的菜单资源 ID，动态菜单可能无法保持选中态", null, null,
                    "CONFIG_APP_ENTRY", "同步菜单", "publish", 324);
        }
        Object runtimeOpenModeValue = firstNonNull(options.get("runtimeOpenMode"), adminMenu.get("runtimeOpenMode"));
        String runtimeOpenMode = text(runtimeOpenModeValue);
        if (StringUtils.isNotBlank(runtimeOpenMode)
                && !RUNTIME_OPEN_MODES.contains(runtimeOpenMode.trim().toUpperCase())) {
            add(items, "APP_RUNTIME_OPEN_MODE_EMPTY", "APP_ENTRY", BusinessPublishCheckLevel.WARN,
                    "运行打开模式不合法", "运行打开模式仅支持 LIST、CREATE_FORM、DETAIL，缺省时系统按 LIST 打开", null, null,
                    "CONFIG_APP_ENTRY", "配置打开方式", "publish", 325);
        }
        Map<String, Object> defaultParams = mapValue(options.get("defaultParams"));
        if (!defaultParams.isEmpty()) {
            for (String key : defaultParams.keySet()) {
                String lowerKey = StringUtils.lowerCase(key);
                if (StringUtils.containsAny(lowerKey, "password", "passwd", "token", "secret", "apikey", "accesskey")) {
                    add(items, "APP_ENTRY_PARAM_SENSITIVE", "APP_ENTRY", BusinessPublishCheckLevel.BLOCK,
                            "入口默认参数包含敏感键", "默认参数不能保存密码、Token 或密钥类长期凭证: " + key,
                            key, null, "CONFIG_APP_ENTRY", "清理默认参数", "publish", 326);
                }
            }
        }
        if (items.stream().noneMatch(item -> "APP_ENTRY".equals(item.getCategory())
                && !BusinessPublishCheckLevel.PASS.equals(item.getLevel()))) {
            add(items, "APP_ENTRY_PASS", "APP_ENTRY", BusinessPublishCheckLevel.PASS,
                    "应用入口检查通过", "运行入口、菜单资源和打开模式已具备基础配置", null, null,
                    null, null, "publish", 329);
        }
    }

    private void checkDocumentConfig(BusinessObjectDesignerService.DesignerContext context,
                                     List<BusinessPublishCheckItemVO> items) {
        BusinessDocumentConfigVO config = documentConfigService.getConfig(context.getObject().getId());
        if (!Boolean.TRUE.equals(config.getDocumentEnabled())) {
            add(items, "DOCUMENT_DISABLED", "DOCUMENT", BusinessPublishCheckLevel.PASS,
                    "单据模式未启用", "当前对象按普通 CRUD 发布", null, null,
                    null, null, "flow", 350);
            return;
        }
        Set<String> fields = collectDocumentFields(context.getModelSchema());
        if (StringUtils.isBlank(config.getStatusField())) {
            add(items, "DOCUMENT_STATUS_EMPTY", "DOCUMENT", BusinessPublishCheckLevel.BLOCK,
                    "单据状态字段为空", "启用单据模式后必须配置状态字段", null, null,
                    "CONFIG_DOCUMENT", "配置单据", "flow", 351);
        } else if (!fields.contains(config.getStatusField())) {
            add(items, "DOCUMENT_STATUS_MISSING", "DOCUMENT", BusinessPublishCheckLevel.BLOCK,
                    "单据状态字段不存在", "状态字段不存在: " + config.getStatusField(), config.getStatusField(), null,
                    "CONFIG_DOCUMENT", "配置单据", "flow", 352);
        }
        if (StringUtils.isNotBlank(config.getStarterField()) && !fields.contains(config.getStarterField())) {
            add(items, "DOCUMENT_STARTER_MISSING", "DOCUMENT", BusinessPublishCheckLevel.BLOCK,
                    "单据发起人字段不存在", "发起人字段不存在: " + config.getStarterField(), config.getStarterField(), null,
                    "CONFIG_DOCUMENT", "配置单据", "flow", 353);
        }
        if (StringUtils.isNotBlank(config.getOwnerField()) && !fields.contains(config.getOwnerField())) {
            add(items, "DOCUMENT_OWNER_MISSING", "DOCUMENT", BusinessPublishCheckLevel.BLOCK,
                    "单据负责人字段不存在", "负责人字段不存在: " + config.getOwnerField(), config.getOwnerField(), null,
                    "CONFIG_DOCUMENT", "配置单据", "flow", 354);
        }
        if (StringUtils.isBlank(config.getNoRuleTemplate()) && StringUtils.isBlank(config.getDocumentNoRule())) {
            add(items, "DOCUMENT_NO_RULE_EMPTY", "DOCUMENT", BusinessPublishCheckLevel.WARN,
                    "编号规则未配置", "单据可运行，但建议配置编号规则以便追踪流程和消息", null, null,
                    "CONFIG_DOCUMENT", "配置编号规则", "document", 354);
        }
        if (config.getStatusMappingRows() == null || config.getStatusMappingRows().isEmpty()) {
            add(items, "DOCUMENT_STATUS_MAPPING_EMPTY", "DOCUMENT", BusinessPublishCheckLevel.BLOCK,
                    "状态映射为空", "启用单据模式后必须配置标准状态到字段值的映射", null, null,
                    "CONFIG_DOCUMENT", "配置状态映射", "document", 354);
        }
        Map<String, Object> mainFlowSummary = config.getMainFlowSummary() == null
                ? Map.of()
                : config.getMainFlowSummary();
        boolean mainFlowConfigured = Boolean.TRUE.equals(mainFlowSummary.get("configured"));
        String startMode = StringUtils.defaultIfBlank(text(mainFlowSummary.get("startMode")), "MANUAL");
        if (!mainFlowConfigured) {
            add(items, "DOCUMENT_FLOW_EMPTY", "DOCUMENT", BusinessPublishCheckLevel.WARN,
                    "主流程未配置", "单据可保存，但发起主流程前需要先在流程与自动化中配置主流程", null, null,
                    "CONFIG_FLOW", "配置流程", "flow", 355);
        } else if (!Boolean.TRUE.equals(mainFlowSummary.get("complete"))) {
            String gapText = summarizeFlowGaps(mainFlowSummary.get("gaps"));
            add(items, "DOCUMENT_FLOW_INCOMPLETE", "DOCUMENT", BusinessPublishCheckLevel.WARN,
                    "主流程配置不完整", StringUtils.defaultIfBlank(gapText, "主流程已选择，但变量映射、发起方式或按钮配置仍需补齐"), null, null,
                    "CONFIG_FLOW", "配置流程", "flow", 356);
        }
        if (requiresTrigger(startMode)) {
            Long triggerCount = triggerMapper.countActiveByObjectAndAction(
                    resolveTenantId(context), context.getObject().getObjectCode(), "START_FLOW");
            if (triggerCount == null || triggerCount <= 0) {
                add(items, "DOCUMENT_TRIGGER_MISSING", "DOCUMENT", BusinessPublishCheckLevel.WARN,
                        "自动发起触发器缺失", "主流程发起方式包含触发器，但当前对象没有启用的发起主流程触发器", null, null,
                        "CONFIG_TRIGGER", "配置触发器", "trigger", 357);
            }
        }
        if (requiresManualButton(startMode)) {
            add(items, "DOCUMENT_MANUAL_BUTTON_PASS", "DOCUMENT", BusinessPublishCheckLevel.PASS,
                    "手动发起按钮自动生成", "业务对象页面会按状态、权限和流程绑定自动生成发起主流程按钮", null, null,
                    null, null, "publish", 358);
        }
        if (items.stream().noneMatch(item -> "DOCUMENT".equals(item.getCategory())
                && !BusinessPublishCheckLevel.PASS.equals(item.getLevel()))) {
            add(items, "DOCUMENT_PASS", "DOCUMENT", BusinessPublishCheckLevel.PASS,
                    "单据检查通过", "单据状态字段和流程配置满足发布要求", null, null,
                    null, null, "flow", 359);
        }
    }

    private void checkTable(LowcodeModelSchema modelSchema, List<BusinessPublishCheckItemVO> items) {
        if (modelSchema == null || StringUtils.isBlank(modelSchema.getTableName())) {
            add(items, "TABLE_NAME_EMPTY", "TABLE", BusinessPublishCheckLevel.BLOCK,
                    "数据表缺失", "模型缺少运行数据表", null, null,
                    "ADVANCED_CONFIG", "高级配置", "advanced", 400);
            return;
        }
        try {
            if (!ddlService.tableExists(modelSchema.getTableName())) {
                boolean canOnlineDdl = hasPermission(DDL_PERMISSION);
                add(items, "TABLE_MISSING", "TABLE", canOnlineDdl ? BusinessPublishCheckLevel.WARN : BusinessPublishCheckLevel.BLOCK,
                        "数据表不存在", canOnlineDdl ? "可在发布时勾选同步表结构自动创建" : "缺少在线建表权限，请联系管理员同步表结构",
                        null, null, "SYNC_TABLE", "同步表结构", "publish", 410);
                return;
            }
            if (!ddlService.hasAutoIncrementPrimaryId(modelSchema.getTableName())) {
                add(items, "TABLE_PK_MISSING", "TABLE", BusinessPublishCheckLevel.BLOCK,
                        "主键不符合要求", "业务表必须包含 id bigint 自增主键", null, null,
                        "FIX_TABLE", "修复数据表", "advanced", 420);
                return;
            }
            List<String> retiredColumns = findRetiredBusinessColumns(modelSchema);
            if (!retiredColumns.isEmpty()) {
                add(items, "TABLE_COLUMN_RETIRED", "TABLE", BusinessPublishCheckLevel.WARN,
                        "存在已隐藏字段列", "字段已隐藏或停用，发布不会物理删除数据表列: " + String.join("、", retiredColumns),
                        null, null, "CHECK_FIELD", "检查字段", "fields", 425);
            }

            LowcodeDdlPreviewVO preview = ddlService.previewCreateTable(modelSchema);
            List<String> ddlStatements = preview.getDdlStatements();
            if (ddlStatements != null && !ddlStatements.isEmpty()) {
                boolean canOnlineDdl = hasPermission(DDL_PERMISSION);
                String itemCode = resolveTableSyncItemCode(ddlStatements);
                add(items, itemCode, "TABLE", canOnlineDdl ? BusinessPublishCheckLevel.WARN : BusinessPublishCheckLevel.BLOCK,
                        "数据表结构未同步",
                        canOnlineDdl ? "发布时勾选同步数据表结构后自动执行受控变更: " + summarizeDdlStatements(ddlStatements)
                                : "数据表结构与字段配置不一致且当前用户无在线同步权限: " + summarizeDdlStatements(ddlStatements),
                        null, null, "SYNC_TABLE", "同步表结构", canOnlineDdl ? "publish" : "advanced", 430);
                if (!canOnlineDdl) {
                    return;
                }
            }
            add(items, "TABLE_PASS", "TABLE", BusinessPublishCheckLevel.PASS,
                    "数据表检查通过", "数据表存在且主键符合低代码运行要求", null, null, null, null, "publish", 490);
        } catch (Exception e) {
            add(items, "TABLE_CHECK_WARN", "TABLE", BusinessPublishCheckLevel.WARN,
                    "数据表检查未完成", "当前环境无法完成数据表检查: " + e.getMessage(), null, null,
                    "CHECK_DATABASE", "检查数据库", "advanced", 430);
        }
    }

    private String summarizeFlowGaps(Object gaps) {
        if (gaps instanceof List<?> list && !list.isEmpty()) {
            return "主流程缺口: " + String.join("、", list.stream()
                    .map(String::valueOf)
                    .filter(StringUtils::isNotBlank)
                    .toList());
        }
        return null;
    }

    private boolean requiresTrigger(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        return "TRIGGER".equals(normalized)
                || "BOTH".equals(normalized)
                || "MANUAL_AND_TRIGGER".equals(normalized)
                || "MANUAL_TRIGGER".equals(normalized);
    }

    private boolean requiresManualButton(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        return "MANUAL".equals(normalized)
                || "BOTH".equals(normalized)
                || "MANUAL_AND_TRIGGER".equals(normalized)
                || "MANUAL_TRIGGER".equals(normalized);
    }

    private void checkPermissionSummary(BusinessObjectDesignerService.DesignerContext context,
                                        List<BusinessPublishCheckItemVO> items) {
        BusinessPermissionSummaryVO summary = permissionService.documentActionSummary(context.getObject().getId());
        List<String> missingRequired = summary.getActionPermissions().stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequired()) && !Boolean.TRUE.equals(item.getConfigured()))
                .map(BusinessPermissionSummaryVO.ActionPermissionVO::getActionName)
                .toList();
        if (!missingRequired.isEmpty()) {
            add(items, "PERMISSION_ACTION_MISSING", "PERMISSION", BusinessPublishCheckLevel.WARN,
                    "关键按钮权限未配置", "缺少动作权限资源: " + String.join("、", missingRequired),
                    null, null, "CONFIG_PERMISSION", "配置权限", "permission", 360);
            return;
        }
        add(items, "PERMISSION_ACTION_PASS", "PERMISSION", BusinessPublishCheckLevel.PASS,
                "按钮权限检查通过", "保存、提交、流程、触发器和报表动作权限已有摘要", null, null,
                null, null, "permission", 369);
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

    private List<String> findRetiredBusinessColumns(LowcodeModelSchema modelSchema) {
        if (modelSchema.getFields() == null) {
            return List.of();
        }
        Set<String> existingColumns = ddlService.listColumns(modelSchema.getTableName());
        return modelSchema.getFields().stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()))
                .filter(field -> "DISABLED".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus()))
                        || "HIDDEN".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus())))
                .map(LowcodeFieldSchema::getColumnName)
                .filter(StringUtils::isNotBlank)
                .filter(column -> !isSystemColumn(column))
                .filter(existingColumns::contains)
                .distinct()
                .toList();
    }

    private String resolveTableSyncItemCode(List<String> ddlStatements) {
        boolean hasModify = ddlStatements.stream().anyMatch(ddl -> StringUtils.containsIgnoreCase(ddl, " MODIFY COLUMN "));
        if (hasModify) {
            return "TABLE_COLUMN_CHANGED";
        }
        boolean hasAdd = ddlStatements.stream().anyMatch(ddl -> StringUtils.containsIgnoreCase(ddl, " ADD COLUMN "));
        if (hasAdd) {
            return "TABLE_COLUMN_MISSING";
        }
        return "TABLE_INDEX_MISSING";
    }

    private String summarizeDdlStatements(List<String> ddlStatements) {
        List<String> summary = ddlStatements.stream()
                .map(this::summarizeDdlStatement)
                .filter(StringUtils::isNotBlank)
                .limit(6)
                .toList();
        String suffix = ddlStatements.size() > summary.size() ? " 等 " + ddlStatements.size() + " 项" : "";
        return String.join("、", summary) + suffix;
    }

    private String summarizeDdlStatement(String ddl) {
        if (StringUtils.isBlank(ddl)) {
            return "";
        }
        String normalized = ddl.toUpperCase();
        if (normalized.contains(" ADD COLUMN ")) {
            return "新增列 " + extractBacktickValueAfter(ddl, "ADD COLUMN");
        }
        if (normalized.contains(" MODIFY COLUMN ")) {
            return "修改列 " + extractBacktickValueAfter(ddl, "MODIFY COLUMN");
        }
        if (normalized.contains(" ADD UNIQUE KEY ")) {
            return "新增唯一索引 " + extractBacktickValueAfter(ddl, "ADD UNIQUE KEY");
        }
        if (normalized.contains(" ADD KEY ")) {
            return "新增索引 " + extractBacktickValueAfter(ddl, "ADD KEY");
        }
        return ddl.length() > 80 ? ddl.substring(0, 80) + "..." : ddl;
    }

    private String extractBacktickValueAfter(String ddl, String marker) {
        String upper = ddl.toUpperCase();
        int markerIndex = upper.indexOf(marker);
        if (markerIndex < 0) {
            return "";
        }
        int start = ddl.indexOf('`', markerIndex + marker.length());
        int end = ddl.indexOf('`', start + 1);
        if (start < 0 || end <= start) {
            return "";
        }
        return "`" + ddl.substring(start + 1, end) + "`";
    }

    private boolean isSystemColumn(String columnName) {
        return "id".equals(columnName)
                || "tenant_id".equals(columnName)
                || "create_by".equals(columnName)
                || "create_time".equals(columnName)
                || "create_dept".equals(columnName)
                || "update_by".equals(columnName)
                || "update_time".equals(columnName)
                || "del_flag".equals(columnName);
    }

    private void add(List<BusinessPublishCheckItemVO> items, String code, String category, String level,
                     String title, String message, String fieldCode, String zoneKey, String fixAction,
                     String fixActionLabel, String fixTarget, Integer sortOrder) {
        BusinessPublishCheckItemVO item = new BusinessPublishCheckItemVO();
        item.setItemCode(code);
        item.setCategory(category);
        item.setLevel(normalizePublishCheckLevel(code, level));
        item.setTitle(title);
        item.setMessage(message);
        item.setFieldCode(fieldCode);
        item.setZoneKey(zoneKey);
        item.setFixAction(fixAction);
        item.setFixActionLabel(fixActionLabel);
        item.setFixTarget(fixTarget);
        item.setSortOrder(sortOrder);
        items.add(item);
    }

    private String normalizePublishCheckLevel(String code, String level) {
        if (!BusinessPublishCheckLevel.BLOCK.equals(level)) {
            return level;
        }
        return REQUIRED_BLOCK_ITEM_CODES.contains(code) ? level : BusinessPublishCheckLevel.WARN;
    }

    private List<Map<String, Object>> resolveLinkageRules(BusinessObjectDesignerService.DesignerContext context) {
        List<Map<String, Object>> rules = new ArrayList<>(readLinkageRulesFromDesignerOptions(context));
        Set<String> existingKeys = new LinkedHashSet<>();
        rules.forEach(rule -> existingKeys.add(linkageKey(rule)));
        if (context.getModelSchema() != null && context.getModelSchema().getFields() != null) {
            for (LowcodeFieldSchema field : context.getModelSchema().getFields()) {
                Map<String, Object> cascade = mapValue(field == null || field.getBasicProps() == null
                        ? null : field.getBasicProps().get("cascade"));
                if (cascade.isEmpty() || isFalse(cascade.get("enabled"))) {
                    continue;
                }
                String sourceField = text(cascade.get("sourceField"));
                String targetField = field.getField();
                if (StringUtils.isBlank(sourceField) || StringUtils.isBlank(targetField)) {
                    continue;
                }
                Map<String, Object> rule = buildLinkageRuleFromCascade(field, cascade);
                String key = linkageKey(rule);
                if (existingKeys.add(key)) {
                    rules.add(rule);
                }
            }
        }
        return rules;
    }

    private List<Map<String, Object>> readLinkageRulesFromDesignerOptions(
            BusinessObjectDesignerService.DesignerContext context) {
        Map<String, Object> designerOptions = readDesignerOptions(context);
        try {
            Object value = designerOptions.get(LINKAGE_SCHEMA_OPTION_KEY);
            Map<String, Object> schema;
            if (value instanceof String text && StringUtils.isNotBlank(text)) {
                schema = objectMapper.readValue(text, new TypeReference<>() {});
            } else {
                schema = mapValue(value);
            }
            Object rules = schema.get("rules");
            if (rules instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> mapValue(item))
                        .toList();
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
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

    private Map<String, Object> buildLinkageRuleFromCascade(LowcodeFieldSchema field, Map<String, Object> cascade) {
        String mode = StringUtils.defaultIfBlank(text(cascade.get("mode")), text(cascade.get("matchMode")));
        String type = StringUtils.defaultIfBlank(text(cascade.get("type")), mode);
        if (StringUtils.isBlank(type)) {
            type = "linkedDict";
        }
        Map<String, Object> rule = new LinkedHashMap<>();
        String sourceField = text(cascade.get("sourceField"));
        rule.put("ruleId", StringUtils.defaultIfBlank(text(cascade.get("ruleId")),
                "linkage_" + sourceField + "_" + field.getField()));
        rule.put("type", type);
        rule.put("sourceField", sourceField);
        rule.put("targetField", field.getField());
        rule.put("dataSourceType", resolveLinkageDataSourceType(type));
        rule.put("matchMode", type);
        rule.put("dictConfig", buildCascadeDictConfig(field, cascade));
        rule.put("remoteConfig", buildCascadeRemoteConfig(cascade));
        rule.put("objectConfig", buildCascadeObjectConfig(field, cascade));
        rule.put("emptyStrategy", StringUtils.defaultIfBlank(text(cascade.get("emptyStrategy")), "empty"));
        rule.put("clearOnSourceChange", !isFalse(cascade.get("clearOnSourceChange"))
                && !isFalse(cascade.get("clearOnParentChange")));
        rule.put("enabled", !isFalse(cascade.get("enabled")));
        return rule;
    }

    private Map<String, Object> buildCascadeDictConfig(LowcodeFieldSchema field, Map<String, Object> cascade) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("sourceDictType", text(cascade.get("sourceDictType")));
        config.put("targetDictType", StringUtils.defaultIfBlank(text(cascade.get("targetDictType")),
                field.getDictType()));
        config.put("linkedDictType", text(cascade.get("linkedDictType")));
        return config;
    }

    private Map<String, Object> buildCascadeRemoteConfig(Map<String, Object> cascade) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("url", text(cascade.get("url")));
        config.put("method", StringUtils.defaultIfBlank(text(cascade.get("method")), "GET"));
        config.put("paramName", StringUtils.defaultIfBlank(text(cascade.get("paramName")),
                text(cascade.get("sourceField"))));
        return config;
    }

    private Map<String, Object> buildCascadeObjectConfig(LowcodeFieldSchema field, Map<String, Object> cascade) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("targetObjectCode", StringUtils.defaultIfBlank(text(cascade.get("targetObjectCode")),
                field.getReferenceObjectCode()));
        config.put("displayField", StringUtils.defaultIfBlank(text(cascade.get("displayField")),
                field.getReferenceDisplayField()));
        return config;
    }

    private String linkageKey(Map<String, Object> rule) {
        return text(rule.get("sourceField")) + "->" + text(rule.get("targetField"));
    }

    private String resolveLinkageDataSourceType(String type) {
        if ("parentDictCode".equals(type) || "linkedDict".equals(type)) {
            return "dict";
        }
        if ("orgScope".equals(type)) {
            return "org";
        }
        if ("objectReference".equals(type)) {
            return "object";
        }
        return "remote";
    }

    private Map<String, LowcodeFieldSchema> collectFieldMap(LowcodeModelSchema modelSchema) {
        Map<String, LowcodeFieldSchema> fields = new LinkedHashMap<>();
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field != null && StringUtils.isNotBlank(field.getField())) {
                    fields.put(field.getField(), field);
                }
            }
        }
        return fields;
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

    private Set<String> collectDocumentFields(LowcodeModelSchema modelSchema) {
        Set<String> fields = new LinkedHashSet<>(collectFields(modelSchema));
        fields.addAll(Set.of("id", "tenantId", "tenant_id", "createBy", "create_by", "createTime", "create_time",
                "createDept", "create_dept", "updateBy", "update_by", "updateTime", "update_time"));
        if (modelSchema != null && modelSchema.getFields() != null) {
            for (LowcodeFieldSchema field : modelSchema.getFields()) {
                if (field == null) {
                    continue;
                }
                if (StringUtils.isNotBlank(field.getColumnName())) {
                    fields.add(field.getColumnName());
                    fields.add(snakeToCamel(field.getColumnName()));
                }
            }
        }
        return fields;
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

    private Map<String, Object> readAppOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(options, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
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

    private boolean isFalse(Object value) {
        return Boolean.FALSE.equals(value) || "false".equalsIgnoreCase(text(value)) || "0".equals(text(value));
    }

    private boolean hasPermission(String permission) {
        try {
            return SessionHelper.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveConfigKey(BusinessObjectDesignerService.DesignerContext context) {
        if (context.getConfig() != null && StringUtils.isNotBlank(context.getConfig().getConfigKey())) {
            return context.getConfig().getConfigKey();
        }
        return StringUtils.defaultString(context.getObject().getSuiteCode())
                + "_" + StringUtils.defaultString(context.getObject().getObjectCode());
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
        List<FormulaObjectDependencyAnalyzer.ObjectContext> contexts = new ArrayList<>();
        addFormulaObjectContext(contexts, context);
        if (context == null || context.getObject() == null || StringUtils.isBlank(context.getObject().getSuiteCode())) {
            return contexts;
        }

        List<AiBusinessObject> suiteObjects = businessObjectMapper.selectBySuiteCode(
                resolveTenantId(context), context.getObject().getSuiteCode());
        if (suiteObjects == null || suiteObjects.isEmpty()) {
            return contexts;
        }

        Long currentObjectId = context.getObject().getId();
        for (AiBusinessObject object : suiteObjects) {
            if (object == null || object.getId() == null || object.getId().equals(currentObjectId)) {
                continue;
            }
            try {
                addFormulaObjectContext(contexts, designerService.loadContext(object.getId()));
            } catch (Exception ignored) {
                // Missing unrelated design context should not hide current object's own formula errors.
            }
        }
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
