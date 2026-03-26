package com.mdframe.forge.flow.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Setter;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 流程服务客户端
 * <p>
 * 供其他微服务/模块通过 HTTP 调用流程服务。
 * 使用 Spring Boot Starter 时由 {@link FlowClientAutoConfiguration} 自动注入，
 * 无需手动 new。
 * <pre>
 * // 发起流程
 * flowClient.startProcess("leave", "leave:2024:001", "张三的请假申请",
 *         Map.of("days", 3, "reason", "探亲"), "u001", "张三", "d01", "研发部");
 *
 * // 审批通过
 * flowClient.approve("task-id-001", "u002", "同意", null);
 * </pre>
 *
 * @author forge
 */
@Setter
public class FlowClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 流程服务地址，默认本地 */
    private String flowServiceUrl = "http://localhost:8080";

    /** Token（静态配置，如需鉴权透传，设置后每次请求携带 Authorization 头） */
    private String token;

    /**
     * 动态 Token 供应商（优先级高于静态 token 配置）
     * 引入 forge-starter-auth 时会自动注入 Sa-Token 实现
     */
    @Setter
    private FlowTokenProvider tokenProvider;

    public FlowClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public FlowClient(String flowServiceUrl) {
        this();
        this.flowServiceUrl = flowServiceUrl;
    }

    public FlowClient(RestTemplate restTemplate, String flowServiceUrl) {
        this.restTemplate = restTemplate;
        this.flowServiceUrl = flowServiceUrl;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public FlowClient(RestTemplate restTemplate, String flowServiceUrl, String token) {
        this(restTemplate, flowServiceUrl);
        this.token = token;
    }

    // ==================== 流程实例接口 ====================

    /**
     * 发起流程（简化版，不带业务类型）
     *
     * @param modelKey    流程模型 Key
     * @param businessKey 业务唯一标识（如 "leave:2024:001"）
     * @param title       流程标题
     * @param variables   流程变量
     * @param userId      发起人 ID
     * @param userName    发起人姓名
     * @param deptId      发起人部门 ID
     * @param deptName    发起人部门名称
     * @return 流程实例 ID
     */
    public FlowResult<String> startProcess(String modelKey, String businessKey, String title,
                                           Map<String, Object> variables,
                                           String userId, String userName,
                                           String deptId, String deptName) {
        return startProcess(modelKey, businessKey, null, title, variables, userId, userName, deptId, deptName);
    }

    /**
     * 发起流程（带业务类型）
     *
     * @param modelKey     流程模型 Key
     * @param businessKey  业务唯一标识
     * @param businessType 业务类型（可选，如 "leave"）
     * @param title        流程标题
     * @param variables    流程变量
     * @param userId       发起人 ID
     * @param userName     发起人姓名
     * @param deptId       发起人部门 ID
     * @param deptName     发起人部门名称
     * @return 流程实例 ID
     */
    public FlowResult<String> startProcess(String modelKey, String businessKey, String businessType,
                                           String title, Map<String, Object> variables,
                                           String userId, String userName,
                                           String deptId, String deptName) {
        String url = flowServiceUrl + "/api/flow/instance/start/" + modelKey;
        Map<String, Object> params = new HashMap<>();
        params.put("businessKey", businessKey);
        params.put("businessType", businessType);
        params.put("title", title);
        params.put("variables", variables);
        params.put("userId", userId);
        params.put("userName", userName);
        params.put("deptId", deptId);
        params.put("deptName", deptName);
        return post(url, params, new TypeReference<FlowResult<String>>() {});
    }

    /**
     * 获取流程实例状态
     *
     * @param businessKey 业务唯一标识
     */
    public FlowResult<Map<String, Object>> getProcessStatus(String businessKey) {
        String url = flowServiceUrl + "/api/flow/instance/status/" + businessKey;
        return get(url, new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    /**
     * 终止流程
     *
     * @param businessKey 业务唯一标识
     * @param userId      操作人 ID
     * @param reason      终止原因
     */
    public FlowResult<Void> terminateProcess(String businessKey, String userId, String reason) {
        String url = flowServiceUrl + "/api/flow/instance/terminate/" + businessKey;
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("reason", reason);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 撤回流程（发起人主动撤回）
     *
     * @param processInstanceId 流程实例 ID
     * @param userId            发起人 ID
     * @param comment           撤回原因
     */
    public FlowResult<Void> withdrawProcess(String processInstanceId, String userId, String comment) {
        String url = flowServiceUrl + "/api/flow/instance/withdraw";
        Map<String, Object> params = new HashMap<>();
        params.put("processInstanceId", processInstanceId);
        params.put("userId", userId);
        params.put("comment", comment);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 获取流程变量
     *
     * @param businessKey 业务唯一标识
     */
    public FlowResult<Map<String, Object>> getProcessVariables(String businessKey) {
        String url = flowServiceUrl + "/api/flow/instance/variables/" + businessKey;
        return get(url, new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    /**
     * 更新流程变量
     *
     * @param businessKey 业务唯一标识
     * @param variables   要更新的变量
     */
    public FlowResult<Void> updateProcessVariables(String businessKey, Map<String, Object> variables) {
        String url = flowServiceUrl + "/api/flow/instance/variables/" + businessKey;
        return post(url, variables, new TypeReference<FlowResult<Void>>() {});
    }

    // ==================== 任务接口 ====================

    /**
     * 获取我的待办任务
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
     * 获取我的已办任务
     */
    public FlowResult<Map<String, Object>> getDoneTasks(String userId, int pageNum, int pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/task/done")
                .queryParam("userId", userId)
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize)
                .toUriString();
        return get(url, new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    /**
     * 获取我发起的流程
     */
    public FlowResult<Map<String, Object>> getStartedTasks(String userId, int pageNum, int pageSize) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/task/started")
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
     *
     * @param taskId    任务 ID
     * @param userId    审批人 ID
     * @param comment   审批意见
     * @param variables 额外流程变量（可为 null）
     */
    public FlowResult<Void> approve(String taskId, String userId, String comment, Map<String, Object> variables) {
        String url = flowServiceUrl + "/api/flow/task/approve";
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("userId", userId);
        params.put("comment", comment);
        if (variables != null) params.put("variables", variables);
        return post(url, params, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 审批驳回
     *
     * @param taskId  任务 ID
     * @param userId  审批人 ID
     * @param comment 驳回意见
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
     * 转办任务
     *
     * @param taskId       任务 ID
     * @param userId       当前处理人 ID
     * @param targetUserId 目标处理人 ID
     * @param comment      转办说明
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
     * 催办任务
     *
     * @param taskId 任务 ID
     */
    public FlowResult<Void> remind(String taskId) {
        String url = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/task/remind")
                .queryParam("taskId", taskId)
                .toUriString();
        return post(url, null, new TypeReference<FlowResult<Void>>() {});
    }

    /**
     * 获取审批历史（评论列表）
     *
     * @param processInstanceId 流程实例 ID
     */
    public FlowResult<List<Map<String, Object>>> getProcessComments(String processInstanceId) {
        String url = flowServiceUrl + "/api/flow/task/comments/" + processInstanceId;
        return get(url, new TypeReference<FlowResult<List<Map<String, Object>>>>() {});
    }

    /**
     * 获取流程图（PNG 字节数组）
     *
     * @param processInstanceId 流程实例 ID
     * @return PNG 图片字节数组
     */
    public byte[] getProcessDiagram(String processInstanceId) {
        String url = flowServiceUrl + "/api/flow/task/diagram/" + processInstanceId;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
    }

    // ==================== 模型接口 ====================

    /**
     * 获取已部署的流程模型列表
     *
     * @param category 分类（可为 null）
     * @param status   状态（可为 null，1=已部署）
     */
    public FlowResult<Map<String, Object>> getModelList(String category, Integer status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(flowServiceUrl + "/api/flow/model/list");
        Optional.ofNullable(category).ifPresent(v -> builder.queryParam("category", v));
        Optional.ofNullable(status).ifPresent(v -> builder.queryParam("status", v));
        return get(builder.toUriString(), new TypeReference<FlowResult<Map<String, Object>>>() {});
    }

    // ==================== HTTP 方法封装 ====================

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 标识为内部服务调用，让流程服务端跳过请求体加解密（服务间直接传输明文 JSON）
        headers.set("X-Inner-Call", "true");
        // 优先使用动态 Token（来自当前请求上下文）实现用户 Token 透传
        String effectiveToken = null;
        if (tokenProvider != null) {
            try {
                effectiveToken = tokenProvider.getToken();
            } catch (Exception e) {
                // TokenProvider 获取失败时降级为静态 token
            }
        }
        if (effectiveToken == null || effectiveToken.isEmpty()) {
            effectiveToken = token;
        }
        if (effectiveToken != null && !effectiveToken.isEmpty()) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveToken);
        }
        return headers;
    }

    private <T> T get(String url, TypeReference<T> typeReference) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new FlowClientException("调用流程服务返回空响应体 [GET " + url + "]");
            }
            return objectMapper.readValue(body, typeReference);
        } catch (FlowClientException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowClientException("调用流程服务失败 [GET " + url + "]: " + e.getMessage(), e);
        }
    }
    
    private <T> T post(String url, Object body, TypeReference<T> typeReference) {
        try {
            String json = body != null ? objectMapper.writeValueAsString(body) : null;
            HttpEntity<String> entity = new HttpEntity<>(json, buildHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new FlowClientException("调用流程服务返回空响应体 [POST " + url + "]" +
                        "（HTTP " + response.getStatusCode().value() + "）请检查服务端日志");
            }
            return objectMapper.readValue(responseBody, typeReference);
        } catch (FlowClientException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowClientException("调用流程服务失败 [POST " + url + "]: " + e.getMessage(), e);
        }
    }
}
