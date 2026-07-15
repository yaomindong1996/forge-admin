package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStep;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationPublishRun;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectPublishDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationSnapshotService.SnapshotBundle;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishCheckVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishRunVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 应用级协调发布编排。各业务步骤独立提交，运行单记录部分完成状态并支持恢复。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationPublishService {

    private final BusinessApplicationReadinessService readinessService;
    private final BusinessApplicationSnapshotService snapshotService;
    private final BusinessApplicationPublishRunService runService;
    private final BusinessApplicationVersionService versionService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessObjectPublishService objectPublishService;
    private final BusinessObjectDesignerService objectDesignerService;
    private final BusinessObjectDesignVersionService objectVersionService;
    private final BusinessAppService businessAppService;
    private final BusinessExtensionMapper extensionMapper;
    private final BusinessExtensionExecutionService extensionExecutionService;

    public BusinessApplicationPublishCheckVO check(Long applicationId, BusinessApplicationPublishDTO dto) {
        return readinessService.publishCheck(applicationId, dto);
    }

    public BusinessApplicationPublishResultVO publish(Long applicationId,
                                                      BusinessApplicationPublishDTO dto,
                                                      String idempotencyKey) {
        AiBusinessApplicationPublishRun existing = runService.findByIdempotencyKey(applicationId, idempotencyKey);
        if (existing != null) {
            return toResult(existing, existingRunMessage(existing));
        }
        preparePrimaryObjectDraft(applicationId);
        BusinessApplicationReadinessService.ResolvedPublishCheck resolvedCheck
                = readinessService.resolvePublishCheck(applicationId, dto);
        BusinessApplicationPublishCheckVO check = resolvedCheck.check();
        if (!Boolean.TRUE.equals(check.getPublishable())) {
            throw new BusinessException(blockedMessage(check));
        }
        SnapshotBundle candidate = snapshotService.prepare(
                applicationId, resolvedCheck.application(), resolvedCheck.selection(),
                resolvedCheck.permissionSummaries(), resolvedCheck.bindings());
        AiBusinessApplicationPublishRun run = runService.reserve(applicationId, idempotencyKey,
                "PUBLISH", null, candidate, check.getSelection());
        if (BusinessApplicationPublishStatus.SUCCESS.equals(run.getRunStatus())
                || BusinessApplicationPublishStatus.PARTIAL.equals(run.getRunStatus())
                || BusinessApplicationPublishStatus.FAILED.equals(run.getRunStatus())
                || BusinessApplicationPublishStatus.RUNNING.equals(run.getRunStatus())) {
            return toResult(run, existingRunMessage(run));
        }
        if (!runService.tryClaimCreated(applicationId, run.getId())) {
            return toResult(runService.requireRun(applicationId, run.getId()), "相同幂等请求已由另一执行器处理");
        }
        run.setRunStatus(BusinessApplicationPublishStatus.RUNNING);
        return resume(run,
                dto == null ? new BusinessApplicationPublishDTO() : dto, resolvedCheck, false);
    }

    public BusinessApplicationPublishResultVO resume(AiBusinessApplicationPublishRun run,
                                                     BusinessApplicationPublishDTO dto) {
        return resume(run, dto, null, true);
    }

    private BusinessApplicationPublishResultVO resume(AiBusinessApplicationPublishRun run,
                                                       BusinessApplicationPublishDTO dto,
                                                       BusinessApplicationReadinessService.ResolvedPublishCheck initialCheck,
                                                       boolean forcePrecheck) {
        String step = run.getCurrentStep();
        try {
            BusinessApplicationAssetSelectionVO selection = runService.readSelection(run);
            BusinessApplicationPublishDTO effectiveDto = dtoFromSelection(selection, dto);
            Map<Long, BusinessPermissionSummaryVO> permissionSummaries = Map.of();
            if (forcePrecheck || !runService.isStepComplete(run, BusinessApplicationPublishStep.PRECHECK)) {
                step = BusinessApplicationPublishStep.PRECHECK;
                run = runService.markStepRunning(run, step);
                BusinessApplicationReadinessService.ResolvedPublishCheck resolvedCheck = initialCheck == null
                        ? readinessService.resolvePublishCheck(run.getApplicationId(), effectiveDto)
                        : initialCheck;
                BusinessApplicationPublishCheckVO check = resolvedCheck.check();
                if (!Boolean.TRUE.equals(check.getPublishable())) {
                    return fail(run, step, "PUBLISH_PRECHECK_BLOCKED", blockedMessage(check));
                }
                permissionSummaries = resolvedCheck.permissionSummaries().stream()
                        .collect(Collectors.toMap(BusinessPermissionSummaryVO::getObjectId, Function.identity()));
                run = runService.markStepSuccess(run, step,
                        check.getWarningCount() + " 项提醒，不阻断发布");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.SNAPSHOT)) {
                step = BusinessApplicationPublishStep.SNAPSHOT;
                run = runService.markStepRunning(run, step);
                run = runService.markStepSuccess(run, step, "候选快照摘要 " + shortHash(run.getSnapshotHash()));
            }

            Map<Long, Long> objectVersions = readPublishedObjectVersions(run.getSnapshotJson());
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.OBJECTS)) {
                step = BusinessApplicationPublishStep.OBJECTS;
                run = runService.markStepRunning(run, step);
                PublishObjectsResult publishResult = publishObjects(
                        run, selection, effectiveDto, objectVersions, permissionSummaries);
                run = publishResult.run();
                objectVersions = publishResult.objectVersions();
                SnapshotBundle objectSnapshot = snapshotService.finalizePublished(
                        run.getSnapshotJson(), objectVersions, selection, run.getTargetVersionNo(), "PUBLISH");
                if (!StringUtils.equals(run.getSnapshotHash(), objectSnapshot.hash())) {
                    run = runService.updateSnapshot(run, objectSnapshot);
                }
                run = runService.markStepSuccess(run, step, "已处理 " + objectVersions.size() + " 个业务对象");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.ENTRIES)) {
                step = BusinessApplicationPublishStep.ENTRIES;
                run = runService.markStepRunning(run, step);
                List<Long> entries = businessAppService.publishEntries(run.getApplicationId(), selection.getEntryIds());
                run = runService.markStepSuccess(run, step, "已切换 " + entries.size() + " 个页面入口");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.EXTENSIONS)) {
                step = BusinessApplicationPublishStep.EXTENSIONS;
                run = runService.markStepRunning(run, step);
                int enabledCount = enableExtensions(run.getApplicationId(), selection.getExtensionIds());
                run = runService.markStepSuccess(run, step, "已确认 " + enabledCount + " 个扩展运行版本");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.COMMIT)) {
                step = BusinessApplicationPublishStep.COMMIT;
                run = runService.markStepRunning(run, step);
                SnapshotBundle finalSnapshot = snapshotService.finalizePublished(
                        run.getSnapshotJson(), objectVersions, selection, run.getTargetVersionNo(), "PUBLISH");
                AiBusinessApplicationVersion version = versionService.commitImmutable(
                        run.getApplicationId(), run.getTargetVersionNo(), finalSnapshot,
                        BusinessApplicationPublishStatus.PUBLISHED, null,
                        StringUtils.defaultIfBlank(dto.getRemark(), "应用协调发布成功"));
                run = runService.markSuccess(run, version.getId(), finalSnapshot);
            }
            return toResult(run, "应用 v" + run.getTargetVersionNo() + " 发布成功");
        } catch (BusinessException e) {
            return fail(run, step, "PUBLISH_STEP_FAILED", safeMessage(e));
        } catch (Exception e) {
            return fail(run, step, "PUBLISH_INTERNAL_ERROR", "发布步骤发生内部异常，详细信息已脱敏");
        }
    }

    private PublishObjectsResult publishObjects(AiBusinessApplicationPublishRun run,
                                                BusinessApplicationAssetSelectionVO selection,
                                                BusinessApplicationPublishDTO dto,
                                                Map<Long, Long> completedVersions,
                                                Map<Long, BusinessPermissionSummaryVO> permissionSummaries) {
        Map<Long, BusinessApplicationObjectVO> objects = applicationObjectService.list(run.getApplicationId()).stream()
                .collect(Collectors.toMap(BusinessApplicationObjectVO::getObjectId, Function.identity()));
        Map<Long, Long> latestPublishedVersions
                = objectVersionService.latestPublishedVersionIds(selection.getObjectIds());
        Map<Long, Long> result = new LinkedHashMap<>(completedVersions);
        for (Long objectId : selection.getObjectIds()) {
            if (result.containsKey(objectId)) {
                continue;
            }
            BusinessApplicationObjectVO object = objects.get(objectId);
            if (object == null) {
                throw new BusinessException("发布对象不属于当前应用: " + objectId);
            }
            Long existingVersion = latestPublishedVersions.get(objectId);
            if ("PUBLISHED".equalsIgnoreCase(object.getDesignStatus()) && existingVersion != null) {
                result.put(objectId, existingVersion);
                continue;
            }
            BusinessObjectPublishDTO objectDto = new BusinessObjectPublishDTO();
            objectDto.setSyncTable(false);
            objectDto.setForce(false);
            objectDto.setRemark("由应用协调发布: " + StringUtils.defaultString(dto.getRemark()));
            result.put(objectId, objectPublishService.publish(
                    objectId, objectDto, permissionSummaries.get(objectId)));
            SnapshotBundle checkpoint = snapshotService.finalizePublished(
                    run.getSnapshotJson(), result, selection, run.getTargetVersionNo(), "PUBLISH");
            run = runService.updateSnapshot(run, checkpoint);
        }
        return new PublishObjectsResult(run, result);
    }

    private void preparePrimaryObjectDraft(Long applicationId) {
        applicationObjectService.list(applicationId).stream()
                .filter(item -> "PRIMARY".equalsIgnoreCase(item.getObjectRole()))
                .map(BusinessApplicationObjectVO::getObjectId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .ifPresent(objectDesignerService::prepareRuntimeDraft);
    }

    private int enableExtensions(Long applicationId, List<Long> extensionIds) {
        Map<Long, AiBusinessExtension> extensions = extensionMapper
                .selectByApplicationId(resolveTenantId(), applicationId).stream()
                .collect(Collectors.toMap(AiBusinessExtension::getId, Function.identity()));
        int count = 0;
        for (Long extensionId : extensionIds) {
            AiBusinessExtension extension = extensions.get(extensionId);
            if (extension == null) {
                throw new BusinessException("发布扩展不属于当前应用: " + extensionId);
            }
            if (BusinessExtensionStatus.TESTED.equals(extension.getStatus())) {
                extensionExecutionService.updateStatus(extensionId, BusinessExtensionStatus.ENABLED);
            } else if (BusinessExtensionStatus.DISABLED.equals(extension.getStatus())) {
                count++;
                continue;
            } else if (!BusinessExtensionStatus.ENABLED.equals(extension.getStatus())
                    || !java.util.Objects.equals(extension.getDraftVersion(), extension.getEnabledVersion())) {
                throw new BusinessException("扩展未通过测试或运行版本落后: " + extension.getExtensionName());
            }
            count++;
        }
        return count;
    }

    private Map<Long, Long> readPublishedObjectVersions(String snapshotJson) {
        Map<Long, Long> result = new LinkedHashMap<>();
        Object value = snapshotService.parse(snapshotJson).get("publishedObjectVersions");
        if (!(value instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Long objectId = longValue(map.get("objectId"));
            Long versionId = longValue(map.get("designVersionId"));
            if (objectId != null && versionId != null) {
                result.put(objectId, versionId);
            }
        }
        return result;
    }

    private BusinessApplicationPublishDTO dtoFromSelection(BusinessApplicationAssetSelectionVO selection,
                                                           BusinessApplicationPublishDTO source) {
        BusinessApplicationPublishDTO dto = new BusinessApplicationPublishDTO();
        dto.setSelectedObjectIds(selection.getObjectIds());
        dto.setSelectedEntryIds(selection.getEntryIds());
        dto.setSelectedExtensionIds(selection.getExtensionIds());
        dto.setIncludeAutomation(selection.getIncludeAutomation());
        dto.setForceWarnings(source == null ? false : source.getForceWarnings());
        dto.setRemark(source == null ? null : source.getRemark());
        return dto;
    }

    private BusinessApplicationPublishResultVO fail(AiBusinessApplicationPublishRun run, String step,
                                                    String code, String message) {
        AiBusinessApplicationPublishRun failed = runService.markFailed(run,
                StringUtils.defaultIfBlank(step, BusinessApplicationPublishStep.PRECHECK), code, message);
        return toResult(failed, message);
    }

    private BusinessApplicationPublishResultVO toResult(AiBusinessApplicationPublishRun run, String message) {
        BusinessApplicationPublishRunVO detail = runService.toVO(run);
        BusinessApplicationPublishResultVO result = new BusinessApplicationPublishResultVO();
        result.setRunId(run.getId());
        result.setApplicationId(run.getApplicationId());
        result.setOperationType(run.getOperationType());
        result.setRunStatus(run.getRunStatus());
        result.setTargetVersionNo(run.getTargetVersionNo());
        result.setResultVersionId(run.getResultVersionId());
        result.setRecoverable(Set.of(BusinessApplicationPublishStatus.PARTIAL,
                BusinessApplicationPublishStatus.FAILED).contains(run.getRunStatus()));
        result.setCurrentStep(run.getCurrentStep());
        result.setMessage(message);
        result.setSteps(detail.getSteps());
        return result;
    }

    private String blockedMessage(BusinessApplicationPublishCheckVO check) {
        String issues = check.getIssues().stream().filter(item -> "BLOCK".equals(item.getLevel()))
                .limit(3).map(item -> item.getTitle() + "：" + item.getMessage())
                .reduce((left, right) -> left + "；" + right).orElse("发布检查未通过");
        return "应用存在 " + check.getBlockingCount() + " 个发布阻断项：" + issues;
    }

    private String existingRunMessage(AiBusinessApplicationPublishRun run) {
        return switch (run.getRunStatus()) {
            case BusinessApplicationPublishStatus.SUCCESS -> "相同幂等请求已成功完成";
            case BusinessApplicationPublishStatus.PARTIAL -> "相同幂等请求部分完成，请执行恢复";
            case BusinessApplicationPublishStatus.FAILED -> "相同幂等请求已失败，请修复后执行恢复";
            default -> "相同幂等请求正在执行";
        };
    }

    private String safeMessage(BusinessException exception) {
        return StringUtils.abbreviate(StringUtils.defaultIfBlank(exception.getMessage(), "发布步骤失败"), 500);
    }

    private String shortHash(String value) {
        return StringUtils.length(value) <= 12 ? value : value.substring(0, 12);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    private record PublishObjectsResult(AiBusinessApplicationPublishRun run,
                                        Map<Long, Long> objectVersions) {
    }
}
