package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStep;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationPublishRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationPublishRunMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishRunVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishStepVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 发布运行单的版本预留、步骤证据和恢复状态服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationPublishRunService
        extends ServiceImpl<BusinessApplicationPublishRunMapper, AiBusinessApplicationPublishRun> {

    private static final Map<String, String> STEP_NAMES = Map.of(
            BusinessApplicationPublishStep.PRECHECK, "发布预检查",
            BusinessApplicationPublishStep.SNAPSHOT, "准备快照",
            BusinessApplicationPublishStep.OBJECTS, "发布业务对象",
            BusinessApplicationPublishStep.ENTRIES, "切换页面入口",
            BusinessApplicationPublishStep.EXTENSIONS, "启用业务扩展",
            BusinessApplicationPublishStep.COMMIT, "提交应用版本"
    );

    private final ObjectMapper objectMapper;
    private final BusinessApplicationVersionMapper versionMapper;

    @Transactional(rollbackFor = Exception.class)
    public AiBusinessApplicationPublishRun reserve(Long applicationId,
                                                   String idempotencyKey,
                                                   String operationType,
                                                   Integer sourceVersionNo,
                                                   BusinessApplicationSnapshotService.SnapshotBundle snapshot,
                                                   BusinessApplicationAssetSelectionVO selection) {
        String key = normalizeIdempotencyKey(idempotencyKey);
        if (baseMapper.lockApplication(resolveTenantId(), applicationId) == null) {
            throw new BusinessException("业务应用不存在");
        }
        AiBusinessApplicationPublishRun existing = baseMapper.selectByIdempotencyKey(
                resolveTenantId(), applicationId, key);
        if (existing != null) {
            return existing;
        }
        int maxVersion = Math.max(value(versionMapper.selectMaxVersionNo(resolveTenantId(), applicationId)),
                value(baseMapper.selectMaxTargetVersionNo(resolveTenantId(), applicationId)));
        AiBusinessApplicationPublishRun run = new AiBusinessApplicationPublishRun();
        run.setTenantId(resolveTenantId());
        run.setApplicationId(applicationId);
        run.setIdempotencyKey(key);
        run.setOperationType(operationType);
        run.setTargetVersionNo(maxVersion + 1);
        run.setSourceVersionNo(sourceVersionNo);
        run.setRunStatus(BusinessApplicationPublishStatus.CREATED);
        run.setCurrentStep(BusinessApplicationPublishStep.PRECHECK);
        run.setSnapshotJson(snapshot.json());
        run.setSnapshotHash(snapshot.hash());
        run.setSelectionJson(writeJson(selection));
        run.setStepResultsJson(writeJson(initialSteps()));
        run.setAttemptCount(1);
        run.setStartedBy(resolveUserId());
        run.setStartedTime(LocalDateTime.now());
        save(run);
        return run;
    }

    public boolean tryClaimCreated(Long applicationId, Long runId) {
        return baseMapper.claimCreated(resolveTenantId(), applicationId, runId, resolveUserId()) == 1;
    }

    public AiBusinessApplicationPublishRun requireRun(Long applicationId, Long runId) {
        AiBusinessApplicationPublishRun run = baseMapper.selectRunById(resolveTenantId(), applicationId, runId);
        if (run == null) {
            throw new BusinessException("应用发布运行单不存在");
        }
        return run;
    }

    public AiBusinessApplicationPublishRun findByIdempotencyKey(Long applicationId, String idempotencyKey) {
        return baseMapper.selectByIdempotencyKey(resolveTenantId(), applicationId,
                normalizeIdempotencyKey(idempotencyKey));
    }

    public List<BusinessApplicationPublishRunVO> list(Long applicationId) {
        return baseMapper.selectRuns(resolveTenantId(), applicationId, 50).stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AiBusinessApplicationPublishRun beginRecovery(Long applicationId, Long runId) {
        AiBusinessApplicationPublishRun run = requireRun(applicationId, runId);
        if (BusinessApplicationPublishStatus.SUCCESS.equals(run.getRunStatus())) {
            return run;
        }
        if (BusinessApplicationPublishStatus.RUNNING.equals(run.getRunStatus())) {
            throw new BusinessException("发布运行单仍在执行，不能重复恢复");
        }
        if (baseMapper.incrementAttempt(resolveTenantId(), applicationId, runId, resolveUserId()) == 0) {
            throw new BusinessException("发布运行单状态已变化，请刷新后重试");
        }
        return requireRun(applicationId, runId);
    }

    public AiBusinessApplicationPublishRun markStepRunning(AiBusinessApplicationPublishRun run, String stepCode) {
        List<BusinessApplicationPublishStepVO> steps = readSteps(run.getStepResultsJson());
        BusinessApplicationPublishStepVO step = findStep(steps, stepCode);
        step.setStatus("RUNNING");
        step.setMessage(null);
        step.setStartedTime(LocalDateTime.now());
        step.setFinishedTime(null);
        return update(run, BusinessApplicationPublishStatus.RUNNING, stepCode, steps,
                null, null, null, null, null, null);
    }

    public AiBusinessApplicationPublishRun markStepSuccess(AiBusinessApplicationPublishRun run,
                                                           String stepCode, String message) {
        List<BusinessApplicationPublishStepVO> steps = readSteps(run.getStepResultsJson());
        BusinessApplicationPublishStepVO step = findStep(steps, stepCode);
        step.setStatus("SUCCESS");
        step.setMessage(StringUtils.abbreviate(message, 500));
        step.setFinishedTime(LocalDateTime.now());
        return update(run, BusinessApplicationPublishStatus.RUNNING, stepCode, steps,
                null, null, null, null, null, null);
    }

    public AiBusinessApplicationPublishRun updateSnapshot(AiBusinessApplicationPublishRun run,
                                                          BusinessApplicationSnapshotService.SnapshotBundle snapshot) {
        List<BusinessApplicationPublishStepVO> steps = readSteps(run.getStepResultsJson());
        return update(run, run.getRunStatus(), run.getCurrentStep(), steps,
                snapshot.json(), snapshot.hash(), run.getResultVersionId(),
                run.getErrorCode(), run.getErrorSummary(), run.getFinishedTime());
    }

    public boolean isStepComplete(AiBusinessApplicationPublishRun run, String stepCode) {
        return readSteps(run.getStepResultsJson()).stream()
                .anyMatch(step -> stepCode.equals(step.getStepCode()) && "SUCCESS".equals(step.getStatus()));
    }

    public AiBusinessApplicationPublishRun markFailed(AiBusinessApplicationPublishRun run,
                                                      String stepCode, String errorCode, String errorSummary) {
        List<BusinessApplicationPublishStepVO> steps = readSteps(run.getStepResultsJson());
        BusinessApplicationPublishStepVO step = findStep(steps, stepCode);
        step.setStatus("FAILED");
        step.setMessage(StringUtils.abbreviate(errorSummary, 500));
        step.setFinishedTime(LocalDateTime.now());
        boolean hasSideEffect = Set.of(BusinessApplicationPublishStep.OBJECTS,
                BusinessApplicationPublishStep.ENTRIES, BusinessApplicationPublishStep.EXTENSIONS,
                BusinessApplicationPublishStep.COMMIT).contains(stepCode)
                || steps.stream().anyMatch(item -> "SUCCESS".equals(item.getStatus())
                && Set.of(BusinessApplicationPublishStep.OBJECTS, BusinessApplicationPublishStep.ENTRIES,
                BusinessApplicationPublishStep.EXTENSIONS).contains(item.getStepCode()));
        String status = hasSideEffect ? BusinessApplicationPublishStatus.PARTIAL
                : BusinessApplicationPublishStatus.FAILED;
        return update(run, status, stepCode, steps, null, null, null,
                errorCode, StringUtils.abbreviate(errorSummary, 500), LocalDateTime.now());
    }

    public AiBusinessApplicationPublishRun markSuccess(AiBusinessApplicationPublishRun run,
                                                       Long resultVersionId,
                                                       BusinessApplicationSnapshotService.SnapshotBundle finalSnapshot) {
        List<BusinessApplicationPublishStepVO> steps = readSteps(run.getStepResultsJson());
        BusinessApplicationPublishStepVO commit = findStep(steps, BusinessApplicationPublishStep.COMMIT);
        commit.setStatus("SUCCESS");
        commit.setMessage("不可变应用版本已提交");
        commit.setFinishedTime(LocalDateTime.now());
        return update(run, BusinessApplicationPublishStatus.SUCCESS, BusinessApplicationPublishStep.COMMIT, steps,
                finalSnapshot.json(), finalSnapshot.hash(), resultVersionId,
                null, null, LocalDateTime.now());
    }

    public BusinessApplicationAssetSelectionVO readSelection(AiBusinessApplicationPublishRun run) {
        try {
            return objectMapper.readValue(run.getSelectionJson(), BusinessApplicationAssetSelectionVO.class);
        } catch (Exception e) {
            throw new BusinessException("应用发布选择快照格式不正确");
        }
    }

    public BusinessApplicationPublishRunVO toVO(AiBusinessApplicationPublishRun run) {
        BusinessApplicationPublishRunVO vo = new BusinessApplicationPublishRunVO();
        vo.setId(run.getId());
        vo.setApplicationId(run.getApplicationId());
        vo.setOperationType(run.getOperationType());
        vo.setTargetVersionNo(run.getTargetVersionNo());
        vo.setSourceVersionNo(run.getSourceVersionNo());
        vo.setRunStatus(run.getRunStatus());
        vo.setCurrentStep(run.getCurrentStep());
        vo.setResultVersionId(run.getResultVersionId());
        vo.setErrorCode(run.getErrorCode());
        vo.setErrorSummary(run.getErrorSummary());
        vo.setAttemptCount(run.getAttemptCount());
        vo.setStartedBy(run.getStartedBy());
        vo.setStartedTime(run.getStartedTime());
        vo.setFinishedTime(run.getFinishedTime());
        vo.setSteps(readSteps(run.getStepResultsJson()));
        return vo;
    }

    private AiBusinessApplicationPublishRun update(
            AiBusinessApplicationPublishRun run,
            String status,
            String stepCode,
            List<BusinessApplicationPublishStepVO> steps,
            String snapshotJson,
            String snapshotHash,
            Long resultVersionId,
            String errorCode,
            String errorSummary,
            LocalDateTime finishedTime) {
        String stepResultsJson = writeJson(steps);
        if (baseMapper.updateProgress(resolveTenantId(), run.getApplicationId(), run.getId(), status,
                stepCode, stepResultsJson, snapshotJson, snapshotHash, resultVersionId,
                errorCode, errorSummary, finishedTime) == 0) {
            throw new BusinessException("应用发布运行单更新失败");
        }
        run.setRunStatus(status);
        run.setCurrentStep(stepCode);
        run.setStepResultsJson(stepResultsJson);
        if (snapshotJson != null) {
            run.setSnapshotJson(snapshotJson);
        }
        if (snapshotHash != null) {
            run.setSnapshotHash(snapshotHash);
        }
        run.setResultVersionId(resultVersionId);
        run.setErrorCode(errorCode);
        run.setErrorSummary(errorSummary);
        run.setFinishedTime(finishedTime);
        return run;
    }

    private List<BusinessApplicationPublishStepVO> initialSteps() {
        List<BusinessApplicationPublishStepVO> steps = new ArrayList<>();
        for (String code : BusinessApplicationPublishStep.ORDERED_STEPS) {
            BusinessApplicationPublishStepVO step = new BusinessApplicationPublishStepVO();
            step.setStepCode(code);
            step.setStepName(STEP_NAMES.get(code));
            step.setStatus("PENDING");
            steps.add(step);
        }
        return steps;
    }

    private BusinessApplicationPublishStepVO findStep(List<BusinessApplicationPublishStepVO> steps, String stepCode) {
        return steps.stream().filter(item -> stepCode.equals(item.getStepCode())).findFirst()
                .orElseThrow(() -> new BusinessException("应用发布步骤不存在: " + stepCode));
    }

    private List<BusinessApplicationPublishStepVO> readSteps(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<ArrayList<BusinessApplicationPublishStepVO>>() { });
        } catch (Exception e) {
            throw new BusinessException("应用发布步骤结果格式不正确");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("应用发布运行单序列化失败");
        }
    }

    private String normalizeIdempotencyKey(String value) {
        String key = StringUtils.trimToNull(value);
        if (key == null || key.length() < 16 || key.length() > 128
                || !key.matches("^[A-Za-z0-9][A-Za-z0-9._:-]{15,127}$")) {
            throw new BusinessException("发布幂等键必须为16-128位字母、数字、点、冒号、下划线或短横线");
        }
        return key;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    private Long resolveUserId() {
        try {
            Long userId = SessionHelper.getUserId();
            return userId == null ? 1L : userId;
        } catch (Exception e) {
            return 1L;
        }
    }
}
