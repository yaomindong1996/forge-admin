package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyEditDTO;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyQuery;
import com.mdframe.forge.plugin.system.entity.SysCachePolicy;
import com.mdframe.forge.plugin.system.mapper.SysCachePolicyMapper;
import com.mdframe.forge.plugin.system.vo.SysManagedCachePolicyVO;
import com.mdframe.forge.starter.cache.managed.ForgeManagedCacheManager;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import com.mdframe.forge.starter.cache.managed.transaction.CacheTransactionExecutor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysManagedCachePolicyServiceTest {

    @Test
    void pageShouldMergeRegisteredDefinitionWithDatabaseOverride() {
        MapperStub stub = new MapperStub();
        stub.active.set(policy(7L, CacheMode.LOCAL, 3L));
        SysManagedCachePolicyServiceImpl service = service(stub);

        SysCachePolicyQuery query = new SysCachePolicyQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        Page<SysManagedCachePolicyVO> result = service.page(query);

        assertThat(result.getTotal()).isOne();
        assertThat(result.getRecords()).singleElement().satisfies(view -> {
            assertThat(view.getCacheName()).isEqualTo("system:dict-data");
            assertThat(view.getCacheMode()).isEqualTo(CacheMode.LOCAL);
            assertThat(view.getPolicyVersion()).isEqualTo(3L);
            assertThat(view.getOverridden()).isTrue();
            assertThat(view.getScope()).isEqualTo(CacheScope.TENANT);
        });
    }

    @Test
    void editShouldInsertFirstOverrideAndApplyItToRuntime() {
        MapperStub stub = new MapperStub();
        SysManagedCachePolicyServiceImpl service = service(stub);

        service.edit(edit(null, CacheMode.MULTI, 60, 1800));

        assertThat(stub.inserted.get()).isNotNull();
        assertThat(stub.inserted.get().getTenantId()).isEqualTo(1L);
        assertThat(stub.inserted.get().getPolicyVersion()).isEqualTo(1L);
        assertThat(service.page(new SysCachePolicyQuery()).getRecords())
                .singleElement()
                .extracting(SysManagedCachePolicyVO::getOverridden)
                .isEqualTo(true);
    }

    @Test
    void editShouldRejectStaleVersionBeforeRuntimePropagation() {
        MapperStub stub = new MapperStub();
        stub.active.set(policy(9L, CacheMode.REDIS, 4L));
        SysManagedCachePolicyServiceImpl service = service(stub);

        assertThatThrownBy(() -> service.edit(edit(3L, CacheMode.LOCAL, 60, 1800)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
        assertThat(stub.updated.get()).isZero();
    }

    @Test
    void editShouldRejectModeOutsideCodeOwnedAllowList() {
        MapperStub stub = new MapperStub();
        SysManagedCachePolicyServiceImpl service = service(stub, List.of(CacheMode.LOCAL, CacheMode.MULTI));

        assertThatThrownBy(() -> service.edit(edit(null, CacheMode.REDIS, 60, 1800)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("允许范围");
        assertThat(stub.inserted.get()).isNull();
    }

    @Test
    void resetAndClearShouldDelegateOnlyForRegisteredCache() {
        MapperStub stub = new MapperStub();
        stub.active.set(policy(11L, CacheMode.LOCAL, 2L));
        ForgeManagedCacheManager manager = manager(List.of(CacheMode.LOCAL, CacheMode.REDIS, CacheMode.MULTI));
        SysManagedCachePolicyServiceImpl service = new SysManagedCachePolicyServiceImpl(
                stub.mapper(), manager, new CacheTransactionExecutor());

        service.edit(edit(2L, CacheMode.LOCAL, 60, 1800));
        assertThat(manager.listCaches()).singleElement().extracting(view -> view.overridden()).isEqualTo(true);

        service.reset("forge-admin", "system:dict-data");
        assertThat(stub.deleted.get()).isOne();
        assertThat(manager.listCaches()).singleElement().extracting(view -> view.overridden()).isEqualTo(false);

        service.clear("forge-admin", "system:dict-data");
        assertThatThrownBy(() -> service.clear("forge-admin", "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void synchronizeShouldRemoveRuntimeOverrideMissingFromDatabase() {
        MapperStub stub = new MapperStub();
        ForgeManagedCacheManager manager = manager(List.of(CacheMode.LOCAL, CacheMode.REDIS, CacheMode.MULTI));
        manager.applyOverride(new CachePolicyOverride(
                "forge-admin", "system:dict-data", true, CacheMode.REDIS,
                60, 1800, 2000, false, 30, 1));
        SysManagedCachePolicyServiceImpl service = new SysManagedCachePolicyServiceImpl(
                stub.mapper(), manager, new CacheTransactionExecutor());

        service.synchronizePolicies();

        assertThat(manager.listCaches()).singleElement().satisfies(view -> {
            assertThat(view.overridden()).isFalse();
            assertThat(view.policy().cacheMode()).isEqualTo(CacheMode.LOCAL);
        });
    }

    private SysManagedCachePolicyServiceImpl service(MapperStub stub) {
        return service(stub, List.of(CacheMode.LOCAL, CacheMode.REDIS, CacheMode.MULTI));
    }

    private SysManagedCachePolicyServiceImpl service(MapperStub stub, List<CacheMode> allowedModes) {
        return new SysManagedCachePolicyServiceImpl(
                stub.mapper(), manager(allowedModes), new CacheTransactionExecutor());
    }

    private ForgeManagedCacheManager manager(List<CacheMode> allowedModes) {
        ManagedCacheProperties properties = new ManagedCacheProperties();
        properties.setApplicationCode("forge-admin");
        ForgeManagedCacheManager manager = new ForgeManagedCacheManager(null, properties);
        manager.register(new CacheDefinition(
                "forge-admin", "system:dict-data", "字典数据", CacheMode.LOCAL,
                allowedModes, CacheScope.TENANT, 60, 1800, 2000, false, 30,
                "SysDictDataServiceImpl"));
        return manager;
    }

    private SysCachePolicyEditDTO edit(Long version, CacheMode mode, long localTtl, long redisTtl) {
        SysCachePolicyEditDTO dto = new SysCachePolicyEditDTO();
        dto.setApplicationCode("forge-admin");
        dto.setCacheName("system:dict-data");
        dto.setEnabled(true);
        dto.setCacheMode(mode);
        dto.setLocalTtlSeconds(localTtl);
        dto.setRedisTtlSeconds(redisTtl);
        dto.setLocalMaxSize(2000);
        dto.setCacheNull(false);
        dto.setNullTtlSeconds(30L);
        dto.setPolicyVersion(version);
        return dto;
    }

    private SysCachePolicy policy(Long id, CacheMode mode, Long version) {
        SysCachePolicy policy = new SysCachePolicy();
        policy.setId(id);
        policy.setTenantId(1L);
        policy.setApplicationCode("forge-admin");
        policy.setCacheName("system:dict-data");
        policy.setEnabled(true);
        policy.setCacheMode(mode);
        policy.setLocalTtlSeconds(60L);
        policy.setRedisTtlSeconds(1800L);
        policy.setLocalMaxSize(2000);
        policy.setCacheNull(false);
        policy.setNullTtlSeconds(30L);
        policy.setPolicyVersion(version);
        policy.setDelFlag(0L);
        return policy;
    }

    private static final class MapperStub {

        private final AtomicReference<SysCachePolicy> active = new AtomicReference<>();
        private final AtomicReference<SysCachePolicy> inserted = new AtomicReference<>();
        private final AtomicInteger updated = new AtomicInteger();
        private final AtomicInteger deleted = new AtomicInteger();

        private SysCachePolicyMapper mapper() {
            return (SysCachePolicyMapper) Proxy.newProxyInstance(
                    SysCachePolicyMapper.class.getClassLoader(),
                    new Class<?>[]{SysCachePolicyMapper.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "selectActivePolicies" -> active.get() == null
                                ? List.of() : List.of(active.get());
                        case "selectByIdentity" -> active.get();
                        case "insert" -> {
                            SysCachePolicy value = (SysCachePolicy) args[0];
                            value.setId(100L);
                            inserted.set(value);
                            active.set(value);
                            yield 1;
                        }
                        case "updateWithVersion" -> {
                            SysCachePolicy value = (SysCachePolicy) args[0];
                            updated.incrementAndGet();
                            active.set(value);
                            yield 1;
                        }
                        case "logicalDeleteWithVersion" -> {
                            deleted.incrementAndGet();
                            active.set(null);
                            yield 1;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
