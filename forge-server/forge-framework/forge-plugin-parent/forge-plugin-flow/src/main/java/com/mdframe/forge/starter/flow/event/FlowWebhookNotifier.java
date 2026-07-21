package com.mdframe.forge.starter.flow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
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

    /** 最大重试次数 */
    private static final int MAX_RETRY = 2;

    private static final long RETRY_DELAY_MILLIS = 1000L;

    private final SecureOutboundClient outboundClient;
    private final ObjectMapper objectMapper;
    private final long retryDelayMillis;

    @Autowired
    public FlowWebhookNotifier(SecureOutboundClient outboundClient) {
        this(outboundClient, createObjectMapper(), RETRY_DELAY_MILLIS);
    }

    FlowWebhookNotifier(SecureOutboundClient outboundClient, ObjectMapper objectMapper, long retryDelayMillis) {
        this.outboundClient = outboundClient;
        this.objectMapper = objectMapper;
        this.retryDelayMillis = retryDelayMillis;
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
        String safeTarget = safeTarget(webhookUrl);

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                doSend(webhookUrl, message);
                log.info("[FlowWebhook] 回调成功(attempt={}): target={}, eventType={}, businessKey={}",
                        attempt, safeTarget, message.getEventType(), message.getBusinessKey());
                return;
            } catch (Exception e) {
                log.warn("[FlowWebhook] 回调失败(attempt={}/{}): target={}, eventType={}, error={}",
                        attempt, MAX_RETRY, safeTarget, message.getEventType(), e.getMessage());
                if (attempt < MAX_RETRY) {
                    try {
                        if (retryDelayMillis > 0) {
                            Thread.sleep(retryDelayMillis * attempt);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    log.error("[FlowWebhook] 回调最终失败，已放弃: target={}, eventType={}, businessKey={}",
                            safeTarget, message.getEventType(), message.getBusinessKey(), e);
                }
            }
        }
    }

    /**
     * 执行实际的 HTTP 请求
     */
    private void doSend(String webhookUrl, FlowEventMessage message) throws JsonProcessingException {
        byte[] body = objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Flow-Event-Type", message.getEventType());
        if (message.getProcessDefKey() != null) {
            headers.put("X-Flow-Process-Key", message.getProcessDefKey());
        }
        if (message.getBusinessKey() != null) {
            headers.put("X-Flow-Business-Key", message.getBusinessKey());
        }

        OutboundResponse response = outboundClient.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(webhookUrl)
                .method("POST")
                .headers(headers)
                .contentType("application/json")
                .body(body)
                .build());
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            throw new IllegalStateException("Webhook 返回非 2xx 状态码: " + response.getStatusCode());
        }
    }

    private String safeTarget(String webhookUrl) {
        try {
            URI uri = URI.create(webhookUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "<invalid>";
            }
            int port = uri.getPort();
            return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (port == -1 ? "" : ":" + port);
        } catch (IllegalArgumentException exception) {
            return "<invalid>";
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
