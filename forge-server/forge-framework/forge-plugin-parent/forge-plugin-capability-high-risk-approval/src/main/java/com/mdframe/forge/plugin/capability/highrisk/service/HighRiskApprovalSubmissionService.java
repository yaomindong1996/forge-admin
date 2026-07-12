package com.mdframe.forge.plugin.capability.highrisk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.highrisk.crypto.CapabilityPayloadCrypto;
import com.mdframe.forge.plugin.capability.highrisk.crypto.EncryptedCapabilityPayload;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.capability.highrisk.support.HighRiskApprovalFlowDefinition;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RequiredArgsConstructor
public class HighRiskApprovalSubmissionService {
    private final CapabilityApprovalMapper approvalMapper;
    private final CapabilityPolicyService policyService;
    private final AiCapabilityClientMapper clientMapper;
    private final CapabilityPayloadCrypto payloadCrypto;
    private final FlowClient flowClient;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final HighRiskBusinessStateService businessStateService;

    public Map<String, Object> submit(SecureActionDescriptor descriptor,
                                      ExecutionIdentity identity,
                                      Map<String, Object> input,
                                      String requestId) {
        Long tenantId = identity.loginUser().getTenantId();
        AiCapabilityPolicy policy = policyService.requireActive(
                tenantId, descriptor.capabilityId(), descriptor.version());
        AiCapabilityClient client = requireClient(identity, tenantId);
        String idempotencyKey = StringUtils.trimToNull(
                input.get("idempotencyKey") == null ? null : String.valueOf(input.get("idempotencyKey")));
        if (idempotencyKey == null || idempotencyKey.length() < 16 || idempotencyKey.length() > 128
                || !idempotencyKey.matches("^[A-Za-z0-9._:-]+$")) {
            throw new BusinessException("INVALID_ARGUMENT");
        }
        String digest = digest(input);
        AiCapabilityApproval existing = approvalMapper.selectByIdempotency(
                tenantId, identity.clientId(), descriptor.capabilityId(), idempotencyKey);
        if (existing != null) {
            return reuseOrRecover(existing, digest, descriptor, policy, identity, client);
        }

        AiCapabilityApproval approval = reservation(
                descriptor, identity, client, input, requestId, policy, digest);
        try {
            requiresNew().executeWithoutResult(status -> approvalMapper.insert(approval));
        }
        catch (DuplicateKeyException exception) {
            AiCapabilityApproval raced = approvalMapper.selectByIdempotency(
                    tenantId, identity.clientId(), descriptor.capabilityId(), idempotencyKey);
            if (raced == null) {
                throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
            }
            return reuseOrRecover(raced, digest, descriptor, policy, identity, client);
        }
        startApprovalFlow(approval, descriptor, policy);
        return result(approval, false);
    }

    private Map<String, Object> reuseOrRecover(AiCapabilityApproval approval,
                                                String digest,
                                                SecureActionDescriptor descriptor,
                                                AiCapabilityPolicy policy,
                                                ExecutionIdentity identity,
                                                AiCapabilityClient client) {
        if (!digest.equals(approval.getRequestDigest())
                || !descriptor.version().equals(approval.getCapabilityVersion())
                || !identity.actorUserId().equals(approval.getActorUserId())
                || !identity.serviceUserId().equals(approval.getServiceUserId())
                || !identity.loginUser().getActiveOrgId().equals(approval.getActiveOrgId())
                || !client.getCredentialVersion().equals(approval.getCredentialVersion())) {
            throw new BusinessException(409, "IDEMPOTENCY_CONFLICT");
        }
        if ("RESERVED".equals(approval.getExecuteStatus())) {
            startApprovalFlow(approval, descriptor, policy);
        }
        return result(approval, true);
    }

