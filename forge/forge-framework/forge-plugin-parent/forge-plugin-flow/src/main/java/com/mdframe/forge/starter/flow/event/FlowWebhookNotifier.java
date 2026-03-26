package com.mdframe.forge.starter.flow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程事件 HTTP Webhook 回调器
 *
 * <p>当流程状态发生变更时，向业务系统配置的 webhookUrl 发送 HTTP POST 回调请求，
 * 业务侧通过接收 Webhook 完成与流程引擎的解耦。</p>
 *
 * <h3>Webhook 请求格式</h3>
 * <pre>
 * POST {webhookUrl}
 * Content-Type: application/json
 * X-Flow-Event-Type: PROCESS_COMPLETED
 * X-Flow-Process-Key: leave-apply
 * X-Flow-Business-Key: BIZ_20240101_001
 *
 * {
 *   "eventType": "PROCESS_COMPLETED",
 *   "eventTime": "2024-01-01T10:00:00",
 *   "processInstanceId": "xxx",
 *   "processDefKey": "leave-apply",
 *   "businessKey": "BIZ_20240101_001",
 *   "businessType": "leave-apply",
 *   "title": "张三的请假申请",
 *   "applyUserId": "1",
 *   "applyUserName": "张三",
 *   ...
 * }
 * </pre>
 *
 * <h3>业务侧 Controller 示例</h3>
 * <pre>
 * {@literal @}PostMapping("/flow/callback")
 * public ResponseEntity&lt;Void&gt; onFlowEvent({@literal @}RequestBody FlowEventMessage event) {
 *     String eventType = event.getEventType();
 *     String businessKey = event.getBusinessKey();
 *     if ("PROCESS_COMPLETED".equals(eventType)) {
 *         // 审批通过后的业务处理
 *         leaveService.handleApproved(businessKey);
 *     } else if ("PROCESS_REJECTED".equals(eventType)) {
 *         // 审批驳回后的业务处理
 *         leaveService.handleRejected(businessKey);
 *     }
 *     return ResponseEntity.ok().build();
 * }
 * </pre>
 */
@Slf4j
@Component
public class FlowWebhookNotifier {

    /** Webhook 请求超时时间 */
    private static final int TIMEOUT_SECONDS = 10;

    /** 最大重试次数 */
    private static final int MAX_RETRY = 2;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FlowWebhookNotifier() {
        // 创建带超时配置的 RestTemplate
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis());
        this.restTemplate = new RestTemplate(factory);

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 异步发送 Webhook 回调（带重试）
     *
     * @param webhookUrl 回调 URL（配置在 FlowModel.webhookUrl）
     * @param message    流程事件消息
     */
    @Async("flowEventExecutor")
    public void notify(String webhookUrl, FlowEventMessage message) {
        if (!StringUtils.hasText(webhookUrl)) {
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                doSend(webhookUrl, message);
                log.info("[FlowWebhook] 回调成功(attempt={}): url={}, eventType={}, businessKey={}",
                        attempt, webhookUrl, message.getEventType(), message.getBusinessKey());
                return; // 成功则退出重试循环
            } catch (Exception e) {
                log.warn("[FlowWebhook] 回调失败(attempt={}/{}): url={}, eventType={}, error={}",
                        attempt, MAX_RETRY, webhookUrl, message.getEventType(), e.getMessage());
                if (attempt < MAX_RETRY) {
                    // 等待后重试（指数退避：1s, 2s）
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    log.error("[FlowWebhook] 回调最终失败，已放弃: url={}, eventType={}, businessKey={}",
                            webhookUrl, message.getEventType(), message.getBusinessKey(), e);
                }
            }
        }
    }

    /**
     * 执行实际的 HTTP 请求
     */
    private void doSend(String webhookUrl, FlowEventMessage message) throws JsonProcessingException {
        String body = objectMapper.writeValueAsString(message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 标识为内部服务调用，跳过业务侧的防重放、加解密等安全过滤器
        headers.set("X-Inner-Call", "true");
        // 携带流程事件元数据头，方便业务侧快速过滤，无需解析 body
        headers.set("X-Flow-Event-Type", message.getEventType());
        if (message.getProcessDefKey() != null) {
            headers.set("X-Flow-Process-Key", message.getProcessDefKey());
        }
        if (message.getBusinessKey() != null) {
            headers.set("X-Flow-Business-Key", message.getBusinessKey());
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("Webhook 返回非 2xx 状态码: " + response.getStatusCode());
        }
    }
}
