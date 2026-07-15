package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishCheckVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessIssueVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckItemVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public BusinessApplicationReadinessVO check(Long applicationId) {
        BusinessApplicationVO application = applicationService.publishContext(applicationId);
        return evaluate(application, selectionService.resolveContext(applicationId, null)).readiness();
    }

    public BusinessApplicationPublishCheckVO publishCheck(Long applicationId, BusinessApplicationPublishDTO dto) {
        return resolvePublishCheck(applicationId, dto).check();
    }

    ResolvedPublishCheck resolvePublishCheck(Long applicationId, BusinessApplicationPublishDTO dto) {
        BusinessApplicationVO application = applicationService.publishContext(applicationId);
        BusinessApplicationAssetSelectionService.ResolvedSelection resolved
                = selectionService.resolveContext(applicationId, dto);
        BusinessApplicationAssetSelectionVO selection = resolved.selection();
        EvaluationResult evaluation = evaluate(application, resolved);
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
                result, application, resolved, evaluation.permissionSummaries(), evaluation.bindings());
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

        if (!Integer.valueOf(1).equals(application.getStatus())) {
            issues.add(issue("APPLICATION_DISABLED", BLOCK, "应用已停用",
                    "停用应用不能发布，请先启用应用。", "overview", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        if (StringUtils.isBlank(application.getSuiteName())) {
            issues.add(issue("SUITE_UNAVAILABLE", BLOCK, "所属业务域不可用",
                    "业务域不存在、已删除或当前租户无权访问。", "overview", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        long primaryCount = allObjects.stream()
                .filter(item -> BusinessApplicationObjectRole.PRIMARY.equals(item.getObjectRole()))
                .count();
        if (primaryCount != 1L) {
            issues.add(issue("PRIMARY_OBJECT_INVALID", BLOCK, "主对象配置不完整",
                    "应用发布前必须且只能配置一个主对象。", "objects", "objects", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        Map<Long, com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp> selectedEntries
                = resolved.entries().stream()
                .filter(entry -> selection.getEntryIds().contains(entry.getId()))
                .collect(java.util.stream.Collectors.toMap(
                        com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp::getId,
                        java.util.function.Function.identity()));
        if (selection.getEntryIds().isEmpty() || selectedEntries.values().stream()
                .noneMatch(entry -> Integer.valueOf(1).equals(entry.getStatus()))) {
            issues.add(issue("ACTIVE_ENTRY_MISSING", WARN, "尚未配置页面入口",
                    "页面入口不是发布必需项；应用仍可发布对象、预览草稿或生成代码。", "entries", "entries", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        selectedEntries.values().forEach(entry -> {
            if (!Integer.valueOf(1).equals(entry.getStatus())) {
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

        List<BusinessApplicationObjectVO> selectedObjectList = allObjects.stream()
                .filter(object -> selectedObjects.contains(object.getObjectId())).toList();
        List<BusinessPermissionSummaryVO> permissionSummaries = permissionService
                .documentActionSummaries(selectedObjectList);
        Map<Long, BusinessPermissionSummaryVO> permissions = permissionSummaries.stream()
                .collect(Collectors.toMap(BusinessPermissionSummaryVO::getObjectId, Function.identity()));
        for (BusinessApplicationObjectVO object : selectedObjectList) {
            checkObject(object, permissions.get(object.getObjectId()), issues);
        }
        for (AiBusinessExtension extension : resolved.extensions()) {
            boolean selected = selectedExtensions.contains(extension.getId());
            if (BusinessExtensionStatus.DRAFT.equals(extension.getStatus())) {
                issues.add(issue("EXTENSION_UNTESTED", selected ? BLOCK : WARN,
                        selected ? "扩展尚未测试" : "未测试扩展已跳过",
                        extension.getExtensionName() + (selected
                                ? " 的当前草稿未通过受限测试。"
                                : " 仍保留为草稿，本次发布不会包含该扩展。"),
                        "enhancements", "enhancements", "EXTENSION", extension.getId(), extension.getExtensionCode()));
            } else if (selected && BusinessExtensionStatus.ENABLED.equals(extension.getStatus())
                    && !java.util.Objects.equals(extension.getDraftVersion(), extension.getEnabledVersion())) {
                issues.add(issue("EXTENSION_VERSION_MISMATCH", BLOCK, "扩展运行版本落后",
                        extension.getExtensionName() + " 的当前草稿尚未启用。",
                        "enhancements", "enhancements", "EXTENSION", extension.getId(), extension.getExtensionCode()));
            }
        }

        List<AiBusinessBinding> bindings = null;
        if (Boolean.TRUE.equals(selection.getIncludeAutomation())) {
            bindings = bindingMapper.selectByApplication(resolveTenantId(), applicationId);
            boolean hasFlow = bindings.stream().anyMatch(binding -> Integer.valueOf(1).equals(binding.getStatus())
                    && ("FLOW".equals(binding.getBindingType()) || "APPROVAL".equals(binding.getBindingType())));
            if (!hasFlow) {
                issues.add(issue("FLOW_OPTIONAL", WARN, "尚未绑定应用级流程",
                        "流程不是发布必需项；需要审批或自动流转时可继续配置。",
                        "automation", "automation", "APPLICATION", applicationId, application.getApplicationCode()));
            }
        }
        return new EvaluationResult(buildReadiness(issues), permissionSummaries, bindings);
    }

    private void checkObject(BusinessApplicationObjectVO object,
                             BusinessPermissionSummaryVO permissionSummary,
                             List<BusinessApplicationReadinessIssueVO> issues) {
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
        String syncStatus = StringUtils.defaultString(object.getSyncStatus()).toUpperCase();
        if (Set.of("OUT_OF_SYNC", "TABLE_MISSING", "CHECK_FAILED", "FAILED").contains(syncStatus)) {
            issues.add(issue("OBJECT_DATABASE_OUT_OF_SYNC", BLOCK, "数据库结构未同步",
                    objectName + " 的数据库映射状态为 " + syncStatus + "。",
                    "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
        } else if ("UNKNOWN".equals(syncStatus)) {
            issues.add(issue("OBJECT_DATABASE_UNKNOWN", WARN, "数据库同步状态未知",
                    objectName + " 尚无最近同步证据，请在发布前复核。",
                    "objects", "objects", "OBJECT", object.getObjectId(), object.getObjectCode()));
        }
        BusinessPublishCheckVO objectCheck = objectPublishService.publishCheck(
                object.getObjectId(), permissionSummary);
        if (Boolean.FALSE.equals(objectCheck.getPublishable())) {
            List<BusinessPublishCheckItemVO> blocks = objectCheck.getBlockItems() == null
                    ? List.of() : objectCheck.getBlockItems();
            String detail = blocks.stream().limit(3)
                    .map(BusinessPublishCheckItemVO::getTitle)
                    .filter(StringUtils::isNotBlank)
                    .reduce((left, right) -> left + "、" + right)
                    .orElse("对象发布检查未通过");
            issues.add(issue("OBJECT_PUBLISH_BLOCKED", BLOCK, "对象发布检查未通过",
                    objectName + "：" + detail, "objects", "objects", "OBJECT",
                    object.getObjectId(), object.getObjectCode()));
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
            List<AiBusinessBinding> bindings) {
    }

    private record EvaluationResult(
            BusinessApplicationReadinessVO readiness,
            List<BusinessPermissionSummaryVO> permissionSummaries,
            List<AiBusinessBinding> bindings) {
    }
}
