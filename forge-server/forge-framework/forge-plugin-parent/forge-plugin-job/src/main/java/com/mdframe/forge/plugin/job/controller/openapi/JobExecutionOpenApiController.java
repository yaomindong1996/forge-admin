package com.mdframe.forge.plugin.job.controller.openapi;

import com.mdframe.forge.plugin.job.constant.JobApiScopes;
import com.mdframe.forge.plugin.job.manager.JobApiRateLimitManager;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.service.JobApiAuthorizationService;
import com.mdframe.forge.plugin.job.service.JobApiTokenService;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/openapi/v1/executions")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "forge.job.open-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JobExecutionOpenApiController {

    private final JobApiTokenService tokenService;
    private final JobApiAuthorizationService authorizationService;
    private final JobApiRateLimitManager rateLimitManager;

    @GetMapping("/{id}")
    public RespInfo<JobOpenApiExecutionVO> detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        long startedAt = System.nanoTime();
        JobApiPrincipal principal = tokenService.authenticate(authorization);
        authorizationService.requireScope(principal, JobApiScopes.EXECUTIONS_READ);
        rateLimitManager.acquireRead(principal);
        JobOpenApiExecutionVO execution = authorizationService.requireExecution(principal, id);
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("定时任务开放API调用完成: keyId={}, jobId={}, executionId={}, resultCode=200, durationMs={}",
                principal.tokenKeyId(), execution.getJobId(), execution.getId(), durationMillis);
        return RespInfo.success(execution);
    }
}
