package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.DataScopeConfigStatusDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysDataScopeConfigServiceImplTest {
    private final SysDataScopeConfigMapper mapper = mock(SysDataScopeConfigMapper.class);
    private final IDataScopeService cache = mock(IDataScopeService.class);
    private final SysDataScopeConfigServiceImpl service = new SysDataScopeConfigServiceImpl(mapper, cache);

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldRefreshOnlyAfterCommitAndPreserveStatusDuringEditing() {
        TransactionSynchronizationManager.initSynchronization();
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setId(7L);
        config.setEnabled(0);
        when(mapper.updateById(config)).thenReturn(1);

        assertTrue(service.updateConfig(config));
        assertNull(config.getEnabled(), "普通编辑不能覆盖列表启停状态");
        verifyNoInteractions(cache);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(cache).refreshDataScopeCache();
    }

    @Test
    void shouldNotRefreshRolledBackChanges() {
        TransactionSynchronizationManager.initSynchronization();
        when(mapper.insert(any(SysDataScopeConfig.class))).thenReturn(1);
        service.insertConfig(new SysDataScopeConfig());

        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(cache);
    }

    @Test
    void shouldUpdateOnlyStatusUsingTheExpectedOldValue() {
        DataScopeConfigStatusDTO dto = statusChange();
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(9L);
            when(mapper.updateConfigStatus(7L, 0, 1, 9L)).thenReturn(1);
            service.updateConfigStatus(dto);
            verify(mapper).updateConfigStatus(7L, 0, 1, 9L);
            verify(mapper, never()).updateById(any(SysDataScopeConfig.class));
            verify(cache).refreshDataScopeCache();
        }
    }

    @Test
    void shouldRejectStaleOrDeletedRowsWithoutRefreshing() {
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getUserId).thenReturn(9L);
            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.updateConfigStatus(statusChange()));
            assertTrue(error.getMessage().contains("规则已被修改或删除"));
            verifyNoInteractions(cache);
        }
    }

    @Test
    void shouldRejectInvalidStatusBeforeWriting() {
        DataScopeConfigStatusDTO dto = statusChange();
        dto.setEnabled(2);
        assertThrows(BusinessException.class, () -> service.updateConfigStatus(dto));
        verifyNoInteractions(mapper, cache);
    }

    @Test
    void shouldReportThatSaveCommittedWhenRefreshFails() {
        TransactionSynchronizationManager.initSynchronization();
        when(mapper.insert(any(SysDataScopeConfig.class))).thenReturn(1);
        doThrow(new IllegalStateException("refresh unavailable")).when(cache).refreshDataScopeCache();
        service.insertConfig(new SysDataScopeConfig());
        BusinessException error = assertThrows(BusinessException.class, () ->
                TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit));
        assertTrue(error.getMessage().contains("配置已保存"));
        assertTrue(error.getMessage().contains("刷新数据权限"));
    }

    private DataScopeConfigStatusDTO statusChange() {
        DataScopeConfigStatusDTO dto = new DataScopeConfigStatusDTO();
        dto.setId(7L);
        dto.setEnabled(0);
        dto.setExpectedEnabled(1);
        return dto;
    }
}
