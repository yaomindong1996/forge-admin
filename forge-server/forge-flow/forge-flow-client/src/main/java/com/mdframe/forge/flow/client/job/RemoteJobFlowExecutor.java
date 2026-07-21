package com.mdframe.forge.flow.client.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClientException;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import com.mdframe.forge.starter.job.flow.JobFlowExecutor;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 通过受控出站通道调用独立 Flow 服务。
 */
public class RemoteJobFlowExecutor implements JobFlowExecutor {

    private static final TypeReference<FlowResult<JobFlowBindingSnapshot>> BINDING_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<FlowResult<JobFlowExecutionResult>> EXECUTION_TYPE =
            new TypeReference<>() {
            };

    private final SecureOutboundClient outboundClient;
    private final ObjectMapper objectMapper;
    private final JobFlowRemoteProperties properties;

    public RemoteJobFlowExecutor(SecureOutboundClient outboundClient,
                                 ObjectMapper objectMapper,
                                 JobFlowRemoteProperties properties) {
        this.outboundClient = outboundClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public JobFlowBindingSnapshot validateBinding(String modelKey, Integer modelVersion) {
        requireConfigured();
        if (modelKey == null || modelKey.isBlank() || modelVersion == null || modelVersion <= 0) {
            throw new FlowClientException("流程模型Key和版本不能为空");
        }
        FlowResult<JobFlowBindingSnapshot> response = execute(
                post("/api/flow/job/bindings/validate",
                        Map.of("modelKey", modelKey.trim(), "modelVersion", modelVersion)),
                BINDING_TYPE);
        if (response.getData() == null) {
            throw new FlowClientException("流程服务未返回绑定快照");
        }
        return response.getData();
    }

    @Override
    public JobFlowExecutionResult start(JobFlowExecutionRequest request) {
        requireConfigured();
        try {
            FlowResult<JobFlowExecutionResult> response = execute(
                    post("/api/flow/job/executions/start", request), EXECUTION_TYPE);
            if (response.getData() == null) {
                throw new FlowClientException("流程启动响应缺少实例信息");
            }
            return response.getData();
        } catch (ExplicitRemoteFailure exception) {
            throw exception;
        } catch (RuntimeException startFailure) {
            try {
                JobFlowExecutionResult recovered = findByBusinessKey(request.businessKey());
                if (recovered != null) {
                    return new JobFlowExecutionResult(
                            recovered.businessKey(), recovered.processInstanceId(), true);
                }
            } catch (RuntimeException recoveryFailure) {
                startFailure.addSuppressed(recoveryFailure);
            }
            throw new FlowClientException("流程启动响应状态未知，按businessKey恢复失败", startFailure);
        }
    }

    @Override
    public JobFlowExecutionResult findByBusinessKey(String businessKey) {
        requireConfigured();
        if (businessKey == null || businessKey.isBlank()) {
            throw new FlowClientException("任务流程businessKey不能为空");
        }
        String encodedBusinessKey = UriUtils.encode(
                businessKey.trim(), StandardCharsets.UTF_8);
        String url = UriComponentsBuilder.fromUriString(endpoint("/api/flow/job/executions/status"))
                .queryParam("businessKey", encodedBusinessKey)
                .build(true)
                .toUriString();
        FlowResult<JobFlowExecutionResult> response = execute(get(url), EXECUTION_TYPE);
        return response.getData();
    }

    private OutboundRequest post(String path, Object body) {
        try {
            return OutboundRequest.builder()
                    .scene(OutboundScenes.FLOW_API)
                    .url(endpoint(path))
                    .method("POST")
                    .headers(headers())
                    .contentType("application/json")
                    .body(objectMapper.writeValueAsBytes(body))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new FlowClientException("任务流程请求序列化失败", exception);
        }
    }

    private OutboundRequest get(String url) {
        return OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(url)
                .method("GET")
                .headers(headers())
                .build();
    }

    private Map<String, String> headers() {
        return Map.of(
                "Authorization", "Bearer " + properties.getToken().trim(),
                "Accept", "application/json");
    }

    private <T> FlowResult<T> execute(OutboundRequest request,
                                      TypeReference<FlowResult<T>> responseType) {
        OutboundResponse outboundResponse = outboundClient.execute(request);
        int statusCode = outboundResponse.getStatusCode();
        if (statusCode >= 400 && statusCode < 500) {
            throw new ExplicitRemoteFailure("流程服务HTTP调用失败: " + statusCode);
        }
        if (statusCode >= 500) {
            throw new FlowClientException("流程服务HTTP调用状态未知: " + statusCode);
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new ExplicitRemoteFailure("流程服务HTTP调用失败: " + statusCode);
        }
        FlowResult<T> response;
        try {
            response = objectMapper.readValue(outboundResponse.getBody(), responseType);
        } catch (IOException exception) {
            throw new FlowClientException("流程服务响应解析失败", exception);
        }
        if (response == null || !response.isSuccess()) {
            throw new ExplicitRemoteFailure("流程服务业务调用失败");
        }
        return response;
    }

    private String endpoint(String path) {
        String baseUrl = properties.getUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(path)
                .build()
                .toUriString();
    }

    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new FlowClientException("任务流程远程适配器未启用");
        }
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new FlowClientException("未配置独立Flow服务地址");
        }
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new FlowClientException("未配置独立Flow服务Token");
        }
    }

    private static final class ExplicitRemoteFailure extends FlowClientException {

        private ExplicitRemoteFailure(String message) {
            super(message);
        }
    }
}
