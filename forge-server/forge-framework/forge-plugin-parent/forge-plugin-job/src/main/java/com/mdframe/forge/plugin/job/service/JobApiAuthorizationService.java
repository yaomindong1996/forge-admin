package com.mdframe.forge.plugin.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import com.mdframe.forge.plugin.job.vo.JobOpenApiSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobApiAuthorizationService {

    private final SysJobConfigMapper jobConfigMapper;
    private final SysJobLogMapper jobLogMapper;

    public void requireScope(JobApiPrincipal principal, String scope) {
        if (principal == null || scope == null || !principal.scopes().contains(scope)) {
            throw JobOpenApiException.forbidden("insufficient_scope");
        }
    }

    public Page<JobOpenApiSummaryVO> listJobs(JobApiPrincipal principal, int pageNum, int pageSize) {
        return jobConfigMapper.selectOpenJobPage(
                new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
                principal.jobIds(), principal.jobGroups());
    }

    public JobOpenApiSummaryVO requireJob(JobApiPrincipal principal, Long jobId) {
        requirePositiveId(jobId, "invalid_job_id");
        JobOpenApiSummaryVO job = jobConfigMapper.selectOpenJobById(
                jobId, principal.jobIds(), principal.jobGroups());
        if (job != null) {
            return job;
        }
        throw jobConfigMapper.countOpenJobById(jobId) > 0
                ? JobOpenApiException.forbidden("job_access_denied")
                : JobOpenApiException.notFound("job_not_found");
    }

    public JobApiTriggerTarget requireTriggerTarget(JobApiPrincipal principal, Long jobId) {
        requirePositiveId(jobId, "invalid_job_id");
        JobApiTriggerTarget target = jobConfigMapper.selectOpenTriggerTarget(
                jobId, principal.jobIds(), principal.jobGroups());
        if (target != null) {
            return target;
        }
        throw jobConfigMapper.countOpenJobById(jobId) > 0
                ? JobOpenApiException.forbidden("job_access_denied")
                : JobOpenApiException.notFound("job_not_found");
    }

    public JobOpenApiExecutionVO requireExecution(JobApiPrincipal principal, Long executionId) {
        requirePositiveId(executionId, "invalid_execution_id");
        JobOpenApiExecutionVO execution = jobLogMapper.selectOpenExecutionById(
                executionId, principal.jobIds(), principal.jobGroups());
        if (execution != null) {
            return execution;
        }
        throw jobLogMapper.countOpenExecutionById(executionId) > 0
                ? JobOpenApiException.forbidden("execution_access_denied")
                : JobOpenApiException.notFound("execution_not_found");
    }

    private void requirePositiveId(Long id, String errorCode) {
        if (id == null || id <= 0) {
            throw JobOpenApiException.badRequest(errorCode);
        }
    }
}
