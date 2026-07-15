package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessIssueVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationWorkspaceSectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationWorkspaceVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用工作台首屏摘要与就绪度编排服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationWorkspaceService {

    private static final String LEVEL_BLOCK = "BLOCK";
    private static final String LEVEL_WARN = "WARN";

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessAppService businessAppService;
    private final BusinessExtensionService businessExtensionService;
    private final BusinessApplicationReadinessService readinessService;

    public BusinessApplicationWorkspaceVO workspace(Long applicationId) {
        return assembleWorkspace(applicationService.detail(applicationId));
    }

    public BusinessApplicationWorkspaceVO workspaceByCode(String applicationCode) {
        return assembleWorkspace(applicationService.detailByCode(applicationCode));
    }

    private BusinessApplicationWorkspaceVO assembleWorkspace(BusinessApplicationVO application) {
        Long applicationId = application.getId();
        List<BusinessApplicationObjectVO> objects = applicationObjectService.list(applicationId);
        List<BusinessAppVO> entries = businessAppService.list(entryQuery(applicationId));
        List<BusinessExtensionVO> extensions = businessExtensionService.listWorkspaceSummaries(applicationId);
        BusinessApplicationReadinessVO readiness = buildWorkspaceReadiness(application, objects, entries);
        List<BusinessApplicationReadinessIssueVO> issues = readiness.getIssues();

        BusinessApplicationWorkspaceVO workspace = new BusinessApplicationWorkspaceVO();
        workspace.setApplication(application);
        workspace.setObjects(objects);
        workspace.setEntries(entries);
        workspace.setExtensions(extensions);
        workspace.setIssues(issues);
        workspace.setReadiness(readiness);
        workspace.setBlockingCount(readiness.getBlockingCount());
        workspace.setWarningCount(readiness.getWarningCount());
        workspace.setLatestChangeTime(application.getUpdateTime());
        workspace.setSections(buildSections(application, objects.size(), entries.size(), extensions.size(), issues));
        return workspace;
    }

    public BusinessApplicationReadinessVO readiness(Long applicationId) {
        return readinessService.check(applicationId);
    }

    private List<BusinessApplicationWorkspaceSectionVO> buildSections(
            BusinessApplicationVO application,
            int objectCount,
            int entryCount,
            int extensionCount,
            List<BusinessApplicationReadinessIssueVO> issues) {
        return List.of(
                section("overview", "概览", 0L, issues),
                section("objects", "数据对象", objectCount, issues),
                section("entries", "页面入口", entryCount, issues),
                section("automation", "流程自动化", value(application.getFlowCount()), issues),
                section("enhancements", "动作与增强", extensionCount, issues),
                section("permissions", "权限", 0L, issues),
                section("releases", "发布历史", application.getLastPublishVersion() == null ? 0L : 1L, issues)
        );
    }

    private BusinessApplicationWorkspaceSectionVO section(
            String key, String name, long assetCount, List<BusinessApplicationReadinessIssueVO> issues) {
        long blocking = issues.stream()
                .filter(item -> key.equals(item.getSectionKey()) && LEVEL_BLOCK.equals(item.getLevel()))
                .count();
        long warning = issues.stream()
                .filter(item -> key.equals(item.getSectionKey()) && LEVEL_WARN.equals(item.getLevel()))
                .count();
        BusinessApplicationWorkspaceSectionVO section = new BusinessApplicationWorkspaceSectionVO();
        section.setSectionKey(key);
        section.setSectionName(name);
        section.setAssetCount(assetCount);
        section.setProblemCount(blocking + warning);
        section.setStatus(blocking > 0L ? "BLOCKED" : warning > 0L ? "WARNING" : "READY");
        section.setLazy(!"overview".equals(key));
        return section;
    }

    /**
     * 工作台浏览只计算轻量配置状态。对象级发布检查、权限检查和数据库检查
     * 统一留在发布分区显式执行，避免普通分区切换触发完整发布链路。
     */
    private BusinessApplicationReadinessVO buildWorkspaceReadiness(
            BusinessApplicationVO application,
            List<BusinessApplicationObjectVO> objects,
            List<BusinessAppVO> entries) {
        List<BusinessApplicationReadinessIssueVO> issues = new ArrayList<>();
        if (!Integer.valueOf(1).equals(application.getStatus())) {
            issues.add(issue("APPLICATION_DISABLED", LEVEL_BLOCK, "应用已停用",
                    "请先启用应用后再继续配置和发布。", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        if (StringUtils.isBlank(application.getSuiteName())) {
            issues.add(issue("SUITE_UNAVAILABLE", LEVEL_BLOCK, "所属业务域不可用",
                    "业务域不存在、已删除或当前租户无权访问。", "overview", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        long primaryCount = objects.stream()
                .filter(item -> BusinessApplicationObjectRole.PRIMARY.equals(item.getObjectRole()))
                .count();
        if (primaryCount != 1L) {
            issues.add(issue("PRIMARY_OBJECT_MISSING", LEVEL_BLOCK, "主对象配置不完整",
                    "应用必须且只能配置一个主对象。", "objects", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        if (entries.stream().noneMatch(item -> Integer.valueOf(1).equals(item.getStatus()))) {
            issues.add(issue("ACTIVE_ENTRY_MISSING", LEVEL_WARN, "尚未配置页面入口",
                    "页面入口可按需配置；当前仍可预览草稿、发布对象或生成代码。", "entries", "APPLICATION",
                    application.getId(), application.getApplicationCode()));
        }
        for (BusinessApplicationObjectVO object : objects) {
            String objectName = StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode());
            if (!Integer.valueOf(1).equals(object.getObjectStatus())) {
                issues.add(issue("OBJECT_DISABLED", LEVEL_WARN, "业务对象已停用",
                        objectName + " 当前处于停用状态。", "objects", "OBJECT",
                        object.getObjectId(), object.getObjectCode()));
            }
            if (!"PUBLISHED".equalsIgnoreCase(StringUtils.defaultString(object.getDesignStatus()))) {
                issues.add(issue("OBJECT_UNPUBLISHED", LEVEL_WARN, "业务对象存在未发布设计",
                        objectName + " 尚未发布最新设计。", "objects", "OBJECT",
                        object.getObjectId(), object.getObjectCode()));
            }
        }
        long blockingCount = issues.stream().filter(item -> LEVEL_BLOCK.equals(item.getLevel())).count();
        long warningCount = issues.stream().filter(item -> LEVEL_WARN.equals(item.getLevel())).count();
        BusinessApplicationReadinessVO readiness = new BusinessApplicationReadinessVO();
        readiness.setReady(blockingCount == 0L);
        readiness.setStatus(blockingCount > 0L ? "BLOCKED" : warningCount > 0L ? "WARNING" : "READY");
        readiness.setBlockingCount(blockingCount);
        readiness.setWarningCount(warningCount);
        readiness.setIssues(issues);
        return readiness;
    }

    private BusinessApplicationReadinessIssueVO issue(
            String code,
            String level,
            String title,
            String message,
            String sectionKey,
            String assetType,
            Long assetId,
            String assetCode) {
        BusinessApplicationReadinessIssueVO issue = new BusinessApplicationReadinessIssueVO();
        issue.setIssueCode(code);
        issue.setLevel(level);
        issue.setTitle(title);
        issue.setMessage(message);
        issue.setSectionKey(sectionKey);
        issue.setActionPanel(sectionKey);
        issue.setAssetType(assetType);
        issue.setAssetId(assetId);
        issue.setAssetCode(assetCode);
        return issue;
    }

    private BusinessAppQueryDTO entryQuery(Long applicationId) {
        BusinessAppQueryDTO query = new BusinessAppQueryDTO();
        query.setApplicationId(applicationId);
        return query;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
