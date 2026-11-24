package com.mdframe.forge.starter.datascope.service.impl;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.event.DataScopeCacheRefreshedEvent;
import com.mdframe.forge.starter.datascope.mapper.*;
import com.mdframe.forge.starter.datascope.model.DataScopeRegionInfo;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataScopeServiceImplTest {
    private final SysDataScopeConfigMapper mapper = mock(SysDataScopeConfigMapper.class);
    private final DataScopeRegionMapper regionMapper = mock(DataScopeRegionMapper.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final DataScopeServiceImpl service = new DataScopeServiceImpl(mapper,
            mock(DataScopeRoleMapper.class), mock(DataScopeOrgMapper.class), mock(SysRoleDataScopeMapper.class),
            mock(SysRoleModuleDataScopeMapper.class), regionMapper,
            new DataScopeProperties(), publisher);

    @Test
    void shouldKeepDisabledRulesInTheRuntimeSnapshot() {
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setTenantId(1L);
        config.setMapperMethod("example.OrderMapper.page");
        config.setEnabled(0);
        when(mapper.selectRuntimeConfigs()).thenReturn(List.of(config));
        service.reloadLocalDataScopeCache();
        assertSame(config, service.getDataScopeConfig("example.OrderMapper.page"));
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldPreferTenantDisabledRuleOverDefaultEnabledRule() {
        SysDataScopeConfig fallback = new SysDataScopeConfig();
        fallback.setTenantId(1L);
        fallback.setMapperMethod("example.OrderMapper.page");
        fallback.setEnabled(1);
        SysDataScopeConfig disabled = new SysDataScopeConfig();
        disabled.setTenantId(2L);
        disabled.setMapperMethod(fallback.getMapperMethod());
        disabled.setEnabled(0);
        when(mapper.selectRuntimeConfigs()).thenReturn(List.of(fallback, disabled));
        service.reloadLocalDataScopeCache();
        TenantContextHolder.executeWithTenant(2L, () -> {
            assertSame(disabled, service.getDataScopeConfig(fallback.getMapperMethod()));
        });
    }

    @Test
    void shouldBroadcastOnlyExplicitRefreshAndNeverPublishFailedLoads() {
        service.refreshDataScopeCache();
        verify(publisher).publishEvent(any(DataScopeCacheRefreshedEvent.class));
        clearInvocations(publisher);
        service.reloadLocalDataScopeCache();
        verifyNoInteractions(publisher);
        when(mapper.selectRuntimeConfigs()).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, service::refreshDataScopeCache);
        verifyNoInteractions(publisher);
        doReturn(List.of()).when(mapper).selectRuntimeConfigs();
        assertNull(service.getDataScopeConfig("example.OrderMapper.page"));
    }

    @Test
    void shouldExpandRegionDescendantsAcrossCountyStreetVillage() {
        DataScopeRegionInfo county = region("110101", "110100");
        DataScopeRegionInfo street = region("110101001", "110101");
        DataScopeRegionInfo village = region("110101001001", "110101001");
        when(regionMapper.selectAllRegions()).thenReturn(List.of(county, street, village));
        when(mapper.selectRuntimeConfigs()).thenReturn(List.of());

        service.reloadLocalDataScopeCache();

        assertEquals(Set.of("110101", "110101001", "110101001001"),
                service.getRegionAndChildCodes("110101"));
        assertEquals(Set.of("110101001", "110101001001"),
                service.getRegionAndChildCodes("110101001"));
        assertEquals(Set.of("110101001001"),
                service.getRegionAndChildCodes("110101001001"));
    }

    private static DataScopeRegionInfo region(String code, String parentCode) {
        DataScopeRegionInfo info = new DataScopeRegionInfo();
        info.setCode(code);
        info.setParentCode(parentCode);
        return info;
    }
}
