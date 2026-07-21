package com.mdframe.forge.plugin.job.controller.openapi;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.constant.JobApiScopes;
import com.mdframe.forge.plugin.job.manager.JobApiRateLimitManager;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.service.JobApiAuthorizationService;
import com.mdframe.forge.plugin.job.service.JobApiExecutionService;
import com.mdframe.forge.plugin.job.service.JobApiTokenService;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import com.mdframe.forge.plugin.job.vo.JobOpenApiSummaryVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/openapi/v1/jobs")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "forge.job.open-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JobOpenApiController {

    private final JobApiTokenService tokenService;
    private final JobApiAuthorizationService authorizationService;
    private final JobApiRateLimitManager rateLimitManager;
    private final JobApiExecutionService executionService;

    @GetMapping
    public RespInfo<Page<JobOpenApiSummaryVO>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        long startedAt = System.nanoTime();
        JobApiPrincipal principal = tokenService.authenticate(authorization);
        authorizationService.requireScope(principal, JobApiScopes.JOBS_READ);
        rateLimitManager.acquireRead(principal);
        Page<JobOpenApiSummaryVO> page = authorizationService.listJobs(
                principal, normalizePageNum(pageNum), normalizePageSize(pageSize));
        logSuccess(principal, null, null, startedAt);
        return RespInfo.success(page);
    }

    @GetMapping("/{id}")
    public RespInfo<JobOpenApiSummaryVO> detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        long startedAt = System.nanoTime();
        JobApiPrincipal principal = tokenService.authenticate(authorization);
        authorizationService.requireScope(principal, JobApiScopes.JOBS_READ);
        rateLimitManager.acquireRead(principal);
        JobOpenApiSummaryVO job = authorizationService.requireJob(principal, id);
        logSuccess(principal, id, null, startedAt);
        return RespInfo.success(job);
    }

    @PostMapping("/{id}/executions")
    public RespInfo<JobOpenApiExecutionVO> trigger(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable Long id) {
        long startedAt = System.nanoTime();
        JobApiPrincipal principal = tokenService.authenticate(authorization);
        authorizationService.requireScope(principal, JobApiScopes.JOBS_TRIGGER);
        rateLimitManager.acquireTrigger(principal);
        JobApiTriggerTarget target = authorizationService.requireTriggerTarget(principal, id);
        JobOpenApiExecutionVO execution = executionService.trigger(principal, target, idempotencyKey);
        logSuccess(principal, id, execution.getId(), startedAt);
        return RespInfo.success(execution);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null ? 1 : Math.max(pageNum, 1);
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
    }

    private void logSuccess(
            JobApiPrincipal principal, Long jobId, Long executionId, long startedAt) {
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("定时任务开放API调用完成: keyId={}, jobId={}, executionId={}, resultCode=200, durationMs={}",
                principal.tokenKeyId(), jobId, executionId, durationMillis);
    }
}
