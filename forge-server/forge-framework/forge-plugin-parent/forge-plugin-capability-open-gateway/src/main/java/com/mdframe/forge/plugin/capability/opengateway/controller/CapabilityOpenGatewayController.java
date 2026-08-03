package com.mdframe.forge.plugin.capability.opengateway.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.identity.security.AuthenticatedCapabilityIdentity;
import com.mdframe.forge.plugin.capability.opengateway.auth.OpenGatewayAuthenticator;
import com.mdframe.forge.plugin.capability.opengateway.dto.OpenGatewayResponse;
import com.mdframe.forge.plugin.capability.opengateway.exception.OpenGatewayException;
import com.mdframe.forge.plugin.capability.opengateway.service.CapabilityInvokeOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 统一能力开放网关入口。Sa-Token 白名单放行后，认证/授权全部由
 * OpenGatewayAuthenticator 与 CapabilityInvokeOrchestrator 完成；
 * 响应走独立对外契约 OpenGatewayResponse，不使用 RespInfo。
 * 网关开关关闭时本控制器不注册，路径自然返回 404（失败关闭）。
 */
@Slf4j
@RestController
@RequestMapping("/openapi/v1/capabilities")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.capability.open-gateway", name = "enabled", havingValue = "true")
public class CapabilityOpenGatewayController {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final OpenGatewayAuthenticator authenticator;
    private final CapabilityInvokeOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    /**
     * 统一能力调用入口。请求体以原始字节接收：签名模式验签需要 body 摘要，
     * 认证通过后再反序列化为 payload。
     */
    @PostMapping("/{capabilityCode}/invoke")
    public ResponseEntity<OpenGatewayResponse> invoke(
            HttpServletRequest request,
            @PathVariable String capabilityCode,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) byte[] body) {
        String requestId = UUID.randomUUID().toString();
        byte[] rawBody = body == null ? new byte[0] : body;
        long startedAt = System.nanoTime();
        String authMode = authenticationMode(request);
        AtomicReference<String> stage = new AtomicReference<>("AUTHENTICATION");
        log.info("[能力开放网关入口] 收到请求: requestId={}, capabilityCode={}, authMode={}, bodyBytes={}, idempotencyKeyPresent={}",
                requestId, capabilityCode, authMode, rawBody.length, idempotencyKey != null && !idempotencyKey.isBlank());
        try {
            AuthenticatedCapabilityIdentity identity = authenticator.authenticate(request, rawBody);
            log.info("[能力开放网关认证] 认证成功: requestId={}, capabilityCode={}, authMode={}, clientId={}, clientCode={}, actorType={}, actorUserId={}, tenantId={}, activeOrgId={}",
                    requestId, capabilityCode, authMode,
                    identity.principal().clientId(), identity.principal().clientCode(),
                    identity.principal().actorType(), identity.principal().actorUserId(),
                    identity.principal().tenantId(), identity.principal().activeOrgId());
            stage.set("PAYLOAD_PARSING");
            Map<String, Object> payload = parsePayload(rawBody);
            stage.set("ORCHESTRATION");
            OpenGatewayResponse response = orchestrator.invoke(
                    identity, capabilityCode, idempotencyKey, payload, requestId);
            log.info("[能力开放网关入口] 请求完成: requestId={}, capabilityCode={}, clientId={}, resultCode={}, httpStatus={}, durationMs={}",
                    requestId, capabilityCode, identity.principal().clientId(), response.code(),
                    response.status(), elapsed(startedAt));
            return respond(response);
        }
        catch (OpenGatewayException exception) {
            if (exception.getHttpStatus() >= 500) {
                log.warn("[能力开放网关入口] 请求失败: requestId={}, capabilityCode={}, authMode={}, failureStage={}, resultCode={}, httpStatus={}, durationMs={}, exceptionType={}",
                        requestId, capabilityCode, authMode, stage.get(), exception.getErrorCode(), exception.getHttpStatus(),
                        elapsed(startedAt), exception.getClass().getSimpleName(), exception);
            }
            else {
                log.warn("[能力开放网关入口] 请求拒绝: requestId={}, capabilityCode={}, authMode={}, failureStage={}, resultCode={}, httpStatus={}, durationMs={}, exceptionType={}",
                        requestId, capabilityCode, authMode, stage.get(), exception.getErrorCode(), exception.getHttpStatus(),
                        elapsed(startedAt), exception.getClass().getSimpleName());
            }
            return respond(OpenGatewayResponse.error(exception.getErrorCode(),
                    exception.getMessage(), requestId, exception.getHttpStatus()));
        }
        catch (RuntimeException exception) {
            // 兜底：网关契约不允许异常外溢到全局处理器（RespInfo 格式）
            log.warn("[能力开放网关入口] 未预期异常: requestId={}, capabilityCode={}, authMode={}, failureStage={}, resultCode=INTERNAL_ERROR, httpStatus=500, durationMs={}, exceptionType={}",
                    requestId, capabilityCode, authMode, stage.get(), elapsed(startedAt),
                    exception.getClass().getSimpleName(), exception);
            return respond(OpenGatewayResponse.error(
                    "INTERNAL_ERROR", "能力执行失败，请稍后重试", requestId, 500));
        }
    }

    private Map<String, Object> parsePayload(byte[] rawBody) {
        if (rawBody.length == 0) {
            return Map.of();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(rawBody, PAYLOAD_TYPE);
            return payload == null ? Map.of() : payload;
        }
        catch (IOException exception) {
            throw new OpenGatewayException(
                    "SCHEMA_INVALID", 400, "请求体必须是合法的 JSON 对象", exception);
        }
    }

    private ResponseEntity<OpenGatewayResponse> respond(OpenGatewayResponse response) {
        return ResponseEntity.status(response.status()).body(response);
    }

    private String authenticationMode(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return "OAUTH";
        }
        if (request.getHeader(OpenGatewayAuthenticator.HEADER_APP_ID) != null) {
            return "SIGNATURE";
        }
        return "UNKNOWN";
    }

    private long elapsed(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
