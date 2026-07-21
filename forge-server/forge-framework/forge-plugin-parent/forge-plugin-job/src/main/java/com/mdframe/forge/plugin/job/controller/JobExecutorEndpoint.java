package com.mdframe.forge.plugin.job.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 任务执行器端点
 * 供调度中心远程调用，执行本地Handler
 */
@Slf4j
@RestController
@RequestMapping("/job/executor")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.job", name = "executor-enabled", havingValue = "true")
public class JobExecutorEndpoint {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApplicationContext applicationContext;
    private final JobProperties jobProperties;

    /**
     * 执行任务
     */
    @PostMapping("/execute")
    @SaIgnore
    @ApiPermissionIgnore
    public ResponseEntity<RespInfo<String>> execute(
            @RequestHeader(value = AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody ExecuteRequest request) {
        String configuredToken;
        try {
            configuredToken = jobProperties.validatedExecutorToken();
        } catch (IllegalStateException exception) {
            log.error("任务执行器端点已开启但服务Token配置无效");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(RespInfo.error(503, "任务执行器不可用"));
        }
        if (!matchesBearerToken(authorization, configuredToken)) {
            log.warn("拒绝未通过服务认证的远程任务执行请求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RespInfo.error(401, "执行器认证失败"));
        }
        try {
            log.info("接收远程任务执行请求: handler={}", request.getHandlerName());
            IJobExecutor executor = applicationContext.getBean(request.getHandlerName(), IJobExecutor.class);
            String result = executor.execute(request.getParam());
            log.info("远程任务执行成功: handler={}", request.getHandlerName());
            return ResponseEntity.ok(RespInfo.success(result));
        } catch (Exception e) {
            log.error("远程任务执行失败: handler={}, exceptionType={}",
                    request.getHandlerName(), e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RespInfo.error("任务执行失败"));
        }
    }

    private boolean matchesBearerToken(String authorization, String configuredToken) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        String suppliedToken = authorization.substring(BEARER_PREFIX.length());
        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }

    @Data
    public static class ExecuteRequest {

        @NotBlank(message = "执行Handler不能为空")
        @Size(max = 200, message = "执行Handler长度不能超过200个字符")
        private String handlerName;

        @Size(max = 1048576, message = "任务参数不能超过1MB")
        private String param;
    }
}