    private void startApprovalFlow(AiCapabilityApproval approval,
                                   SecureActionDescriptor descriptor,
                                   AiCapabilityPolicy policy) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("approvalId", String.valueOf(approval.getId()));
        variables.put("recordId", String.valueOf(approval.getId()));
        variables.put("businessKey", "capability-approval:" + approval.getId());
        variables.put("objectCode", HighRiskApprovalFlowDefinition.OBJECT_CODE);
        variables.put("approvalCandidateGroup", policy.getApprovalCandidateGroup());
        FlowResult<String> started = flowClient.startHighRiskApprovalForDelegatedUser(
                policy.getApprovalFlowModelKey(), "capability-approval:" + approval.getId(),
                "高风险动作审批-" + descriptor.capabilityName(), variables);
        if (started == null || !started.isSuccess() || StringUtils.isBlank(started.getData())) {
            throw new BusinessException("APPROVAL_FLOW_UNAVAILABLE");
        }
        approval.setProcessInstanceId(started.getData());
        approval.setExecuteStatus("PENDING_APPROVAL");
        approval.setResultCode("PENDING_APPROVAL");
        requiresNew().executeWithoutResult(status -> updateRequired(approval));
    }

    private AiCapabilityApproval reservation(SecureActionDescriptor descriptor,
                                             ExecutionIdentity identity,
                                             AiCapabilityClient client,
                                             Map<String, Object> input,
                                             String requestId,
                                             AiCapabilityPolicy policy,
                                             String digest) {
        AiCapabilityApproval approval = new AiCapabilityApproval();
        approval.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        approval.setTenantId(identity.loginUser().getTenantId());
        approval.setRequestId(requestId);
        approval.setClientId(identity.clientId());
        approval.setCredentialVersion(client.getCredentialVersion());
        approval.setCapabilityId(descriptor.capabilityId());
        approval.setCapabilityCode(descriptor.capabilityCode());
        approval.setCapabilityVersion(descriptor.version());
        approval.setActorUserId(identity.actorUserId());
        approval.setServiceUserId(identity.serviceUserId());
        approval.setActiveOrgId(identity.loginUser().getActiveOrgId());
        approval.setIdempotencyKey(String.valueOf(input.get("idempotencyKey")));
        approval.setRequestDigest(digest);
        approval.setBusinessStateDigest(businessStateService.snapshot(descriptor, input));
        approval.setFlowModelKey(policy.getApprovalFlowModelKey());
        approval.setExecuteStatus("RESERVED");
        approval.setResultCode("RESERVED");
        approval.setExpiresAt(LocalDateTime.now().plusSeconds(policy.getExpireSeconds()));
        approval.setDelFlag(0);
        byte[] aad = aad(approval);
        try {
            EncryptedCapabilityPayload encrypted = payloadCrypto.encrypt(
                    objectMapper.writeValueAsBytes(canonicalize(input)), aad);
            approval.setKeyId(encrypted.keyId());
            approval.setWrappedDek(encrypted.wrappedDek());
            approval.setPayloadIv(encrypted.iv());
            approval.setPayloadCiphertext(encrypted.ciphertext());
            approval.setPayloadAuthTag(encrypted.authTag());
        }
        catch (BusinessException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new BusinessException("APPROVAL_PAYLOAD_INVALID");
        }
        return approval;
    }

    private AiCapabilityClient requireClient(ExecutionIdentity identity, Long tenantId) {
        AiCapabilityClient client = clientMapper.selectTenantById(tenantId, identity.clientId());
        if (client == null || !"ENABLED".equals(client.getStatus())
                || !identity.serviceUserId().equals(client.getServiceUserId())
                || !identity.loginUser().getActiveOrgId().equals(client.getActiveOrgId())
                || (client.getExpiresAt() != null && !client.getExpiresAt().isAfter(LocalDateTime.now()))) {
            throw new BusinessException(403, "AUTHORIZATION_REVOKED");
        }
        return client;
    }

    private Map<String, Object> result(AiCapabilityApproval approval, boolean idempotentHit) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executeStatus", approval.getExecuteStatus());
        result.put("message", resultMessage(approval.getExecuteStatus()));
        result.put("correlationId", approval.getRequestId());
        result.put("idempotentHit", idempotentHit);
        result.put("approvalRequestId", String.valueOf(approval.getId()));
        return Map.copyOf(result);
    }

    private String resultMessage(String status) {
        return switch (StringUtils.defaultString(status)) {
            case "RESERVED" -> "高风险审批正在创建，可使用相同幂等键重试";
            case "PENDING_APPROVAL" -> "高风险动作已提交人工审批";
            case "SUCCESS" -> "审批通过且业务动作已执行";
            case "REJECTED" -> "审批已驳回，业务动作未执行";
            case "CANCELLED" -> "审批已取消，业务动作未执行";
            case "EXPIRED" -> "审批已过期，业务动作未执行";
            case "FAILED" -> "审批后置校验或业务执行失败";
            default -> "高风险审批状态不可用";
        };
    }

    byte[] aad(AiCapabilityApproval approval) {
        return String.join("|", String.valueOf(approval.getTenantId()), String.valueOf(approval.getId()),
                String.valueOf(approval.getClientId()), String.valueOf(approval.getCapabilityId()),
                approval.getCapabilityVersion(), approval.getRequestDigest()).getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> decryptAndVerify(AiCapabilityApproval approval) {
        EncryptedCapabilityPayload payload = new EncryptedCapabilityPayload(
                approval.getKeyId(), approval.getWrappedDek(), approval.getPayloadIv(),
                approval.getPayloadCiphertext(), approval.getPayloadAuthTag());
        try {
            Map<String, Object> input = objectMapper.readValue(
                    payloadCrypto.decrypt(payload, aad(approval)), new TypeReference<>() { });
            if (!approval.getRequestDigest().equals(digest(input))) {
                throw new BusinessException("APPROVAL_PAYLOAD_INVALID");
            }
            return input;
        }
        catch (BusinessException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new BusinessException("APPROVAL_PAYLOAD_INVALID");
        }
    }

    private String digest(Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(canonicalize(value));
            return "sha256:" + java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
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
            List<Object> list = new ArrayList<>();
            iterable.forEach(item -> list.add(canonicalize(item)));
            return list;
        }
        return value;
    }

    private void updateRequired(AiCapabilityApproval approval) {
        if (approvalMapper.updateState(approval) != 1) {
            throw new BusinessException("APPROVAL_AUDIT_UNAVAILABLE");
        }
    }

    private TransactionTemplate requiresNew() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
