package com.mdframe.forge.plugin.capability.flowaction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.flowaction.domain.AiCapabilityFlowActionLog;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionExecutionLogMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class FlowActionExecutionLogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final long RECOVERY_STALE_SECONDS = 30L;

    private final FlowActionExecutionLogMapper logMapper;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public Map<String, Object> execute(
            SecureActionDescriptor descriptor,
            ExecutionIdentity identity,
            Map<String, Object> input,
            String requestId,
            Supplier<Map<String, Object>> action) {
        return executeWithAttempt(
                descriptor, identity, input, requestId, ignored -> action.get());
    }

    public Map<String, Object> executeWithAttempt(
            SecureActionDescriptor descriptor,
            ExecutionIdentity identity,
            Map<String, Object> input,
            String requestId,
            Function<ExecutionAttempt, Map<String, Object>> action) {
        long startedAt = System.nanoTime();
        String operation = descriptor.actionCode();
        String idempotencyKey = String.valueOf(input.get("idempotencyKey"));
        String digest = requestDigest(descriptor, input);
        AiCapabilityFlowActionLog existing = findExisting(identity, descriptor, operation, idempotencyKey);
        AiCapabilityFlowActionLog reservation;
        if (existing != null) {
            Map<String, Object> completed = reuseCompleted(existing, digest);
            if (completed != null) {
                return completed;
            }
            requireRecoverable(existing);
            reservation = existing;
            markRecovering(reservation, startedAt);
        }
        else {
            reservation = reservation(
                    descriptor, identity, input, requestId, operation, idempotencyKey, digest);
            try {
                AiCapabilityFlowActionLog newReservation = reservation;
                requiresNewTransaction().executeWithoutResult(status -> logMapper.insert(newReservation));
            }
            catch (DuplicateKeyException exception) {
                existing = findExisting(identity, descriptor, operation, idempotencyKey);
                if (existing == null) {
                    throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
                }
                Map<String, Object> completed = reuseCompleted(existing, digest);
                if (completed != null) {
                    return completed;
                }
                requireRecoverable(existing);
                reservation = existing;
                markRecovering(reservation, startedAt);
            }
            catch (RuntimeException exception) {
                throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", exception);
            }
        }

        AiCapabilityFlowActionLog activeReservation = reservation;
        Map<String, Object> result;
        try {
            TransactionTemplate actionTransaction = new TransactionTemplate(transactionManager);
            if ("SUBMIT".equals(operation)) {
                /*
                 * SUBMIT 会在 REQUIRES_NEW 事务中创建业务记录并提交 recordId 检查点。
                 * MySQL REPEATABLE_READ 下，外层事务若先读取过能力元数据，会继续使用旧快照，
                 * 随后的流程启动将看不到刚提交的记录并误报 RESOURCE_NOT_FOUND。
                 * READ_COMMITTED 保证外层后续读取能看到检查点事务已经提交的业务记录。
                 */
                actionTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            }
            result = actionTransaction.execute(status -> {
                Map<String, Object> actionResult = Map.copyOf(
                        action.apply(new ExecutionAttempt(activeReservation)));
                activeReservation.setExecuteStatus("SUCCESS");
                activeReservation.setResultCode("SUCCESS");
                activeReservation.setResultSnapshot(writeJson(actionResult));
                activeReservation.setErrorCode(null);
                activeReservation.setDurationMs(elapsed(startedAt));
                updateRequired(activeReservation);
                return actionResult;
            });
        }
        catch (SecureActionUnavailableException | BusinessException exception) {
            markFailed(activeReservation, stableErrorCode(exception), startedAt);
            throw exception;
        }
        catch (RuntimeException exception) {
            markFailed(activeReservation, "FLOW_EXECUTION_FAILED", startedAt);
            throw new BusinessException(503, "流程动作执行失败，请稍后重试", exception);
        }
        if (result == null) {
            markFailed(activeReservation, "FLOW_EXECUTION_FAILED", startedAt);
            throw new BusinessException(503, "流程动作执行失败，请稍后重试");
        }
        return result;
    }

    /**
     * 判断同请求是否已成功或可恢复，用于在任务可办理预检前识别幂等重放。
     * SUCCESS 由日志快照直接复用；FAILED 或超时 RUNNING 进入受控恢复；
     * 活动 RUNNING 和异摘要请求必须在调用任务查询前就失败关闭。
     */
    boolean isRecoverableRequest(SecureActionDescriptor descriptor,
                                 ExecutionIdentity identity,
                                 Map<String, Object> input) {
        String idempotencyKey = String.valueOf(input.get("idempotencyKey"));
        AiCapabilityFlowActionLog existing = findExisting(
                identity, descriptor, descriptor.actionCode(), idempotencyKey);
        if (existing == null) {
            return false;
        }
        if (!requestDigest(descriptor, input).equals(existing.getRequestDigest())) {
            throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
        }
        if ("SUCCESS".equals(existing.getExecuteStatus())) {
            return true;
        }
        requireRecoverable(existing);
        return true;
    }

    private AiCapabilityFlowActionLog findExisting(
            ExecutionIdentity identity,
            SecureActionDescriptor descriptor,
            String operation,
            String idempotencyKey) {
        try {
            return logMapper.selectByIdempotency(
                    identity.loginUser().getTenantId(), identity.clientId(), descriptor.capabilityId(),
                    operation, idempotencyKey);
        }
        catch (RuntimeException exception) {
            throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", exception);
        }
    }

    private Map<String, Object> reuseCompleted(AiCapabilityFlowActionLog existing, String digest) {
        if (!digest.equals(existing.getRequestDigest())) {
            throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
        }
        if ("SUCCESS".equals(existing.getExecuteStatus())) {
            try {
                Map<String, Object> result = objectMapper.readValue(existing.getResultSnapshot(), MAP_TYPE);
                result = new LinkedHashMap<>(result);
                result.put("idempotentHit", true);
                return Map.copyOf(result);
            }
            catch (Exception exception) {
                throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", exception);
            }
        }
        return null;
    }

    private void markRecovering(AiCapabilityFlowActionLog log, long startedAt) {
        log.setExecuteStatus("RUNNING");
        log.setResultCode("RECOVERY_PENDING");
        log.setResultSnapshot(null);
        log.setErrorCode(null);
        log.setDurationMs(elapsed(startedAt));
        try {
            requiresNewTransaction().executeWithoutResult(status -> updateRequired(log));
        }
        catch (RuntimeException exception) {
            throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", exception);
        }
    }

    private void requireRecoverable(AiCapabilityFlowActionLog existing) {
        if ("FAILED".equals(existing.getExecuteStatus())) {
            return;
        }
        LocalDateTime lastTouched = existing.getUpdateTime() != null
                ? existing.getUpdateTime() : existing.getCreateTime();
        if ("RUNNING".equals(existing.getExecuteStatus())
                && lastTouched != null
                && lastTouched.isBefore(LocalDateTime.now().minusSeconds(RECOVERY_STALE_SECONDS))) {
            return;
        }
        throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
    }

    private AiCapabilityFlowActionLog reservation(
            SecureActionDescriptor descriptor,
            ExecutionIdentity identity,
            Map<String, Object> input,
            String requestId,
            String operation,
            String idempotencyKey,
            String digest) {
        AiCapabilityFlowActionLog log = new AiCapabilityFlowActionLog();
        log.setTenantId(identity.loginUser().getTenantId());
        log.setRequestId(requestId);
        log.setClientId(identity.clientId());
        log.setCapabilityId(descriptor.capabilityId());
        log.setCapabilityCode(descriptor.capabilityCode());
        log.setCapabilityVersion(descriptor.version());
        log.setOperation(operation);
        log.setObjectCode(descriptor.objectCode());
        log.setRecordId(initialRecordId(input));
        log.setTaskRef(taskRef(input));
        log.setIdempotencyKey(idempotencyKey);
        log.setRequestDigest(digest);
        log.setActorType(identity.actorType());
        log.setActorUserId(identity.actorUserId());
        log.setServiceUserId(identity.serviceUserId());
        log.setActiveOrgId(identity.loginUser().getActiveOrgId());
        log.setExecuteStatus("RUNNING");
        log.setResultCode("EXECUTION_PENDING");
        log.setDurationMs(0L);
        log.setDelFlag(0L);
        return log;
    }

    private void updateRequired(AiCapabilityFlowActionLog log) {
        try {
            if (logMapper.updateResultByIdentity(log) != 1) {
                throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", null);
            }
        }
        catch (SecureActionUnavailableException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", exception);
        }
    }

    private void markFailed(AiCapabilityFlowActionLog log, String errorCode, long startedAt) {
        log.setExecuteStatus("FAILED");
        log.setResultCode("FAILED");
        log.setResultSnapshot(null);
        log.setErrorCode(errorCode);
        log.setDurationMs(elapsed(startedAt));
        try {
            requiresNewTransaction().executeWithoutResult(status -> logMapper.updateResultByIdentity(log));
        }
        catch (RuntimeException ignored) {
            // 主异常优先；Capability 审计仍会记录安全错误码。
        }
    }

    String requestDigest(SecureActionDescriptor descriptor, Map<String, Object> input) {
        Map<String, Object> digest = new LinkedHashMap<>();
        digest.put("capabilityCode", descriptor.capabilityCode());
        digest.put("version", descriptor.version());
        digest.put("operation", descriptor.actionCode());
        digest.put("recordId", input.get("recordId"));
        digest.put("arguments", input.get("arguments"));
        digest.put("data", input.get("data"));
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(canonicalize(digest));
            return "sha256:" + java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical));
        }
        catch (Exception exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonicalize(item)));
            return sorted;
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.ArrayList<Object> values = new java.util.ArrayList<>();
            iterable.forEach(item -> values.add(canonicalize(item)));
            return List.copyOf(values);
        }
        return value;
    }

    private String taskRef(Map<String, Object> input) {
        Object arguments = input.get("arguments");
        if (!(arguments instanceof Map<?, ?> map) || map.get("taskId") == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(map.get("taskId")).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest, 0, 8);
        }
        catch (Exception exception) {
            return null;
        }
    }

    private String initialRecordId(Map<String, Object> input) {
        Object value = input.get("recordId");
        String recordId = value == null ? null : StringUtils.trimToNull(String.valueOf(value));
        return recordId == null ? "PENDING" : recordId;
    }

    private boolean unresolvedRecordId(String value) {
        return StringUtils.isBlank(value)
                || "PENDING".equals(value)
                || "null".equalsIgnoreCase(value);
    }

    private String stableErrorCode(RuntimeException exception) {
        String message = StringUtils.trimToNull(exception.getMessage());
        if (message != null && message.matches("^[A-Z][A-Z0-9_]{0,63}$")) {
            return message;
        }
        return "FLOW_EXECUTION_FAILED";
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception exception) {
            throw new SecureActionUnavailableException("FLOW_AUDIT_UNAVAILABLE", exception);
        }
    }

    private long elapsed(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private TransactionTemplate requiresNewTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    public final class ExecutionAttempt {

        private final AiCapabilityFlowActionLog reservation;

        private ExecutionAttempt(AiCapabilityFlowActionLog reservation) {
            this.reservation = reservation;
        }

        /**
         * 在同一个 REQUIRES_NEW 事务中创建业务记录并保存 recordId 检查点。
         * 检查点已存在时直接复用，保证流程启动失败后的重试不会重复建单。
         */
        public String resolveOrCreateRecord(Supplier<String> creator) {
            if (!unresolvedRecordId(reservation.getRecordId())) {
                return reservation.getRecordId();
            }
            try {
                String recordId = requiresNewTransaction().execute(status -> {
                    String created = StringUtils.trimToNull(creator.get());
                    if (created == null || !created.matches("^[1-9]\\d*$")) {
                        throw new BusinessException("业务申请未返回有效记录主键");
                    }
                    reservation.setRecordId(created);
                    updateRequired(reservation);
                    return created;
                });
                if (recordId == null) {
                    throw new BusinessException("业务申请未返回有效记录主键");
                }
                return recordId;
            }
            catch (SecureActionUnavailableException | BusinessException exception) {
                throw exception;
            }
            catch (RuntimeException exception) {
                throw exception;
            }
        }
    }
}
