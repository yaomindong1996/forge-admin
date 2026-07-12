package com.mdframe.forge.plugin.capability.controlplane.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityInvocationLog;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityInvocationLogMapper;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CapabilityInvocationAuditService {

    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final Pattern CLIENT_CODE = Pattern.compile("^[a-z][a-z0-9_]{2,63}$");
    private static final Pattern CAPABILITY_CODE = Pattern.compile(
            "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");
    private static final Pattern VERSION = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");
    private static final Pattern STABLE_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");
    private static final Pattern SCHEMA_PATH = Pattern.compile("^[A-Za-z0-9_.$\\[\\]-]{1,256}$");
    private static final Pattern TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final AiCapabilityInvocationLogMapper logMapper;

    public void record(Long tenantId, CapabilityInvocationAuditEvent event) {
        AiCapabilityInvocationLog log = buildLog(tenantId, event);
        logMapper.insertIdempotent(log);
    }

    public void recordOrUpdate(Long tenantId, CapabilityInvocationAuditEvent event) {
        AiCapabilityInvocationLog log = buildLog(tenantId, event);
        if (logMapper.updateResultByRequestIdentity(log) > 0) {
            return;
        }
        if (logMapper.insertIdempotent(log) > 0) {
            return;
        }
        AiCapabilityInvocationLog existing = logMapper.selectByRequestId(tenantId, event.requestId());
        if (!sameIdentity(existing, log)) {
            throw new BusinessException("能力调用审计请求身份冲突");
        }
        logMapper.updateResultByRequestIdentity(log);
    }

    private boolean sameIdentity(
            AiCapabilityInvocationLog existing,
            AiCapabilityInvocationLog incoming) {
        return existing != null
                && Objects.equals(existing.getTenantId(), incoming.getTenantId())
                && Objects.equals(existing.getClientId(), incoming.getClientId())
                && Objects.equals(existing.getClientCode(), incoming.getClientCode())
                && Objects.equals(existing.getCapabilityId(), incoming.getCapabilityId())
                && Objects.equals(existing.getCapabilityCode(), incoming.getCapabilityCode())
                && Objects.equals(existing.getActorType(), incoming.getActorType())
                && Objects.equals(existing.getActorUserId(), incoming.getActorUserId())
                && Objects.equals(existing.getServiceUserId(), incoming.getServiceUserId())
                && Objects.equals(existing.getActiveOrgId(), incoming.getActiveOrgId());
    }

    private AiCapabilityInvocationLog buildLog(Long tenantId, CapabilityInvocationAuditEvent event) {
        if (tenantId == null || tenantId <= 0 || event == null) {
            throw new BusinessException("能力调用审计缺少必要上下文");
        }
        validateEvent(event);
        AiCapabilityInvocationLog log = new AiCapabilityInvocationLog();
        log.setId(IdWorker.getId(log));
        log.setTenantId(tenantId);
        log.setRequestId(event.requestId());
        log.setClientId(event.clientId());
        log.setClientCode(event.clientCode());
        log.setCapabilityId(event.capabilityId());
        log.setCapabilityCode(event.capabilityCode());
        log.setCapabilityVersion(event.capabilityVersion());
        log.setActorType(event.actorType().name());
        log.setActorUserId(event.actorUserId());
        log.setServiceUserId(event.serviceUserId());
        log.setActiveOrgId(event.activeOrgId());
        log.setResultStatus(event.resultStatus().name());
        log.setResultCode(event.resultCode());
        log.setErrorCode(event.errorCode());
        log.setSchemaPath(event.schemaPath());
        log.setTraceId(event.traceId());
        log.setDurationMs(Math.max(0L, event.durationMs()));
        log.setDelFlag(0);
        return log;
    }

    private void validateEvent(CapabilityInvocationAuditEvent event) {
        requirePattern("requestId", event.requestId(), REQUEST_ID, false);
        requirePositive("clientId", event.clientId());
        requirePattern("clientCode", event.clientCode(), CLIENT_CODE, false);
        requirePattern("capabilityCode", event.capabilityCode(), CAPABILITY_CODE, false);
        requirePattern("capabilityVersion", event.capabilityVersion(), VERSION, true);
        if (event.actorType() == null || event.resultStatus() == null) {
            throw new BusinessException("能力调用审计缺少主体或结果状态");
        }
        requirePositive("actorUserId", event.actorUserId());
        requirePositive("serviceUserId", event.serviceUserId());
        requirePositive("activeOrgId", event.activeOrgId());
        if (event.actorType() == CapabilityActorType.SERVICE
                && !Objects.equals(event.actorUserId(), event.serviceUserId())) {
            throw new BusinessException("服务账号调用的实际主体必须与绑定服务账号一致");
        }
        requireStableCode("结果码", event.resultCode(), false);
        requireStableCode("错误码", event.errorCode(), true);
        requirePattern("schemaPath", event.schemaPath(), SCHEMA_PATH, true);
        requirePattern("traceId", event.traceId(), TRACE_ID, true);
    }

    private void requireStableCode(String fieldName, String value, boolean nullable) {
        requirePattern(fieldName, value, STABLE_CODE, nullable);
    }

    private void requirePattern(
            String fieldName,
            String value,
            Pattern pattern,
            boolean nullable) {
        if (value == null || value.isBlank()) {
            if (nullable) {
                return;
            }
            throw new BusinessException("能力调用审计缺少" + fieldName);
        }
        if (!pattern.matcher(value).matches()) {
            throw new BusinessException("能力调用审计" + fieldName + "格式无效");
        }
    }

    private void requirePositive(String fieldName, Long value) {
        if (value == null || value <= 0) {
            throw new BusinessException("能力调用审计" + fieldName + "无效");
        }
    }

    public Page<AiCapabilityInvocationLog> page(
            Long tenantId,
            PageQuery pageQuery,
            Long clientId,
            String capabilityCode,
            String resultCode) {
        return logMapper.selectPage(
                pageQuery.toPage(), requireTenant(tenantId), clientId, capabilityCode, resultCode);
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }
}
