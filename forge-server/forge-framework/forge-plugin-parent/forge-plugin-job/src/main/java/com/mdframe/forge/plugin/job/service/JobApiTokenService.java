package com.mdframe.forge.plugin.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.constant.JobApiScopes;
import com.mdframe.forge.plugin.job.constant.JobApiTokenStatus;
import com.mdframe.forge.plugin.job.dto.JobApiTokenCreateRequest;
import com.mdframe.forge.plugin.job.entity.SysJobApiToken;
import com.mdframe.forge.plugin.job.mapper.SysJobApiTokenMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.support.IssuedJobApiToken;
import com.mdframe.forge.plugin.job.support.JobApiTokenCodec;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.plugin.job.vo.JobApiTokenCreatedVO;
import com.mdframe.forge.plugin.job.vo.JobApiTokenVO;
import com.mdframe.forge.plugin.job.vo.JobApiResourceOptionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApiTokenService {

    private static final String DUMMY_HASH = "0".repeat(64);
    private static final TypeReference<Set<Long>> LONG_SET_TYPE = new TypeReference<>() { };
    private static final TypeReference<Set<String>> STRING_SET_TYPE = new TypeReference<>() { };

    private final SysJobApiTokenMapper tokenMapper;
    private final SysJobConfigMapper jobConfigMapper;
    private final JobApiTokenCodec tokenCodec;
    private final JobProperties jobProperties;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public JobApiTokenCreatedVO create(Long tenantId, JobApiTokenCreateRequest request) {
        NormalizedTokenRequest normalized = normalize(tenantId, request);
        LocalDateTime now = now();
        if (!normalized.expiresAt().isAfter(now)) {
            throw new BusinessException("Token过期时间必须晚于当前时间");
        }
        IssuedJobApiToken issued = tokenCodec.issue();
        SysJobApiToken token = buildToken(normalized, issued, now);
        if (tokenMapper.insert(token) <= 0 || token.getId() == null) {
            throw new BusinessException("创建开放API服务账号失败");
        }
        return new JobApiTokenCreatedVO(
                token.getId(), issued.token(), issued.prefix(), token.getExpiresAt());
    }

    public Page<JobApiTokenVO> page(
            Long tenantId, int pageNum, int pageSize, String callerName, String status) {
        requireTenant(tenantId);
        String normalizedStatus = normalizeStatus(status);
        Page<SysJobApiToken> source = tokenMapper.selectTokenPage(
                new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100)),
                tenantId,
                trimToNull(callerName),
                normalizedStatus,
                now());
        Page<JobApiTokenVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toView).toList());
        return result;
    }

    public List<JobApiResourceOptionVO> listResourceOptions() {
        return jobConfigMapper.selectJobApiResourceOptions();
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long tenantId, Long tokenId) {
        SysJobApiToken current = requireToken(tenantId, tokenId);
        if (!JobApiTokenStatus.ACTIVE.equals(current.getStatus())) {
            throw new BusinessException("Token已吊销");
        }
        if (tokenMapper.revoke(tenantId, tokenId, now()) == 0) {
            throw new BusinessException("Token状态已变化，请刷新后重试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public JobApiTokenCreatedVO rotate(Long tenantId, Long tokenId) {
        SysJobApiToken current = requireToken(tenantId, tokenId);
        LocalDateTime now = now();
        if (!JobApiTokenStatus.ACTIVE.equals(current.getStatus())
                || current.getExpiresAt() == null || !current.getExpiresAt().isAfter(now)) {
            throw new BusinessException("只有未过期的生效Token可以轮换");
        }
        IssuedJobApiToken issued = tokenCodec.issue();
        SysJobApiToken replacement = copyForRotation(current, issued, now);
        if (tokenMapper.insert(replacement) <= 0 || replacement.getId() == null) {
            throw new BusinessException("创建轮换Token失败");
        }
        if (tokenMapper.revoke(tenantId, tokenId, now) == 0) {
            throw new BusinessException("Token状态已变化，轮换已取消");
        }
        return new JobApiTokenCreatedVO(
                replacement.getId(), issued.token(), issued.prefix(), replacement.getExpiresAt());
    }

    @Transactional(rollbackFor = Exception.class)
    public JobApiPrincipal authenticate(String authorizationHeader) {
        String tokenValue = extractBearerToken(authorizationHeader);
        try {
            String keyId = tokenCodec.extractKeyId(tokenValue);
            SysJobApiToken token = keyId == null ? null : tokenMapper.selectActiveByTokenKeyId(keyId);
            boolean matches = tokenCodec.matches(
                    tokenValue, token == null ? DUMMY_HASH : token.getTokenHash());
            LocalDateTime now = now();
            if (token == null || !matches || !JobApiTokenStatus.ACTIVE.equals(token.getStatus())
                    || token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)) {
                throw JobOpenApiException.unauthorized();
            }
            Set<String> scopes = parseScopes(token.getScopes());
            Set<Long> jobIds = readJson(token.getResourceJobIds(), LONG_SET_TYPE);
            Set<String> jobGroups = readJson(token.getResourceJobGroups(), STRING_SET_TYPE);
            if (scopes.isEmpty() || !JobApiScopes.ALLOWED.containsAll(scopes)
                    || (jobIds.isEmpty() && jobGroups.isEmpty())) {
                throw JobOpenApiException.unauthorized();
            }
            touchLastUsed(token, now);
            return new JobApiPrincipal(
                    token.getId(), token.getTenantId(), token.getTokenKeyId(), token.getCallerName(),
                    scopes, jobIds, jobGroups);
        } catch (JobOpenApiException exception) {
            throw exception;
        } catch (BusinessException | IllegalStateException exception) {
            throw JobOpenApiException.unavailable();
        }
    }

    private SysJobApiToken buildToken(
            NormalizedTokenRequest request, IssuedJobApiToken issued, LocalDateTime now) {
        SysJobApiToken token = new SysJobApiToken();
        token.setTenantId(request.tenantId());
        token.setCallerName(request.callerName());
        token.setCallerDescription(request.callerDescription());
        token.setTokenKeyId(issued.keyId());
        token.setTokenPrefix(issued.prefix());
        token.setTokenHash(issued.tokenHash());
        token.setScopes(String.join(" ", request.scopes()));
        token.setResourceJobIds(writeJson(request.jobIds()));
        token.setResourceJobGroups(writeJson(request.jobGroups()));
        token.setStatus(JobApiTokenStatus.ACTIVE);
        token.setIssuedAt(now);
        token.setExpiresAt(request.expiresAt());
        token.setDelFlag(0);
        return token;
    }

    private SysJobApiToken copyForRotation(
            SysJobApiToken current, IssuedJobApiToken issued, LocalDateTime now) {
        SysJobApiToken replacement = new SysJobApiToken();
        replacement.setTenantId(current.getTenantId());
        replacement.setCallerName(current.getCallerName());
        replacement.setCallerDescription(current.getCallerDescription());
        replacement.setTokenKeyId(issued.keyId());
        replacement.setTokenPrefix(issued.prefix());
        replacement.setTokenHash(issued.tokenHash());
        replacement.setScopes(current.getScopes());
        replacement.setResourceJobIds(current.getResourceJobIds());
        replacement.setResourceJobGroups(current.getResourceJobGroups());
        replacement.setStatus(JobApiTokenStatus.ACTIVE);
        replacement.setIssuedAt(now);
        replacement.setExpiresAt(current.getExpiresAt());
        replacement.setCreateBy(current.getCreateBy());
        replacement.setCreateDept(current.getCreateDept());
        replacement.setDelFlag(0);
        return replacement;
    }

    private NormalizedTokenRequest normalize(Long tenantId, JobApiTokenCreateRequest request) {
        requireTenant(tenantId);
        if (request == null) {
            throw new BusinessException("Token创建参数不能为空");
        }
        String callerName = trimToNull(request.callerName());
        if (callerName == null || callerName.length() > 100) {
            throw new BusinessException("调用方名称不能为空且不能超过100个字符");
        }
        String description = trimToNull(request.callerDescription());
        if (description != null && description.length() > 500) {
            throw new BusinessException("调用方说明不能超过500个字符");
        }
        Set<String> scopes = sortedSet(request.scopes());
        if (scopes.isEmpty() || !JobApiScopes.ALLOWED.containsAll(scopes)) {
            throw new BusinessException("Token Scope包含不支持的权限");
        }
        Set<Long> jobIds = request.jobIds() == null ? Set.of() : request.jobIds().stream()
                .filter(value -> value != null && value > 0)
                .collect(Collectors.toCollection(TreeSet::new));
        if (request.jobIds() != null && jobIds.size() != request.jobIds().size()) {
            throw new BusinessException("任务ID必须为正整数");
        }
        Set<String> groups = normalizeGroups(request.jobGroups());
        if (jobIds.isEmpty() && groups.isEmpty()) {
            throw new BusinessException("至少选择一个任务ID或任务组");
        }
        if (request.expiresAt() == null) {
            throw new BusinessException("Token过期时间不能为空");
        }
        return new NormalizedTokenRequest(
                tenantId, callerName, description, scopes, jobIds, groups, request.expiresAt());
    }

    private Set<String> normalizeGroups(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> normalized = new TreeSet<>();
        for (String value : values) {
            String group = trimToNull(value);
            if (group == null || group.length() > 200 || group.chars().anyMatch(Character::isISOControl)) {
                throw new BusinessException("任务组不能为空、不能超过200字符且不能包含控制字符");
            }
            normalized.add(group);
        }
        if (normalized.size() != values.size()) {
            throw new BusinessException("任务组不能重复");
        }
        return normalized;
    }

    private Set<String> sortedSet(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private SysJobApiToken requireToken(Long tenantId, Long tokenId) {
        requireTenant(tenantId);
        if (tokenId == null || tokenId <= 0) {
            throw new BusinessException("Token ID无效");
        }
        SysJobApiToken token = tokenMapper.selectTenantById(tenantId, tokenId);
        if (token == null) {
            throw new BusinessException("Token不存在");
        }
        return token;
    }

    private JobApiTokenVO toView(SysJobApiToken token) {
        JobApiTokenVO view = new JobApiTokenVO();
        view.setId(token.getId());
        view.setCallerName(token.getCallerName());
        view.setCallerDescription(token.getCallerDescription());
        view.setTokenPrefix(token.getTokenPrefix());
        view.setScopes(parseScopes(token.getScopes()));
        view.setJobIds(readJsonForManagement(token.getResourceJobIds(), LONG_SET_TYPE));
        view.setJobGroups(readJsonForManagement(token.getResourceJobGroups(), STRING_SET_TYPE));
        view.setStatus(effectiveStatus(token));
        view.setIssuedAt(token.getIssuedAt());
        view.setExpiresAt(token.getExpiresAt());
        view.setLastUsedAt(token.getLastUsedAt());
        view.setRevokedAt(token.getRevokedAt());
        view.setCreateTime(token.getCreateTime());
        return view;
    }

    private void touchLastUsed(SysJobApiToken token, LocalDateTime now) {
        LocalDateTime cutoff = now.minus(jobProperties.getOpenApi().validatedLastUsedTouchInterval());
        if (token.getLastUsedAt() == null || token.getLastUsedAt().isBefore(cutoff)) {
            if (tokenMapper.touchLastUsed(token.getTenantId(), token.getId(), now) == 0) {
                throw JobOpenApiException.unauthorized();
            }
        }
    }

    private String extractBearerToken(String header) {
        if (header == null || header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        String value = header.substring(7).trim();
        return value.indexOf(' ') >= 0 ? "" : value;
    }

    private String normalizeStatus(String status) {
        String value = trimToNull(status);
        if (value == null) {
            return null;
        }
        if (!Set.of(JobApiTokenStatus.ACTIVE, JobApiTokenStatus.REVOKED, JobApiTokenStatus.EXPIRED)
                .contains(value)) {
            throw new BusinessException("Token状态筛选值无效");
        }
        return value;
    }

    private String effectiveStatus(SysJobApiToken token) {
        if (JobApiTokenStatus.ACTIVE.equals(token.getStatus())
                && token.getExpiresAt() != null && !token.getExpiresAt().isAfter(now())) {
            return JobApiTokenStatus.EXPIRED;
        }
        return token.getStatus();
    }

    private Set<String> parseScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scopes.trim().split(" +"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private <T> Set<T> readJson(String json, TypeReference<Set<T>> type) {
        try {
            Set<T> values = objectMapper.readValue(json, type);
            return values == null ? Set.of() : new LinkedHashSet<>(values);
        } catch (JsonProcessingException | RuntimeException exception) {
            throw JobOpenApiException.unauthorized();
        }
    }

    private <T> Set<T> readJsonForManagement(String json, TypeReference<Set<T>> type) {
        try {
            Set<T> values = objectMapper.readValue(json, type);
            return values == null ? Set.of() : new LinkedHashSet<>(values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Token资源范围数据损坏");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Token资源范围序列化失败");
        }
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("租户身份无效");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private record NormalizedTokenRequest(
            Long tenantId,
            String callerName,
            String callerDescription,
            Set<String> scopes,
            Set<Long> jobIds,
            Set<String> jobGroups,
            LocalDateTime expiresAt) {
    }
}
