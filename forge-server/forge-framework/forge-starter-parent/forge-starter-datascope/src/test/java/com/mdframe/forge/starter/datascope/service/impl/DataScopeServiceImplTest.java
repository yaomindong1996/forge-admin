package com.mdframe.forge.starter.datascope.service.impl;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.event.DataScopeCacheRefreshedEvent;
import com.mdframe.forge.starter.datascope.mapper.*;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataScopeServiceImplTest {
    private final SysDataScopeConfigMapper mapper = mock(SysDataScopeConfigMapper.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final DataScopeServiceImpl service = new DataScopeServiceImpl(mapper,
            mock(DataScopeRoleMapper.class), mock(DataScopeOrgMapper.class), mock(SysRoleDataScopeMapper.class),
            mock(SysRoleModuleDataScopeMapper.class), mock(DataScopeRegionMapper.class),
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
}
