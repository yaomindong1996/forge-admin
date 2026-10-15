package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessSchemaValidator;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessValidationContext;
import com.mdframe.forge.plugin.generator.businessprocess.validation.BusinessProcessValidationContextResolver;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishCheckVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessIssueVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableFieldMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessValidationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 应用级发布门禁，聚合对象、数据库、入口、流程、扩展和权限状态。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationReadinessService {

    private static final String BLOCK = "BLOCK";
    private static final String WARN = "WARN";

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationAssetSelectionService selectionService;
    private final BusinessObjectPublishService objectPublishService;
    private final BusinessPermissionService permissionService;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessApplicationPageDependencyInspector pageDependencyInspector;
    private final BusinessObjectTableMappingService tableMappingService;
    private final BusinessProcessSchemaValidator processSchemaValidator;
    private final BusinessProcessValidationContextResolver processValidationContextResolver;

    public BusinessApplicationReadinessVO check(Long applicationId) {
        BusinessApplicationVO application = applicationService.publishContext(applicationId);
        return evaluate(application, selectionService.resolveContext(applicationId, null)).readiness();
    }

    public BusinessApplicationPublishCheckVO publishCheck(Long applicationId, BusinessApplicationPublishDTO dto) {
        return resolvePublishCheck(applicationId, dto).check();
    }

    ResolvedPublishCheck resolvePublishCheck(Long applicationId, BusinessApplicationPublishDTO dto) {
        return resolvePublishCheck(applicationId, dto, false);
    }

    /**
     * 应用协调发布专用门禁：只拦应用本身不可发布的问题，不做对象/页面/权限/流程深检。
     */
    ResolvedPublishCheck resolveStatusPublishCheck(Long applicationId, BusinessApplicationPublishDTO dto) {
        return resolvePublishCheck(applicationId, dto, true);
    }

    private ResolvedPublishCheck resolvePublishCheck(Long applicationId,
                                                     BusinessApplicationPublishDTO dto,
                                                     boolean statusOnly) {
        BusinessApplicationVO application = applicationService.publishContext(applicationId);
        BusinessApplicationAssetSelectionService.ResolvedSelection resolved
                = selectionService.resolveContext(applicationId, dto);
        BusinessApplicationAssetSelectionVO selection = resolved.selection();
        EvaluationResult evaluation = statusOnly
                ? evaluateStatusOnly(application, resolved)
                : evaluate(application, resolved);
        BusinessApplicationReadinessVO readiness = evaluation.readiness();
        BusinessApplicationPublishCheckVO result = new BusinessApplicationPublishCheckVO();
        result.setApplicationId(applicationId);
        result.setApplicationCode(application.getApplicationCode());
        result.setPublishable(readiness.getReady());
        result.setStatus(readiness.getStatus());
        result.setBlockingCount(readiness.getBlockingCount());
        result.setWarningCount(readiness.getWarningCount());
        result.setIssues(readiness.getIssues());
        result.setSelection(selection);
        return new ResolvedPublishCheck(
                result, application, resolved, evaluation.permissionSummaries(), evaluation.bindings(),
                evaluation.objectContexts());
    }

    /** 状态发布门禁：应用启用、业务域、门户地址。 */
    private EvaluationResult evaluateStatusOnly(
            BusinessApplicationVO application,
            BusinessApplicationAssetSelectionService.ResolvedSelection resolved) {
        List<BusinessApplicationReadinessIssueVO> issues = new ArrayList<>();
        if (!EnableStatus.ENABLED.matches(application.getStatus())) {
            issues.add(issue("APPLICATION_DISABLED", BLOCK, "应用已停用",
                    "停用应用不能发布，请先启用应用。", "overview", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        if (StringUtils.isBlank(application.getSuiteName())) {
            issues.add(issue("SUITE_UNAVAILABLE", BLOCK, "所属业务域不可用",
                    "业务域不存在、已删除或当前租户无权访问。", "overview", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        checkPortalConfiguration(application, issues);
        return new EvaluationResult(buildReadiness(issues), List.of(), null, Map.of());
    }

    private EvaluationResult evaluate(
            BusinessApplicationVO application,
            BusinessApplicationAssetSelectionService.ResolvedSelection resolved) {
        Long applicationId = application.getId();
        BusinessApplicationAssetSelectionVO selection = resolved.selection();
        List<BusinessApplicationObjectVO> allObjects = resolved.objects();
        Set<Long> selectedObjects = new HashSet<>(selection.getObjectIds());
        Set<Long> selectedExtensions = new HashSet<>(selection.getExtensionIds());
        List<BusinessApplicationReadinessIssueVO> issues = new ArrayList<>();
        List<BusinessApplicationObjectVO> selectedObjectList = allObjects.stream()
                .filter(object -> selectedObjects.contains(object.getObjectId())).toList();

        if (!EnableStatus.ENABLED.matches(application.getStatus())) {
            issues.add(issue("APPLICATION_DISABLED", BLOCK, "应用已停用",
                    "停用应用不能发布，请先启用应用。", "overview", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        if (StringUtils.isBlank(application.getSuiteName())) {
            issues.add(issue("SUITE_UNAVAILABLE", BLOCK, "所属业务域不可用",
                    "业务域不存在、已删除或当前租户无权访问。", "overview", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        checkPortalConfiguration(application, issues);
        BusinessApplicationPageDependencyInspector.InspectionResult dependencyInspection
                = pageDependencyInspector.inspect(application, selectedObjectList);
        dependencyInspection.issues().forEach(item -> {
            // 页面对象引用属于页面级问题：发布只提醒，不阻断应用状态切换。
            boolean pageLevel = item.code() != null && item.code().startsWith("PAGE_");
            issues.add(issue(
                    item.code(), pageLevel ? WARN : BLOCK, item.title(), item.message(),
                    "objects", "objects", pageLevel ? "PAGE" : "APPLICATION",
                    application.getId(), item.pageId()));
        });
        Map<Long, com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp> selectedEntries
                = resolved.entries().stream()
                .filter(entry -> selection.getEntryIds().contains(entry.getId()))
                .collect(java.util.stream.Collectors.toMap(
                        com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp::getId,
                        java.util.function.Function.identity()));
        if (selection.getEntryIds().isEmpty() || selectedEntries.values().stream()
                .noneMatch(entry -> EnableStatus.ENABLED.matches(entry.getStatus()))) {
            issues.add(issue("ACTIVE_ENTRY_MISSING", WARN, "尚未配置页面入口",
                    "页面入口不是发布必需项；应用仍可发布对象、预览草稿或生成代码。", "entries", "entries", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        selectedEntries.values().forEach(entry -> {
            if (!EnableStatus.ENABLED.matches(entry.getStatus())) {
                issues.add(issue("ENTRY_DISABLED", BLOCK, "发布入口已停用",
                        entry.getAppName() + " 当前处于停用状态。", "entries", "entries", "ENTRY",
                        entry.getId(), entry.getAppCode()));
            } else if ("RUNTIME".equalsIgnoreCase(entry.getEntryMode())
                    && StringUtils.isBlank(entry.getConfigKey())) {
                issues.add(issue("ENTRY_RUNTIME_CONFIG_MISSING", BLOCK, "运行入口缺少发布配置",
                        entry.getAppName() + " 尚未关联已发布页面配置。", "entries", "entries", "ENTRY",
                        entry.getId(), entry.getAppCode()));
            }
        });

        List<BusinessPermissionSummaryVO> permissionSummaries = permissionService
                .documentActionSummaries(selectedObjectList);
        Map<Long, BusinessPermissionSummaryVO> permissions = permissionSummaries.stream()
                .collect(Collectors.toMap(BusinessPermissionSummaryVO::getObjectId, Function.identity()));
        Map<Long, BusinessObjectDesignerService.DesignerContext> objectContexts = new HashMap<>();
        for (BusinessApplicationObjectVO object : selectedObjectList) {
            checkObject(object, permissions.get(object.getObjectId()), issues, objectContexts);
        }
        for (AiBusinessExtension extension : resolved.extensions()) {
            boolean selected = selectedExtensions.contains(extension.getId());
            if (BusinessExtensionStatus.DRAFT.matches(extension.getStatus())) {
                issues.add(issue("EXTENSION_UNTESTED", selected ? BLOCK : WARN,
                        selected ? "扩展尚未测试" : "未测试扩展已跳过",
                        extension.getExtensionName() + (selected
                                ? " 的当前草稿未通过受限测试。"
                                : " 仍保留为草稿，本次发布不会包含该扩展。"),
                        "enhancements", "enhancements", "EXTENSION", extension.getId(), extension.getExtensionCode()));
            } else if (selected && BusinessExtensionStatus.ENABLED.matches(extension.getStatus())
                    && !java.util.Objects.equals(extension.getDraftVersion(), extension.getEnabledVersion())) {
                issues.add(issue("EXTENSION_VERSION_MISMATCH", BLOCK, "扩展运行版本落后",
                        extension.getExtensionName() + " 的当前草稿尚未启用。",
                        "enhancements", "enhancements", "EXTENSION", extension.getId(), extension.getExtensionCode()));
            }
        }

        checkProcesses(application, resolved, selection, issues);

        List<AiBusinessBinding> bindings = null;
        if (Boolean.TRUE.equals(selection.getIncludeAutomation())) {
            bindings = bindingMapper.selectByApplication(resolveTenantId(), applicationId);
            if (selection.getProcessIds().isEmpty()) {
                issues.add(issue("PROCESS_OPTIONAL", WARN, "尚未配置应用级业务流程",
                        "业务流程不是发布必需项；需要审批或自动流转时可在业务流程画布中配置。",
                        "automation", "automation", "APPLICATION", applicationId, application.getApplicationCode()));
            }
        }
        return new EvaluationResult(buildReadiness(issues), permissionSummaries, bindings, objectContexts);
    }

    private void checkPortalConfiguration(
            BusinessApplicationVO application,
            List<BusinessApplicationReadinessIssueVO> issues) {
        try {
            if (!applicationService.slugAvailable(application.getPortalSlug(), application.getId())) {
                issues.add(issue("PORTAL_SLUG_CONFLICT", BLOCK, "门户访问地址已被占用",
                        "请在应用设置中更换门户访问地址。", "settings", "settings", "APPLICATION",
                        application.getId(), application.getPortalSlug()));
            }
        } catch (BusinessException e) {
            issues.add(issue("PORTAL_SLUG_INVALID", BLOCK, "门户访问地址不合法",
                    e.getMessage(), "settings", "settings", "APPLICATION",
                    application.getId(), application.getPortalSlug()));
        }

        JSONObject portalConfig = parseObject(application.getPortalConfig());
        JSONObject watermark = portalConfig.getJSONObject("watermark");
        if (watermark != null && StringUtils.length(watermark.getString("text")) > 50) {
            issues.add(issue("PORTAL_WATERMARK_TOO_LONG", BLOCK, "门户水印文字过长",
                    "水印文字不能超过 50 个字符。", "settings", "settings", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }

        JSONObject assistant = parseObject(application.getAiAssistantConfig());
        if (!Boolean.TRUE.equals(assistant.getBoolean("enabled"))) {
            return;
        }
        if (assistant.get("agentId") == null && StringUtils.isBlank(assistant.getString("agentCode"))) {
            issues.add(issue("AI_ASSISTANT_MISSING", BLOCK, "AI 助理尚未绑定",
                    "启用应用 AI 助理前必须选择已有助理。", "releases", "releases", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        Set<String> publishedPageIds = applicationPageIds(application.getOptions());
        JSONArray pageIds = assistant.getJSONArray("pageIds");
        if (pageIds == null) {
            return;
        }
        List<String> missingPageIds = pageIds.stream()
                .map(String::valueOf)
                .filter(pageId -> !publishedPageIds.contains(pageId))
                .toList();
        if (!missingPageIds.isEmpty()) {
            issues.add(issue("AI_ASSISTANT_PAGE_MISSING", BLOCK, "AI 助理页面范围无效",
                    "以下页面已不存在或未纳入当前版本：" + String.join("、", missingPageIds),
                    "releases", "releases", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
    }

    private Set<String> applicationPageIds(String optionsJson) {
        JSONObject options = parseObject(optionsJson);
        JSONObject builder = options.getJSONObject("inAppBuilder");
        JSONArray nodes = builder == null ? null : builder.getJSONArray("nodes");
        if (nodes == null) {
            return Set.of();
        }
        return nodes.stream()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .filter(node -> "page".equalsIgnoreCase(node.getString("type")))
                .map(node -> node.getString("id"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private JSONObject parseObject(String value) {
        if (StringUtils.isBlank(value)) {
            return new JSONObject();
        }
        try {
            JSONObject object = JSON.parseObject(value);
            return object == null ? new JSONObject() : object;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private void checkProcesses(
            BusinessApplicationVO application,
            BusinessApplicationAssetSelectionService.ResolvedSelection resolved,
            BusinessApplicationAssetSelectionVO selection,
            List<BusinessApplicationReadinessIssueVO> issues) {
        if (!Boolean.TRUE.equals(selection.getIncludeAutomation())) {
            return;
        }
        Set<Long> selectedIds = new HashSet<>(selection.getProcessIds());
        for (AiBusinessProcess process : resolved.processes()) {
            if (!selectedIds.contains(process.getId())) {
                continue;
            }
            String processName = StringUtils.defaultIfBlank(
                    process.getProcessName(), process.getProcessCode());
            if (!EnableStatus.ENABLED.matches(process.getStatus())) {
                issues.add(issue("PROCESS_DISABLED", BLOCK, "业务流程已停用",
                        processName + " 当前处于停用状态。", "automation", "automation", "PROCESS",
                        process.getId(), process.getProcessCode()));
                continue;
            }
            BusinessProcessSchema schema;
            try {
                schema = processSchemaValidator.normalize(process.getDraftSchemaJson());
            } catch (IllegalArgumentException exception) {
                issues.add(issue("PROCESS_SCHEMA_INVALID", BLOCK, "业务流程协议无效",
                        processName + "：" + StringUtils.abbreviate(exception.getMessage(), 300),
                        "automation", "automation", "PROCESS", process.getId(), process.getProcessCode()));
                continue;
            }
            BusinessProcessValidationContext context;
            try {
                context = processValidationContextResolver.resolve(
                        resolveTenantId(), application.getId(), process.getProcessCode(), schema);
            } catch (RuntimeException exception) {
                issues.add(issue("PROCESS_DEPENDENCY_CHECK_FAILED", BLOCK, "业务流程依赖检查失败",
                        processName + "：无法解析当前应用的受治理依赖。",
                        "automation", "automation", "PROCESS", process.getId(), process.getProcessCode()));
                continue;
            }
            BusinessProcessValidationVO validation = processSchemaValidator.validate(schema, context);
            validation.getIssues().stream()
                    .filter(item -> "ERROR".equals(item.getLevel()))
                    .forEach(item -> issues.add(issue(
                            "PROCESS_" + item.getCode(), BLOCK, "业务流程发布检查未通过",
                            processName + "：" + item.getMessage(),
                            "automation", "automation", "PROCESS", process.getId(), process.getProcessCode())));
        }
    }

    private void checkObject(BusinessApplicationObjectVO object,
                             BusinessPermissionSummaryVO permissionSummary,
                             List<BusinessApplicationReadinessIssueVO> issues,
                             Map<Long, BusinessObjectDesignerService.DesignerContext> objectContexts) {
        String objectName = StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode());
        if (!Integer.valueOf(1).equals(object.getObjectStatus())) {
            issues.add(issue("OBJECT_DISABLED", BLOCK, "业务对象已停用",
                    objectName + " 当前处于停用状态。", "objects", "objects", "OBJECT",
                    object.getObjectId(), object.getObjectCode()));
            return;
        }
        if (StringUtils.isBlank(object.getTableName())) {
            issues.add(issue("OBJECT_TABLE_MISSING", BLOCK, "业务对象缺少物理表",
                    objectName + " 尚未绑定可发布的物理表。", "objects", "objects", "OBJECT",
                    object.getObjectId(), object.getObjectCode()));
        }
        checkDatabaseMapping(object, objectName, issues);
        // 应用发布只做应用级门禁；对象级页面/公式/单据等 publishCheck 不在此执行。
        // 未发布对象仍加载上下文，供后续 OBJECTS 步骤复用；已 PUBLISHED 对象正式步骤会跳过。
        if (!BusinessObjectDesignStatus.PUBLISHED.matches(object.getDesignStatus())) {
            objectContexts.put(object.getObjectId(),
                    objectPublishService.loadContextForApplicationPublish(object.getObjectId()));
        }
        if (permissionSummary != null && Boolean.FALSE.equals(permissionSummary.getAllRequiredConfigured())) {
            issues.add(issue("OBJECT_PERMISSION_MISSING", BLOCK, "对象必需权限未配置",
                    objectName + " 缺少必需的查看、保存、提交或流程权限资源。",
                    "permissions", "permissions", "OBJECT", object.getObjectId(), object.getObjectCode()));
        }
        if (value(object.getSharedApplicationCount()) > 1L
                && !"PUBLISHED".equalsIgnoreCase(object.getDesignStatus())) {
            issues.add(issue("SHARED_OBJECT_CHANGED", WARN, "共享对象变更影响提醒",
                    objectName + " 被 " + object.getSharedApplicationCount()
                            + " 个应用复用，本次发布不会因此阻断，请按需评估其他应用的影响。",
                    "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
        }
    }

    private void checkDatabaseMapping(
            BusinessApplicationObjectVO object,
            String objectName,
            List<BusinessApplicationReadinessIssueVO> issues) {
        // 仅当「状态=IN_SYNC 且同步时的设计版本仍等于当前草稿版本」才信任摘要，避免改字段后
        // 仍拿旧 IN_SYNC 跳过探库，导致预览已一致/预检放行、发布 OBJECTS 又因 DDL 失败。
        if (canTrustCachedDatabaseInSync(object)) {
            return;
        }
        BusinessObjectTableMappingVO mapping;
        try {
            mapping = tableMappingService.getTableMapping(object.getObjectId());
        } catch (RuntimeException e) {
            issues.add(issue("OBJECT_DATABASE_CHECK_FAILED", BLOCK, "数据库结构检查失败",
                    objectName + "：" + safeMessage(e),
                    "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
            return;
        }
        String syncStatus = StringUtils.defaultString(mapping.getSyncStatus(), "UNKNOWN").toUpperCase();
        if ("IN_SYNC".equals(syncStatus)) {
            return;
        }
        if ("UNKNOWN".equals(syncStatus)) {
            issues.add(issue("OBJECT_DATABASE_UNKNOWN", WARN, "数据库同步状态未知",
                    objectName + " 尚无可用的实时结构检查结果，请在发布前复核。",
                    "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
            return;
        }
        if ("CHECK_FAILED".equals(syncStatus) || "FAILED".equals(syncStatus)) {
            issues.add(issue("OBJECT_DATABASE_CHECK_FAILED", BLOCK, "数据库结构检查失败",
                    objectName + "：" + StringUtils.defaultIfBlank(
                            mapping.getLastSyncMessage(), "无法读取目标数据库结构"),
                    "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
            return;
        }
        issues.add(issue("OBJECT_DATABASE_OUT_OF_SYNC", BLOCK, "数据库结构未同步",
                databaseDiffMessage(objectName, mapping),
                "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
    }

    /**
     * 摘要 IN_SYNC 必须与当前设计版本对齐；缺版本号、版本漂移或 PARTIAL/FAILED 一律实时探库。
     */
    private boolean canTrustCachedDatabaseInSync(BusinessApplicationObjectVO object) {
        if (object == null || !"IN_SYNC".equalsIgnoreCase(StringUtils.trimToEmpty(object.getSyncStatus()))) {
            return false;
        }
        Integer currentVersion = object.getDesignVersion();
        Integer syncedVersion = object.getSyncDesignVersion();
        return currentVersion != null && currentVersion.equals(syncedVersion);
    }

    private String databaseDiffMessage(String objectName, BusinessObjectTableMappingVO mapping) {
        if ("TABLE_MISSING".equalsIgnoreCase(mapping.getSyncStatus())
                || Boolean.FALSE.equals(mapping.getTableExists())) {
            return objectName + "：目标表 "
                    + StringUtils.defaultIfBlank(mapping.getTableName(), "未配置") + " 不存在。";
        }
        List<String> details = new ArrayList<>();
        for (BusinessObjectTableFieldMappingVO field : safeFields(mapping)) {
            if (!Boolean.TRUE.equals(field.getBlockingDifference())) {
                continue;
            }
            if (details.size() >= 3) {
                break;
            }
            String columnName = StringUtils.defaultIfBlank(field.getColumnName(), field.getFieldCode());
            String fieldName = StringUtils.defaultIfBlank(field.getBusinessName(), columnName);
            if ("MISSING_DATABASE_COLUMN".equals(field.getSyncStatus())) {
                details.add("缺少数据库列 " + columnName);
            } else if ("TYPE_MISMATCH".equals(field.getSyncStatus())) {
                details.add(fieldName + "类型不一致（设计 " + configuredType(field)
                        + "，数据库 " + StringUtils.defaultIfBlank(field.getDatabaseType(), "未知") + "）");
            } else if ("UNMAPPED_DATABASE_COLUMN".equals(field.getSyncStatus())) {
                details.add("存在未映射业务列 " + columnName
                        + "（数据库为必填且无默认值，请添加字段映射或调整数据库默认值）");
            }
        }
        int pendingDdlCount = mapping.getPendingDdlCount() == null ? 0 : mapping.getPendingDdlCount();
        if (details.size() < 3 && pendingDdlCount > 0) {
            details.add("待执行 " + pendingDdlCount + " 条数据库变更");
        }
        if (details.isEmpty()) {
            details.add("实时检查发现未同步的数据库结构差异");
        }
        return objectName + "：" + String.join("；", details) + "。";
    }

    private List<BusinessObjectTableFieldMappingVO> safeFields(BusinessObjectTableMappingVO mapping) {
        return mapping.getFields() == null ? List.of() : mapping.getFields();
    }

    private String configuredType(BusinessObjectTableFieldMappingVO field) {
        String dataType = StringUtils.defaultIfBlank(field.getDataType(), "未知");
        if (field.getLength() == null) {
            return dataType;
        }
        if ("decimal".equalsIgnoreCase(dataType)) {
            int precision = field.getPrecision() == null ? 2 : field.getPrecision();
            return dataType + "(" + field.getLength() + "," + precision + ")";
        }
        return dataType + "(" + field.getLength() + ")";
    }

    private String safeMessage(Throwable error) {
        return StringUtils.abbreviate(
                StringUtils.defaultIfBlank(error.getMessage(), "无法读取目标数据库结构"), 300);
    }

    private BusinessApplicationReadinessVO buildReadiness(List<BusinessApplicationReadinessIssueVO> issues) {
        long blockingCount = issues.stream().filter(item -> BLOCK.equals(item.getLevel())).count();
        long warningCount = issues.stream().filter(item -> WARN.equals(item.getLevel())).count();
        BusinessApplicationReadinessVO readiness = new BusinessApplicationReadinessVO();
        readiness.setReady(blockingCount == 0L);
        readiness.setStatus(blockingCount > 0L ? "BLOCKED" : warningCount > 0L ? "WARNING" : "READY");
        readiness.setBlockingCount(blockingCount);
        readiness.setWarningCount(warningCount);
        readiness.setIssues(issues);
        return readiness;
    }

    private BusinessApplicationReadinessIssueVO issue(String code, String level, String title, String message,
                                                       String sectionKey, String actionPanel, String assetType,
                                                       Long assetId, String assetCode) {
        BusinessApplicationReadinessIssueVO issue = new BusinessApplicationReadinessIssueVO();
        issue.setIssueCode(code);
        issue.setLevel(level);
        issue.setTitle(title);
        issue.setMessage(message);
        issue.setSectionKey(sectionKey);
        issue.setActionPanel(actionPanel);
        issue.setAssetType(assetType);
        issue.setAssetId(assetId);
        issue.setAssetCode(assetCode);
        return issue;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    record ResolvedPublishCheck(
            BusinessApplicationPublishCheckVO check,
            BusinessApplicationVO application,
            BusinessApplicationAssetSelectionService.ResolvedSelection selection,
            List<BusinessPermissionSummaryVO> permissionSummaries,
            List<AiBusinessBinding> bindings,
            Map<Long, BusinessObjectDesignerService.DesignerContext> objectContexts) {
    }

    private record EvaluationResult(
            BusinessApplicationReadinessVO readiness,
            List<BusinessPermissionSummaryVO> permissionSummaries,
            List<AiBusinessBinding> bindings,
            Map<Long, BusinessObjectDesignerService.DesignerContext> objectContexts) {
    }
}
