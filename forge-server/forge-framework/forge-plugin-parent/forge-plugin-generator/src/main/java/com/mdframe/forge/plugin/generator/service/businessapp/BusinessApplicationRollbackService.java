package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStep;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationPublishRun;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRollbackDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationSnapshotService.SnapshotBundle;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishRunVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignVersionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableFieldMappingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectTableMappingVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 从历史应用版本恢复运行配置；不回滚破坏性 DDL 或业务数据。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationRollbackService {

    private final ObjectMapper objectMapper;
    private final BusinessApplicationVersionService versionService;
    private final BusinessApplicationService applicationService;
    private final BusinessApplicationSnapshotService snapshotService;
    private final BusinessApplicationPublishRunService runService;
    private final BusinessObjectDesignVersionService objectVersionService;
    private final BusinessObjectTableMappingService tableMappingService;
    private final BusinessObjectPublishService objectPublishService;
    private final BusinessAppService businessAppService;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessExtensionMapper extensionMapper;
    private final BusinessExtensionVersionMapper extensionVersionMapper;

    public BusinessApplicationPublishResultVO rollback(Long applicationId,
                                                       Integer sourceVersionNo,
                                                       BusinessApplicationRollbackDTO dto,
                                                       String idempotencyKey) {
        AiBusinessApplicationPublishRun existing = runService.findByIdempotencyKey(applicationId, idempotencyKey);
        if (existing != null) {
            return toResult(existing, existingRunMessage(existing));
        }
        AiBusinessApplicationVersion source = versionService.requireVersion(applicationId, sourceVersionNo);
        Map<String, Object> candidateMap = snapshotService.parse(source.getSnapshotJson());
        assertCompatible(applicationId, candidateMap);
        prepareRollbackCandidate(candidateMap, sourceVersionNo);
        SnapshotBundle candidate = snapshotService.bundle(candidateMap);
        BusinessApplicationAssetSelectionVO selection = resolveSelection(candidateMap);
        AiBusinessApplicationPublishRun run = runService.reserve(applicationId, idempotencyKey,
                "ROLLBACK", sourceVersionNo, candidate, selection);
        if (BusinessApplicationPublishStatus.SUCCESS.equals(run.getRunStatus())
                || BusinessApplicationPublishStatus.PARTIAL.equals(run.getRunStatus())
                || BusinessApplicationPublishStatus.FAILED.equals(run.getRunStatus())
                || BusinessApplicationPublishStatus.RUNNING.equals(run.getRunStatus())) {
            return toResult(run, existingRunMessage(run));
        }
        if (!runService.tryClaimCreated(applicationId, run.getId())) {
            return toResult(runService.requireRun(applicationId, run.getId()), "相同幂等回滚已由另一执行器处理");
        }
        run.setRunStatus(BusinessApplicationPublishStatus.RUNNING);
        return resume(run, dto);
    }

    public BusinessApplicationPublishResultVO resume(AiBusinessApplicationPublishRun run,
                                                     BusinessApplicationRollbackDTO dto) {
        String step = run.getCurrentStep();
        try {
            Map<String, Object> snapshot = snapshotService.parse(run.getSnapshotJson());
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.PRECHECK)) {
                step = BusinessApplicationPublishStep.PRECHECK;
                run = runService.markStepRunning(run, step);
                assertCompatible(run.getApplicationId(), snapshot);
                run = runService.markStepSuccess(run, step, "历史版本兼容性检查通过");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.SNAPSHOT)) {
                step = BusinessApplicationPublishStep.SNAPSHOT;
                run = runService.markStepRunning(run, step);
                run = runService.markStepSuccess(run, step,
                        "已锁定来源版本 v" + run.getSourceVersionNo());
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.OBJECTS)) {
                step = BusinessApplicationPublishStep.OBJECTS;
                run = runService.markStepRunning(run, step);
                run = rollbackObjects(run);
                run = runService.markStepSuccess(run, step, "业务对象运行配置已恢复");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.ENTRIES)) {
                step = BusinessApplicationPublishStep.ENTRIES;
                run = runService.markStepRunning(run, step);
                snapshot = snapshotService.parse(run.getSnapshotJson());
                applicationService.restoreSnapshotMetadata(run.getApplicationId(), map(snapshot.get("application")));
                businessAppService.restoreSnapshotEntries(run.getApplicationId(), listOfMap(snapshot.get("entries")));
                restoreBindings(run.getApplicationId(), listOfMap(snapshot.get("bindings")));
                run = runService.markStepSuccess(run, step, "页面入口和应用挂接配置已恢复");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.EXTENSIONS)) {
                step = BusinessApplicationPublishStep.EXTENSIONS;
                run = runService.markStepRunning(run, step);
                snapshot = snapshotService.parse(run.getSnapshotJson());
                restoreExtensions(run.getApplicationId(), listOfMap(snapshot.get("extensions")));
                run = runService.markStepSuccess(run, step, "扩展运行版本已恢复");
            }
            if (!runService.isStepComplete(run, BusinessApplicationPublishStep.COMMIT)) {
                step = BusinessApplicationPublishStep.COMMIT;
                run = runService.markStepRunning(run, step);
                SnapshotBundle finalSnapshot = snapshotService.bundle(snapshotService.parse(run.getSnapshotJson()));
                AiBusinessApplicationVersion version = versionService.commitImmutable(
                        run.getApplicationId(), run.getTargetVersionNo(), finalSnapshot,
                        BusinessApplicationPublishStatus.ROLLBACK, run.getSourceVersionNo(),
                        StringUtils.defaultIfBlank(dto == null ? null : dto.getRemark(),
                                "从应用 v" + run.getSourceVersionNo() + " 回滚"));
                run = runService.markSuccess(run, version.getId(), finalSnapshot);
            }
            return toResult(run, "已从 v" + run.getSourceVersionNo()
                    + " 生成回滚版本 v" + run.getTargetVersionNo());
        } catch (BusinessException e) {
            return fail(run, step, "ROLLBACK_STEP_FAILED", safeMessage(e));
        } catch (Exception e) {
            return fail(run, step, "ROLLBACK_INTERNAL_ERROR", "回滚步骤发生内部异常，详细信息已脱敏");
        }
    }

    private AiBusinessApplicationPublishRun rollbackObjects(AiBusinessApplicationPublishRun run) {
        Map<String, Object> snapshot = snapshotService.parse(run.getSnapshotJson());
        Map<Long, Long> sources = objectVersionMap(snapshot.get("rollbackSourceObjectVersions"));
        Map<Long, Long> completed = objectVersionMap(snapshot.get("publishedObjectVersions"));
        for (Map.Entry<Long, Long> source : sources.entrySet()) {
            if (completed.containsKey(source.getKey())) {
                continue;
            }
            Long rollbackVersionId = objectPublishService.rollbackForApplication(source.getKey(), source.getValue());
            completed.put(source.getKey(), rollbackVersionId);
            snapshot.put("publishedObjectVersions", objectVersionList(completed));
            run = runService.updateSnapshot(run, snapshotService.bundle(snapshot));
        }
        return run;
    }

    private void assertCompatible(Long applicationId, Map<String, Object> snapshot) {
        Map<Long, Long> objectVersions = objectVersionMap(firstNonNull(
                snapshot.get("rollbackSourceObjectVersions"), snapshot.get("publishedObjectVersions")));
        if (objectVersions.isEmpty()) {
            throw new BusinessException("历史应用版本缺少业务对象版本引用，不能安全回滚");
        }
        for (Map.Entry<Long, Long> item : objectVersions.entrySet()) {
            BusinessObjectDesignVersionVO version = objectVersionService.detail(item.getKey(), item.getValue());
            BusinessObjectTableMappingVO mapping = tableMappingService.getTableMapping(item.getKey());
            if (Boolean.FALSE.equals(mapping.getTableExists())) {
                throw new BusinessException("历史版本依赖的物理表已不存在: " + mapping.getTableName());
            }
            Set<String> currentColumns = mapping.getFields().stream()
                    .filter(field -> StringUtils.isNotBlank(field.getDatabaseType()))
                    .map(BusinessObjectTableFieldMappingVO::getColumnName)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
            if (version.getModelSnapshot() != null && version.getModelSnapshot().getFields() != null) {
                for (LowcodeFieldSchema field : version.getModelSnapshot().getFields()) {
                    if (field != null && StringUtils.isNotBlank(field.getColumnName())
                            && !currentColumns.contains(field.getColumnName())) {
                        throw new BusinessException("历史版本依赖当前不存在的数据库字段: " + field.getColumnName());
                    }
                }
            }
        }
        Set<Long> currentEntryIds = businessAppService.listByApplicationId(applicationId).stream()
                .map(AiBusinessApp::getId).collect(Collectors.toSet());
        for (Map<String, Object> entry : listOfMap(snapshot.get("entries"))) {
            Long entryId = longValue(entry.get("id"));
            if (entryId == null || !currentEntryIds.contains(entryId)) {
                throw new BusinessException("历史版本依赖的访问入口已不存在: " + entryId);
            }
        }
        Map<Long, AiBusinessExtension> extensions = extensionMapper
                .selectByApplicationId(resolveTenantId(), applicationId).stream()
                .collect(Collectors.toMap(AiBusinessExtension::getId, Function.identity()));
        for (Map<String, Object> extension : listOfMap(snapshot.get("extensions"))) {
            Long extensionId = longValue(extension.get("id"));
            Integer enabledVersion = integerValue(extension.get("enabledVersion"));
            if (!extensions.containsKey(extensionId)
                    || enabledVersion == null
                    || extensionVersionMapper.selectVersion(resolveTenantId(), extensionId, enabledVersion) == null) {
                throw new BusinessException("历史版本依赖的扩展版本已不存在: " + extensionId);
            }
        }
        Set<Long> currentBindings = bindingMapper.selectByApplication(resolveTenantId(), applicationId).stream()
                .map(AiBusinessBinding::getId).collect(Collectors.toSet());
        for (Map<String, Object> binding : listOfMap(snapshot.get("bindings"))) {
            Long bindingId = longValue(binding.get("id"));
            if (bindingId == null || !currentBindings.contains(bindingId)) {
                throw new BusinessException("历史版本依赖的应用挂接已不存在: " + bindingId);
            }
        }
    }

    private void prepareRollbackCandidate(Map<String, Object> snapshot, Integer sourceVersionNo) {
        List<Map<String, Object>> sourceObjects = listOfMap(snapshot.get("publishedObjectVersions"));
        snapshot.put("rollbackSourceObjectVersions", sourceObjects);
        snapshot.put("publishedObjectVersions", new ArrayList<>());
        snapshot.put("operationType", "ROLLBACK");
        snapshot.put("sourceVersionNo", sourceVersionNo);
    }

    private BusinessApplicationAssetSelectionVO resolveSelection(Map<String, Object> snapshot) {
        Object selectionValue = snapshot.get("selection");
        if (selectionValue != null) {
            return objectMapper.convertValue(selectionValue, BusinessApplicationAssetSelectionVO.class);
        }
        BusinessApplicationAssetSelectionVO selection = new BusinessApplicationAssetSelectionVO();
        selection.setObjectIds(listOfMap(snapshot.get("objects")).stream()
                .map(item -> longValue(item.get("objectId"))).filter(java.util.Objects::nonNull).toList());
        selection.setEntryIds(listOfMap(snapshot.get("entries")).stream()
                .map(item -> longValue(item.get("id"))).filter(java.util.Objects::nonNull).toList());
        selection.setExtensionIds(listOfMap(snapshot.get("extensions")).stream()
                .map(item -> longValue(item.get("id"))).filter(java.util.Objects::nonNull).toList());
        selection.setIncludeAutomation(true);
        return selection;
    }

    private void restoreBindings(Long applicationId, List<Map<String, Object>> snapshots) {
        for (Map<String, Object> snapshot : snapshots) {
            Long bindingId = longValue(snapshot.get("id"));
            String config = writeOptionalJson(snapshot.get("bindingConfig"));
            int changed = bindingMapper.restoreApplicationBinding(resolveTenantId(), applicationId, bindingId,
                    config, integerValue(snapshot.get("status")), integerValue(snapshot.get("sortOrder")));
            if (changed == 0) {
                throw new BusinessException("应用挂接恢复失败: " + bindingId);
            }
        }
    }

    private void restoreExtensions(Long applicationId, List<Map<String, Object>> snapshots) {
        for (Map<String, Object> snapshot : snapshots) {
            Long extensionId = longValue(snapshot.get("id"));
            Integer enabledVersion = integerValue(snapshot.get("enabledVersion"));
            String status = StringUtils.defaultIfBlank(text(snapshot.get("status")), "ENABLED");
            if (extensionMapper.restoreEnabledVersion(resolveTenantId(), applicationId, extensionId,
                    status, enabledVersion) == 0) {
                throw new BusinessException("扩展运行版本恢复失败: " + extensionId);
            }
        }
    }

    private Map<Long, Long> objectVersionMap(Object value) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> item : listOfMap(value)) {
            Long objectId = longValue(item.get("objectId"));
            Long versionId = longValue(item.get("designVersionId"));
            if (objectId != null && versionId != null) {
                result.put(objectId, versionId);
            }
        }
        return result;
    }

    private List<Map<String, Object>> objectVersionList(Map<Long, Long> values) {
        return values.entrySet().stream().map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("objectId", String.valueOf(item.getKey()));
            value.put("designVersionId", String.valueOf(item.getValue()));
            return value;
        }).toList();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return list.stream().filter(Map.class::isInstance)
                .map(item -> objectMapper.convertValue(item, new TypeReference<LinkedHashMap<String, Object>>() { }))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private String writeOptionalJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("历史应用挂接配置格式不正确");
        }
    }

    private BusinessApplicationPublishResultVO fail(AiBusinessApplicationPublishRun run, String step,
                                                    String code, String message) {
        return toResult(runService.markFailed(run,
                StringUtils.defaultIfBlank(step, BusinessApplicationPublishStep.PRECHECK), code, message), message);
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

    private String existingRunMessage(AiBusinessApplicationPublishRun run) {
        return switch (run.getRunStatus()) {
            case BusinessApplicationPublishStatus.SUCCESS -> "相同幂等回滚已成功完成";
            case BusinessApplicationPublishStatus.PARTIAL -> "相同幂等回滚部分完成，请执行恢复";
            case BusinessApplicationPublishStatus.FAILED -> "相同幂等回滚已失败，请修复后执行恢复";
            default -> "相同幂等回滚正在执行";
        };
    }

    private String safeMessage(BusinessException exception) {
        return StringUtils.abbreviate(StringUtils.defaultIfBlank(exception.getMessage(), "应用回滚失败"), 500);
    }

    private Object firstNonNull(Object first, Object second) {
        return first == null ? second : first;
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

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }
}
