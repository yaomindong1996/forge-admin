package com.mdframe.forge.flow.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程服务客户端
 * 供其他服务调用流程服务
 * 
 * @author forge
 */
@Setter
public class FlowClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 流程服务地址
     */
    private String flowServiceUrl = "http://localhost:8081";

    public FlowClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public FlowClient(String flowServiceUrl) {
        this();
        this.flowServiceUrl = flowServiceUrl;
    }

    public FlowClient(RestTemplate restTemplate, String flowServiceUrl) {
        this.restTemplate = restTemplate;
        this.flowServiceUrl = flowServiceUrl;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 流程实例接口 ====================

    /**
     * 发起流程
     */
    public FlowResult<String> startProcess(String modelKey, Map<String, Object> params) {
        String url = flowServiceUrl + "/api/flow/instance/start/" + modelKey;
        return post(url, params, new TypeReference<FlowResult<String>>() {});
    }

    /**
     * 获取流程状态
     */
    public FlowResult<Map<String, Object>> getProcessStatus(String businessKey) {
        String url = flowServiceUrl + "/api/flow/instance/status/" + businessKey;
        return get(url, new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    /**
     * 终止流程
     */
    public FlowResult<Void> terminateProcess(String businessKey, String userId, String reason) {
        String url = flowServiceUrl + "/api/flow/instance/terminate/" + businessKey;
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("reason", reason);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    // ==================== 任务接口 ====================

    /**
     * 获取待办任务
     */
    public FlowResult<Map<String, Object>> getTodoTasks(String userId, int pageNum, int pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/task/todo")
                .queryParam("userId", userId)
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .toUriString();
        return get(url, new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    /**
     * 签收任务
     */
    public FlowResult<Void> claimTask(String taskId, String userId) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/task/claim")
                .queryParam("taskId", taskId)
                .queryParam("userId", userId)
                .toUriString();
        return post(url, null, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 审批通过
     */
    public FlowResult<Void> approve(String taskId, String userId, String comment, Map<String, Object> variables) {
        String url = flowServiceUrl + "/api/flow/task/approve";
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("userId", userId);
        params.put("comment", comment);
        params.put("variables", variables);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 审批驳回
     */
    public FlowResult<Void> reject(String taskId, String userId, String comment) {
        String url = flowServiceUrl + "/api/flow/task/reject";
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("userId", userId);
        params.put("comment", comment);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 转办
     */
    public FlowResult<Void> delegate(String taskId, String userId, String targetUserId, String comment) {
        String url = flowServiceUrl + "/api/flow/task/delegate";
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("userId", userId);
        params.put("targetUserId", targetUserId);
        params.put("comment", comment);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 催办
     */
    public FlowResult<Void> remind(String taskId) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/task/remind")
                .queryParam("taskId", taskId)
                .toUriString();
        return post(url, null, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 获取流程图
     */
    public byte[] getProcessDiagram(String processInstanceId) {
        String url = flowServiceUrl + "/api/flow/task/diagram/" + processInstanceId;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
    }

    // ==================== 模型接口 ====================

    /**
     * 获取流程模型列表
     */
    public FlowResult<Map<String, Object>> getModelList(String category, Integer status) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/model/list")
                .queryParamIfPresent("category", category != null ? java.util.Optional.of(category) : java.util.Optional.empty())
                .queryParamIfPresent("status", status != null ? java.util.Optional.of(status) : java.util.Optional.empty())
                .toUriString();
        return get(url, new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    // ==================== HTTP 方法封装 ====================

    private <T> T get(String url, TypeReference<T> typeReference) {
        try {
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readValue(response, typeReference);
        } catch (Exception e) {
            throw new FlowClientException("调用流程服务失败: " + e.getMessage(), e);
        }
    }

    private <T> T post(String url, Object body, TypeReference<T> typeReference) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(
                    body != null ? objectMapper.writeValueAsString(body) : null, 
                    headers);
            
            String response = restTemplate.postForObject(url, entity, String.class);
            return objectMapper.readValue(response, typeReference);
        } catch (Exception e) {
            throw new FlowClientException("调用流程服务失败: " + e.getMessage(), e);
        }
    }
}