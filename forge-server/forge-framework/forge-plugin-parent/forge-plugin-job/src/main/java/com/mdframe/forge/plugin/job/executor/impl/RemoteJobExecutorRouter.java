package com.mdframe.forge.plugin.job.executor.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.executor.IJobExecutorRouter;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分布式模式下通过受控 HTTP 调用远程任务执行器。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "forge.job", name = "deploy-mode", havingValue = "DISTRIBUTED")
@RequiredArgsConstructor
public class RemoteJobExecutorRouter implements IJobExecutorRouter {

    private static final int SUCCESS_CODE = 200;

    private final JobProperties jobProperties;
    private final SecureOutboundClient outboundClient;
    private final ObjectMapper objectMapper;

    @Override
    public String route(String executeMode,
                        String executorBean,
                        String executorMethod,
                        String executorHandler,
                        String executorService,
                        String jobParam) {
        if (!"RPC".equals(executeMode)) {
            throw new UnsupportedOperationException("远程路由仅支持RPC模式");
        }
        if (executorService == null || executorService.isBlank()) {
            throw new IllegalArgumentException("未指定执行器服务名称");
        }
        return executeRpcMode(executorService.trim(), executorHandler, jobParam);
    }

    @Override
    public boolean support(String executeMode) {
        return "RPC".equals(executeMode);
    }

    private String executeRpcMode(String serviceName, String handlerName, String param) {
        byte[] requestBody = serializeRequest(handlerName, param);
        String token = jobProperties.validatedExecutorToken();
        log.info("调用远程任务执行器: service={}, handler={}", serviceName, handlerName);

        OutboundResponse response = outboundClient.execute(OutboundRequest.builder()
                .scene(OutboundScenes.JOB_RPC)
                .url(buildServiceUrl(serviceName))
                .method("POST")
                .headers(Map.of(
                        "Authorization", "Bearer " + token,
                        "Accept", "application/json"))
                .contentType("application/json")
                .body(requestBody)
                .build());
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new IllegalStateException("远程任务执行HTTP失败: " + response.getStatusCode());
        }
        JsonNode body = parseResponse(response);
        if (!body.has("code") || body.get("code").asInt() != SUCCESS_CODE) {
            throw new IllegalStateException("远程任务执行失败");
        }
        log.info("远程任务执行成功: service={}, handler={}", serviceName, handlerName);
        JsonNode data = body.get("data");
        return data == null || data.isNull() ? null : data.asText();
    }

    private byte[] serializeRequest(String handlerName, String param) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("handlerName", handlerName);
        requestBody.put("param", param);
        try {
            return objectMapper.writeValueAsBytes(requestBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("远程任务请求序列化失败", exception);
        }
    }

    private JsonNode parseResponse(OutboundResponse response) {
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception exception) {
            throw new IllegalStateException("远程任务执行响应格式不合法", exception);
        }
    }

    private String buildServiceUrl(String serviceName) {
        return "http://" + serviceName + "/job/executor/execute";
    }
}
