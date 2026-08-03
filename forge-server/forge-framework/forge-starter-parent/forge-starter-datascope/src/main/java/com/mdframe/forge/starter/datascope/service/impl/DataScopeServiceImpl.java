package com.mdframe.forge.starter.datascope.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.entity.SysRoleDataScope;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import com.mdframe.forge.starter.datascope.mapper.DataScopeOrgMapper;
import com.mdframe.forge.starter.datascope.mapper.DataScopeRegionMapper;
import com.mdframe.forge.starter.datascope.mapper.DataScopeRoleMapper;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleDataScopeMapper;
import com.mdframe.forge.starter.datascope.model.DataScopeOrgInfo;
import com.mdframe.forge.starter.datascope.model.DataScopeRegionInfo;
import com.mdframe.forge.starter.datascope.model.DataScopeRoleInfo;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 数据权限服务实现。
 *
 * <p>数据权限配置、角色、组织和行政区划属于 Forge 平台控制面元数据，始终从主库加载到内存快照。
 * 业务库查询期间只读取内存快照，避免 dynamic-datasource 切到租户业务库后继续访问 sys_* 平台表。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements IDataScopeService {

    private final SysDataScopeConfigMapper dataScopeConfigMapper;
    private final DataScopeRoleMapper roleMapper;
    private final DataScopeOrgMapper orgMapper;
    private final SysRoleDataScopeMapper roleDataScopeMapper;
    private final DataScopeRegionMapper regionMapper;
    private final DataScopeProperties properties;

    private final Object metadataRefreshMonitor = new Object();

    private volatile boolean metadataLoaded = false;
    private volatile Map<String, SysDataScopeConfig> tenantConfigByMapper = Collections.emptyMap();
    private volatile Map<String, SysDataScopeConfig> defaultConfigByMapper = Collections.emptyMap();
    private volatile Map<Long, Integer> roleDataScopeByRoleId = Collections.emptyMap();
    private volatile Map<Long, Set<Long>> customOrgIdsByRoleId = Collections.emptyMap();
    private volatile Map<Long, Set<Long>> orgAndChildIdsByOrgId = Collections.emptyMap();
    private volatile Map<String, Set<String>> regionAndChildCodesByCode = Collections.emptyMap();

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpDataScopeMetadata() {
        try {
            refreshDataScopeCache();
        } catch (Exception e) {
            metadataLoaded = false;
            log.error("数据权限平台元数据预热失败，后续首次使用时将重试", e);
        }
    }

    @Override
    public DataScopeContext getCurrentUserDataScope() {
        LoginUser loginUser = ExecutionIdentityContextHolder.current()
                .map(identity -> identity.loginUser())
                .orElse(null);
        if (loginUser == null) {
            if (!StpUtil.isLogin()) {
                return null;
            }
            loginUser = SessionHelper.getLoginUser();
        }
        if (loginUser == null) {
            return null;
        }

        if (loginUser.isAdmin()) {
            DataScopeContext context = new DataScopeContext();
            context.setUserId(loginUser.getUserId());
            context.setTenantId(loginUser.getTenantId());
            context.setMinDataScope(DataScopeType.ALL.getCode());
            fillActiveOrgInfo(context, loginUser);
            fillRegionInfo(context, loginUser);
            return context;
        }

        if (loginUser.isTenantAdmin()) {
            DataScopeContext context = new DataScopeContext();
            context.setUserId(loginUser.getUserId());
            context.setTenantId(loginUser.getTenantId());
            context.setMinDataScope(DataScopeType.TENANT_ALL.getCode());
            fillActiveOrgInfo(context, loginUser);
            fillRegionInfo(context, loginUser);
            return context;
        }

        List<Long> roleIds = loginUser.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            DataScopeContext context = new DataScopeContext();
            context.setUserId(loginUser.getUserId());
            context.setTenantId(loginUser.getTenantId());
            context.setMinDataScope(DataScopeType.SELF.getCode());
            fillActiveOrgInfo(context, loginUser);
            fillRegionInfo(context, loginUser);
            return context;
        }

        ensureMetadataLoaded();
        Integer minDataScope = roleIds.stream()
                .map(roleDataScopeByRoleId::get)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(DataScopeType.SELF.getCode());

        Set<Long> customOrgIds = null;
        if (DataScopeType.CUSTOM.getCode().equals(minDataScope)) {
            customOrgIds = collectCustomOrgIds(roleIds);
        }

        DataScopeContext context = new DataScopeContext();
        context.setUserId(loginUser.getUserId());
        context.setTenantId(loginUser.getTenantId());
        context.setRoleIds(roleIds);
        fillActiveOrgInfo(context, loginUser);
        context.setMinDataScope(minDataScope);
        context.setCustomOrgIds(customOrgIds);
        fillRegionInfo(context, loginUser);

        return context;
    }

    private void fillActiveOrgInfo(DataScopeContext context, LoginUser loginUser) {
        Long activeOrgId = loginUser.getActiveOrgId();
        context.setActiveOrgId(activeOrgId);
        context.setOrgIds(activeOrgId == null ? Collections.emptyList() : List.of(activeOrgId));
    }

    private void fillRegionInfo(DataScopeContext context, LoginUser loginUser) {
        context.setRegionCode(loginUser.getRegionCode());
        context.setRegionLevel(loginUser.getRegionLevel());
        context.setRegionAncestors(loginUser.getRegionAncestors());
    }

    @Override
    public SysDataScopeConfig getDataScopeConfig(String mapperId) {
        if (StrUtil.isBlank(mapperId)) {
            return null;
        }
        ensureMetadataLoaded();

        Long tenantId = resolveCurrentTenantId();
        if (tenantId != null) {
            SysDataScopeConfig tenantConfig = tenantConfigByMapper.get(buildConfigKey(tenantId, mapperId));
            if (tenantConfig != null) {
                return tenantConfig;
            }
        }
        return defaultConfigByMapper.get(mapperId);
    }

    @Override
    public Set<Long> getOrgAndChildIds(List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Collections.emptySet();
        }
        ensureMetadataLoaded();

        Set<Long> allOrgIds = new LinkedHashSet<>();
        for (Long orgId : orgIds) {
            if (orgId == null) {
                continue;
            }
            Set<Long> cached = orgAndChildIdsByOrgId.get(orgId);
            if (cached != null && !cached.isEmpty()) {
                allOrgIds.addAll(cached);
            } else {
                allOrgIds.add(orgId);
            }
        }
        return Collections.unmodifiableSet(allOrgIds);
    }

    @Override
    public Set<String> getRegionAndChildCodes(String regionCode) {
        if (StrUtil.isBlank(regionCode)) {
            return Collections.emptySet();
        }
        ensureMetadataLoaded();

        Set<String> cached = regionAndChildCodesByCode.get(regionCode);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        return Collections.singleton(regionCode);
    }

    @Override
    public void refreshDataScopeCache() {
        synchronized (metadataRefreshMonitor) {
            MetadataSnapshot snapshot = executeOnMetadataDatasource(this::loadMetadataSnapshot);
            tenantConfigByMapper = snapshot.tenantConfigByMapper();
            defaultConfigByMapper = snapshot.defaultConfigByMapper();
            roleDataScopeByRoleId = snapshot.roleDataScopeByRoleId();
            customOrgIdsByRoleId = snapshot.customOrgIdsByRoleId();
            orgAndChildIdsByOrgId = snapshot.orgAndChildIdsByOrgId();
            regionAndChildCodesByCode = snapshot.regionAndChildCodesByCode();
            metadataLoaded = true;
            log.info("数据权限平台元数据缓存已刷新: configs={}, roles={}, customRoles={}, orgs={}, regions={}",
                    tenantConfigByMapper.size(), roleDataScopeByRoleId.size(), customOrgIdsByRoleId.size(),
                    orgAndChildIdsByOrgId.size(), regionAndChildCodesByCode.size());
        }
    }

    private void ensureMetadataLoaded() {
        if (metadataLoaded) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("数据权限平台元数据缓存未初始化，不能在业务事务中加载");
        }
        refreshDataScopeCache();
    }

    private MetadataSnapshot loadMetadataSnapshot() {
        List<SysDataScopeConfig> configs = dataScopeConfigMapper.selectEnabledConfigs();
        List<DataScopeRoleInfo> roles = roleMapper.selectActiveRoleDataScopes();
        List<SysRoleDataScope> roleDataScopes = roleDataScopeMapper.selectAllRoleDataScopes();
        List<DataScopeOrgInfo> orgs = orgMapper.selectAllOrgAncestors();
        List<DataScopeRegionInfo> regions = regionMapper.selectAllRegions();

        return new MetadataSnapshot(
                buildTenantConfigMap(configs),
                buildDefaultConfigMap(configs),
                buildRoleDataScopeMap(roles),
                buildCustomOrgMap(roleDataScopes),
                buildOrgChildMap(orgs),
                buildRegionChildMap(regions)
        );
    }

    private Map<String, SysDataScopeConfig> buildTenantConfigMap(List<SysDataScopeConfig> configs) {
        Map<String, SysDataScopeConfig> result = new HashMap<>();
        if (configs == null) {
            return Collections.emptyMap();
        }
        for (SysDataScopeConfig config : configs) {
            if (config == null || config.getTenantId() == null || StrUtil.isBlank(config.getMapperMethod())) {
                continue;
            }
            result.putIfAbsent(buildConfigKey(config.getTenantId(), config.getMapperMethod()), config);
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, SysDataScopeConfig> buildDefaultConfigMap(List<SysDataScopeConfig> configs) {
        Map<String, SysDataScopeConfig> result = new HashMap<>();
        if (configs == null) {
            return Collections.emptyMap();
        }
        Long defaultTenantId = properties.getDefaultConfigTenantId();
        for (SysDataScopeConfig config : configs) {
            if (config == null || StrUtil.isBlank(config.getMapperMethod())) {
                continue;
            }
            if (config.getTenantId() == null || Objects.equals(config.getTenantId(), defaultTenantId)) {
                result.putIfAbsent(config.getMapperMethod(), config);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<Long, Integer> buildRoleDataScopeMap(List<DataScopeRoleInfo> roles) {
        Map<Long, Integer> result = new HashMap<>();
        if (roles == null) {
            return Collections.emptyMap();
        }
        for (DataScopeRoleInfo role : roles) {
            if (role == null || role.getRoleId() == null || role.getDataScope() == null) {
                continue;
            }
            result.put(role.getRoleId(), role.getDataScope());
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<Long, Set<Long>> buildCustomOrgMap(List<SysRoleDataScope> roleDataScopes) {
        Map<Long, Set<Long>> mutable = new HashMap<>();
        if (roleDataScopes != null) {
            for (SysRoleDataScope item : roleDataScopes) {
                if (item == null || item.getRoleId() == null || item.getOrgId() == null) {
                    continue;
                }
                mutable.computeIfAbsent(item.getRoleId(), key -> new LinkedHashSet<>()).add(item.getOrgId());
            }
        }
        return unmodifiableNestedLongMap(mutable);
    }

    private Map<Long, Set<Long>> buildOrgChildMap(List<DataScopeOrgInfo> orgs) {
        Map<Long, Set<Long>> mutable = new HashMap<>();
        if (orgs != null) {
            for (DataScopeOrgInfo org : orgs) {
                if (org == null || org.getOrgId() == null) {
                    continue;
                }
                mutable.computeIfAbsent(org.getOrgId(), key -> new LinkedHashSet<>()).add(org.getOrgId());
                for (Long ancestorId : parseAncestorIds(org.getAncestors())) {
                    mutable.computeIfAbsent(ancestorId, key -> new LinkedHashSet<>()).add(org.getOrgId());
                }
            }
        }
        return unmodifiableNestedLongMap(mutable);
    }

    private Map<String, Set<String>> buildRegionChildMap(List<DataScopeRegionInfo> regions) {
        Map<String, Set<String>> mutable = new HashMap<>();
        if (regions != null) {
            for (DataScopeRegionInfo region : regions) {
                if (region == null || StrUtil.isBlank(region.getCode())) {
                    continue;
                }
                mutable.computeIfAbsent(region.getCode(), key -> new LinkedHashSet<>()).add(region.getCode());
                if (StrUtil.isNotBlank(region.getParentCode())) {
                    mutable.computeIfAbsent(region.getParentCode(), key -> new LinkedHashSet<>()).add(region.getCode());
                }
            }
        }
        mutable.forEach((code, children) -> children.add(code));
        return unmodifiableNestedStringMap(mutable);
    }

    private Set<Long> collectCustomOrgIds(List<Long> roleIds) {
        Set<Long> result = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            Set<Long> orgIds = customOrgIdsByRoleId.get(roleId);
            if (orgIds != null) {
                result.addAll(orgIds);
            }
        }
        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }

    private List<Long> parseAncestorIds(String ancestors) {
        if (StrUtil.isBlank(ancestors)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String item : ancestors.split(",")) {
            if (StrUtil.isBlank(item)) {
                continue;
            }
            try {
                ids.add(Long.valueOf(item.trim()));
            } catch (NumberFormatException e) {
                log.debug("数据权限组织 ancestors 存在非数字节点: {}", item);
            }
        }
        return ids;
    }

    private Long resolveCurrentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        try {
            return Optional.ofNullable(SessionHelper.getLoginUser())
                    .map(LoginUser::getTenantId)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T executeOnMetadataDatasource(Supplier<T> supplier) {
        String metadataDatasource = properties.getMetadataDatasource();
        if (StrUtil.isBlank(metadataDatasource)) {
            return TenantContextHolder.executeIgnore(supplier);
        }
        DynamicDataSourceContextHolder.push(metadataDatasource);
        try {
            return TenantContextHolder.executeIgnore(supplier);
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private String buildConfigKey(Long tenantId, String mapperId) {
        return tenantId + ":" + mapperId;
    }

    private Map<Long, Set<Long>> unmodifiableNestedLongMap(Map<Long, Set<Long>> source) {
        Map<Long, Set<Long>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(value))));
        return Collections.unmodifiableMap(result);
    }

    private Map<String, Set<String>> unmodifiableNestedStringMap(Map<String, Set<String>> source) {
        Map<String, Set<String>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(value))));
        return Collections.unmodifiableMap(result);
    }

    private record MetadataSnapshot(
            Map<String, SysDataScopeConfig> tenantConfigByMapper,
            Map<String, SysDataScopeConfig> defaultConfigByMapper,
            Map<Long, Integer> roleDataScopeByRoleId,
            Map<Long, Set<Long>> customOrgIdsByRoleId,
            Map<Long, Set<Long>> orgAndChildIdsByOrgId,
            Map<String, Set<String>> regionAndChildCodesByCode) {
    }
}
