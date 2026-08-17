package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.exception.SaTokenContextException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyEditDTO;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyQuery;
import com.mdframe.forge.plugin.system.entity.SysCachePolicy;
import com.mdframe.forge.plugin.system.mapper.SysCachePolicyMapper;
import com.mdframe.forge.plugin.system.service.ISysManagedCachePolicyService;
import com.mdframe.forge.plugin.system.vo.SysManagedCachePolicyVO;
import com.mdframe.forge.starter.cache.managed.ForgeManagedCacheManager;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.model.EffectiveCachePolicy;
import com.mdframe.forge.starter.cache.managed.model.ManagedCacheView;
import com.mdframe.forge.starter.cache.managed.transaction.CacheTransactionExecutor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysManagedCachePolicyServiceImpl implements ISysManagedCachePolicyService {

    private static final long PLATFORM_TENANT_ID = 1L;

    private final SysCachePolicyMapper cachePolicyMapper;
    private final ForgeManagedCacheManager cacheManager;
    private final CacheTransactionExecutor transactionExecutor;

    @Override
    public Page<SysManagedCachePolicyVO> page(SysCachePolicyQuery query) {
        SysCachePolicyQuery safeQuery = query == null ? new SysCachePolicyQuery() : query;
        Map<String, SysCachePolicy> policies = new HashMap<>();
        cachePolicyMapper.selectActivePolicies(PLATFORM_TENANT_ID)
                .forEach(policy -> policies.put(identity(policy.getApplicationCode(), policy.getCacheName()), policy));

        List<SysManagedCachePolicyVO> filtered = cacheManager.listCaches().stream()
                .filter(view -> containsIgnoreCase(
                        view.definition().applicationCode(), safeQuery.getApplicationCode()))
                .filter(view -> containsIgnoreCase(view.definition().cacheName(), safeQuery.getCacheName()))
                .map(view -> toView(view, policies.get(view.definition().identity())))
                .toList();

        int pageNum = safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize();
        long startOffset = (long) (pageNum - 1) * pageSize;
        int start = (int) Math.min(startOffset, filtered.size());
        int end = Math.min(start + pageSize, filtered.size());
        Page<SysManagedCachePolicyVO> page = new Page<>(pageNum, pageSize, filtered.size());
        page.setRecords(filtered.subList(start, end));
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(SysCachePolicyEditDTO dto) {
        SysCachePolicy existing = cachePolicyMapper.selectByIdentity(
                PLATFORM_TENANT_ID, dto.getApplicationCode(), dto.getCacheName());
        long nextVersion = existing == null ? 1L : existing.getPolicyVersion() + 1L;
        if (existing == null && dto.getPolicyVersion() != null && dto.getPolicyVersion() > 0) {
            throw conflict();
        }
        if (existing != null && !existing.getPolicyVersion().equals(dto.getPolicyVersion())) {
            throw conflict();
        }

        CachePolicyOverride override = toOverride(dto, nextVersion);
        cacheManager.validateOverride(override);
        if (existing == null) {
            insert(dto, nextVersion);
        } else {
            update(existing, dto, nextVersion);
        }
        transactionExecutor.afterCommit(() -> cacheManager.applyOverride(override));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reset(String applicationCode, String cacheName) {
        requireDefinition(applicationCode, cacheName);
        SysCachePolicy existing = cachePolicyMapper.selectByIdentity(
                PLATFORM_TENANT_ID, applicationCode, cacheName);
        if (existing != null) {
            int affected = cachePolicyMapper.logicalDeleteWithVersion(
                    PLATFORM_TENANT_ID, applicationCode, cacheName, existing.getPolicyVersion(),
                    currentUserId(), LocalDateTime.now());
            if (affected != 1) {
                throw conflict();
            }
        }
        transactionExecutor.afterCommit(() -> cacheManager.removeOverride(applicationCode, cacheName));
    }

    @Override
    public void clear(String applicationCode, String cacheName) {
        requireDefinition(applicationCode, cacheName);
        transactionExecutor.afterCommit(() -> cacheManager.clear(applicationCode, cacheName));
    }

    @Override
    public void synchronizePolicies() {
        List<SysCachePolicy> activePolicies = cachePolicyMapper.selectActivePolicies(PLATFORM_TENANT_ID);
        Set<String> retainedIdentities = activePolicies.stream()
                .map(policy -> identity(policy.getApplicationCode(), policy.getCacheName()))
                .collect(Collectors.toUnmodifiableSet());
        for (SysCachePolicy policy : activePolicies) {
            try {
                cacheManager.applyOverride(toOverride(policy));
            } catch (RuntimeException exception) {
                log.warn("同步受管缓存策略失败，已跳过: cache={}",
                        identity(policy.getApplicationCode(), policy.getCacheName()), exception);
            }
        }
        cacheManager.removeOverridesNotIn(retainedIdentities);
    }

    private void insert(SysCachePolicyEditDTO dto, long nextVersion) {
        SysCachePolicy policy = toEntity(dto, nextVersion);
        try {
            if (cachePolicyMapper.insert(policy) != 1) {
                throw new BusinessException("保存缓存策略失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "缓存策略已被其他请求创建，请刷新后重试", exception);
        }
    }

    private void update(SysCachePolicy existing, SysCachePolicyEditDTO dto, long nextVersion) {
        SysCachePolicy policy = toEntity(dto, nextVersion);
        policy.setId(existing.getId());
        policy.setUpdateBy(currentUserId());
        policy.setUpdateTime(LocalDateTime.now());
        if (cachePolicyMapper.updateWithVersion(policy, existing.getPolicyVersion()) != 1) {
            throw conflict();
        }
    }

    private SysManagedCachePolicyVO toView(ManagedCacheView runtimeView, SysCachePolicy override) {
        CacheDefinition definition = runtimeView.definition();
        EffectiveCachePolicy policy = override == null
                ? EffectiveCachePolicy.from(definition, null)
                : EffectiveCachePolicy.from(definition, toOverride(override));
        SysManagedCachePolicyVO view = new SysManagedCachePolicyVO();
        view.setApplicationCode(definition.applicationCode());
        view.setCacheName(definition.cacheName());
        view.setDescription(definition.description());
        view.setSource(definition.source());
        view.setScope(definition.scope());
        view.setAllowedModes(definition.allowedModes());
        view.setEnabled(policy.enabled());
        view.setCacheMode(policy.cacheMode());
        view.setLocalTtlSeconds(policy.localTtlSeconds());
        view.setRedisTtlSeconds(policy.redisTtlSeconds());
        view.setLocalMaxSize(policy.localMaxSize());
        view.setCacheNull(policy.cacheNull());
        view.setNullTtlSeconds(policy.nullTtlSeconds());
        view.setPolicyVersion(policy.policyVersion());
        view.setOverridden(override != null);
        view.setHitCount(runtimeView.hitCount());
        view.setMissCount(runtimeView.missCount());
        view.setPutCount(runtimeView.putCount());
        view.setEvictionCount(runtimeView.evictionCount());
        view.setFailureCount(runtimeView.failureCount());
        return view;
    }

    private SysCachePolicy toEntity(SysCachePolicyEditDTO dto, long version) {
        SysCachePolicy policy = new SysCachePolicy();
        policy.setTenantId(PLATFORM_TENANT_ID);
        policy.setApplicationCode(dto.getApplicationCode());
        policy.setCacheName(dto.getCacheName());
        policy.setEnabled(dto.getEnabled());
        policy.setCacheMode(dto.getCacheMode());
        policy.setLocalTtlSeconds(dto.getLocalTtlSeconds());
        policy.setRedisTtlSeconds(dto.getRedisTtlSeconds());
        policy.setLocalMaxSize(dto.getLocalMaxSize());
        policy.setCacheNull(dto.getCacheNull());
        policy.setNullTtlSeconds(dto.getNullTtlSeconds());
        policy.setPolicyVersion(version);
        policy.setDelFlag(0L);
        return policy;
    }

    private CachePolicyOverride toOverride(SysCachePolicyEditDTO dto, long version) {
        return new CachePolicyOverride(
                dto.getApplicationCode(), dto.getCacheName(), dto.getEnabled(), dto.getCacheMode(),
                dto.getLocalTtlSeconds(), dto.getRedisTtlSeconds(), dto.getLocalMaxSize(),
                dto.getCacheNull(), dto.getNullTtlSeconds(), version);
    }

    private CachePolicyOverride toOverride(SysCachePolicy policy) {
        return new CachePolicyOverride(
                policy.getApplicationCode(), policy.getCacheName(), policy.getEnabled(), policy.getCacheMode(),
                policy.getLocalTtlSeconds(), policy.getRedisTtlSeconds(), policy.getLocalMaxSize(),
                policy.getCacheNull(), policy.getNullTtlSeconds(), policy.getPolicyVersion());
    }

    private void requireDefinition(String applicationCode, String cacheName) {
        if (cacheManager.findDefinition(applicationCode, cacheName) == null) {
            throw new IllegalArgumentException("缓存定义不存在: " + identity(applicationCode, cacheName));
        }
    }

    private boolean containsIgnoreCase(String value, String search) {
        return !StringUtils.hasText(search)
                || value.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private String identity(String applicationCode, String cacheName) {
        return applicationCode + "::" + cacheName;
    }

    private BusinessException conflict() {
        return new BusinessException(409, "缓存策略已被其他请求修改，请刷新后重试");
    }

    private Long currentUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (SaTokenContextException exception) {
            log.debug("当前线程没有登录上下文，缓存策略审计用户留空");
            return null;
        }
    }
}
