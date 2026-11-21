package com.mdframe.forge.plugin.collaboration.service;

import com.mdframe.forge.plugin.collaboration.domain.CollaborationCallbackProcessStatus;
import com.mdframe.forge.plugin.collaboration.domain.callback.CallbackAcceptResult;
import com.mdframe.forge.plugin.collaboration.domain.callback.VerifiedCallback;
import com.mdframe.forge.plugin.collaboration.domain.entity.SocialCallbackEvent;
import com.mdframe.forge.plugin.collaboration.mapper.SocialCallbackEventMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.domain.entity.SysSocialAppConfig;
import com.mdframe.forge.starter.social.security.SecretContext;
import com.mdframe.forge.starter.social.security.SocialAppCredentialService;
import com.mdframe.forge.starter.social.service.ISocialAppConfigService;
import com.mdframe.forge.starter.social.service.ISocialConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 企业协同回调事件收件箱服务（Task 7）。
 * <p>
 * 只受理已验签解密的事件：按去重哈希幂等入库，正文以 FPC1 密文存储；
 * claim/标记接口供后续增量同步 Worker（Task 11）以 CAS 方式消费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborationCallbackInboxService {

    /**
     * 事件处理最大重试次数，超过后置为 DISCARDED
     */
    public static final int MAX_RETRY = 5;

    private static final String PAYLOAD_CREDENTIAL_TYPE = "CALLBACK_PAYLOAD";
    private static final String SIGNATURE_VERIFIED = "VERIFIED";
    private final ISocialConfigService socialConfigService;
    private final ISocialAppConfigService socialAppConfigService;
    private final SocialAppCredentialService credentialService;
    private final SocialCallbackEventMapper callbackEventMapper;

    /**
     * 按连接编码解析启用中的连接，不存在或停用时失败关闭
     */
    public SysSocialConfig requireConnection(String connectionCode) {
        SysSocialConfig connection = socialConfigService.selectConnectionByCode(connectionCode);
        if (connection == null || !EnableStatus.ENABLED.matches(connection.getStatus())) {
            throw new BusinessException("协同连接不存在或未启用");
        }
        return connection;
    }

    /**
     * 按应用编码解析连接下启用中的应用，不存在或停用时失败关闭
     */
    public SysSocialAppConfig requireApp(SysSocialConfig connection, String appCode) {
        return socialAppConfigService.listApps(connection.getTenantId(), connection.getId()).stream()
                .filter(app -> appCode != null && appCode.equals(app.getAppCode()))
                .filter(app -> EnableStatus.ENABLED.matches(app.getStatus()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("协同应用不存在或未启用"));
    }

    /**
     * 受理已验签事件（按连接/应用编码解析后入库）
     */
    public CallbackAcceptResult accept(String connectionCode, String appCode, VerifiedCallback callback) {
        SysSocialConfig connection = requireConnection(connectionCode);
        SysSocialAppConfig app = requireApp(connection, appCode);
        return accept(connection, app, callback);
    }

    /**
     * 受理已验签事件：去重哈希幂等入库，重复事件直接吞并返回 duplicate
     */
    public CallbackAcceptResult accept(SysSocialConfig connection, SysSocialAppConfig app, VerifiedCallback callback) {
        SocialCallbackEvent event = new SocialCallbackEvent();
        event.setTenantId(connection.getTenantId());
        event.setConnectionId(connection.getId());
        event.setAppConfigId(app.getId());
        event.setEventId(callback.eventId());
        event.setDedupHash(dedupHash(callback));
        event.setEventType(callback.eventType());
        event.setEventTime(callback.eventTime());
        event.setSignatureStatus(SIGNATURE_VERIFIED);
        event.setPayloadCipher(encryptPayload(connection, app, callback.plaintext()));
        event.setProcessStatus(CollaborationCallbackProcessStatus.PENDING.getCode());
        event.setRetryCount(0);
        try {
            callbackEventMapper.insert(event);
            return CallbackAcceptResult.accepted(event.getId());
        } catch (DuplicateKeyException e) {
            log.info("协同回调事件重复，幂等忽略：connectionId={}, eventType={}",
                    connection.getId(), callback.eventType());
            return CallbackAcceptResult.duplicated();
        }
    }

    /**
     * 批量领取待处理事件（PENDING 或到期重试的 FAILED），返回领取数量。
     * 两段式：先查候选 id，再 CAS 更新，避免 {@code UPDATE ... ORDER BY LIMIT} 方言。
     */
    public int claimPendingEvents(Long tenantId, int batchSize, String workerId) {
        if (batchSize <= 0) {
            return 0;
        }
        List<Long> candidateIds = callbackEventMapper.selectClaimCandidateIds(tenantId, batchSize);
        if (candidateIds == null || candidateIds.isEmpty()) {
            return 0;
        }
        return callbackEventMapper.claimEventsByIds(tenantId, workerId, candidateIds);
    }

    /**
     * 查询指定 Worker 已领取的事件
     */
    public List<SocialCallbackEvent> listClaimedEvents(Long tenantId, String workerId) {
        return callbackEventMapper.selectClaimedEvents(tenantId, workerId);
    }

    /**
     * CAS 标记事件处理成功
     */
    public boolean markProcessed(Long id, Long tenantId, String workerId) {
        return callbackEventMapper.markProcessed(id, tenantId, workerId) > 0;
    }

    /**
     * CAS 标记事件处理失败，指数退避重试，超过上限置为 DISCARDED。
     * 退避分钟数在 Java 计算：min(2^(retry+1), 60)，避免 Mapper 使用 DATE_ADD/POW。
     */
    public boolean markFailed(SocialCallbackEvent event, String workerId, String errorCode, String errorSummary) {
        int currentRetry = event.getRetryCount() == null ? 0 : event.getRetryCount();
        int nextRetry = currentRetry + 1;
        boolean discard = nextRetry >= MAX_RETRY;
        LocalDateTime nextRetryTime = null;
        if (!discard) {
            long minutes = Math.min(1L << Math.min(nextRetry, 6), 60L);
            nextRetryTime = LocalDateTime.now().plusMinutes(minutes);
        }
        String processStatus = discard
                ? CollaborationCallbackProcessStatus.DISCARDED.getCode()
                : CollaborationCallbackProcessStatus.FAILED.getCode();
        return callbackEventMapper.markFailed(
                event.getId(), event.getTenantId(), workerId,
                processStatus, nextRetryTime, errorCode, errorSummary) > 0;
    }

    /**
     * 解密事件正文密文，供消费 Worker 解析事件明文
     */
    public String decryptPayload(SocialCallbackEvent event) {
        char[] plaintext = credentialService.decrypt(event.getPayloadCipher(),
                SecretContext.of(event.getTenantId(), event.getConnectionId(),
                        event.getAppConfigId(), PAYLOAD_CREDENTIAL_TYPE));
        try {
            return new String(plaintext);
        } finally {
            Arrays.fill(plaintext, '\0');
        }
    }

    private String encryptPayload(SysSocialConfig connection, SysSocialAppConfig app, String plaintext) {
        char[] chars = plaintext.toCharArray();
        try {
            return credentialService.encrypt(chars, SecretContext.of(
                    connection.getTenantId(), connection.getId(), app.getId(), PAYLOAD_CREDENTIAL_TYPE));
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    private String dedupHash(VerifiedCallback callback) {
        String payloadDigest = sha256Hex(callback.plaintext());
        return sha256Hex(callback.msgSignature() + "|" + callback.timestamp()
                + "|" + callback.nonce() + "|" + payloadDigest);
    }

    private String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("去重哈希计算失败");
        }
    }
}
