package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.mapper.FlowApprovalLevelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowNodeConfigMapper;
import com.mdframe.forge.starter.flow.mapper.FlowNodeOperationMapper;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 节点配置保存必须按主键/(modelId,nodeId) upsert，禁止重复 insert。
 */
class FlowNodeConfigSaveUpsertTest {

    private FlowNodeConfigMapper nodeConfigMapper;
    private FlowApprovalLevelMapper approvalLevelMapper;
    private FlowNodeOperationMapper nodeOperationMapper;
    private FlowNodeConfigServiceImpl service;
    private MockedStatic<TenantContextHolder> tenantContext;
    private MockedStatic<SessionHelper> sessionHelper;

    @BeforeEach
    void setUp() {
        nodeConfigMapper = mock(FlowNodeConfigMapper.class);
        approvalLevelMapper = mock(FlowApprovalLevelMapper.class);
        nodeOperationMapper = mock(FlowNodeOperationMapper.class);
        service = new FlowNodeConfigServiceImpl(
                approvalLevelMapper,
                nodeOperationMapper,
                new ObjectMapper(),
                mock(FlowOrgIntegrationService.class));
        // ServiceImpl.baseMapper 通过反射注入
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", nodeConfigMapper);

        tenantContext = mockStatic(TenantContextHolder.class);
        sessionHelper = mockStatic(SessionHelper.class);
        tenantContext.when(TenantContextHolder::getTenantId).thenReturn(1L);
        sessionHelper.when(SessionHelper::getTenantId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        tenantContext.close();
        sessionHelper.close();
    }

    @Test
    void saveWithExistingIdUpdatesInsteadOfInsert() {
        FlowNodeConfig existing = new FlowNodeConfig();
        existing.setId("cfg-1");
        existing.setTenantId(1L);
        existing.setModelId("model-1");
        existing.setNodeId("Approve_1");
        existing.setDelFlag("0");
        when(nodeConfigMapper.selectByIdAndTenant("cfg-1", 1L)).thenReturn(existing);
        when(nodeConfigMapper.updateById(any(FlowNodeConfig.class))).thenReturn(1);

        FlowNodeConfig incoming = new FlowNodeConfig();
        incoming.setId("cfg-1");
        incoming.setModelId("model-1");
        incoming.setNodeId("Approve_1");
        incoming.setRequireComment(true);
        incoming.setAllowReject(false);

        assertThat(service.saveNodeConfig(incoming, null)).isTrue();

        ArgumentCaptor<FlowNodeConfig> captor = ArgumentCaptor.forClass(FlowNodeConfig.class);
        verify(nodeConfigMapper).updateById(captor.capture());
        verify(nodeConfigMapper, never()).insert(any(FlowNodeConfig.class));
        assertThat(captor.getValue().getId()).isEqualTo("cfg-1");
        assertThat(captor.getValue().getRequireComment()).isTrue();
        assertThat(captor.getValue().getAllowReject()).isFalse();
    }

    @Test
    void saveWithoutIdButSameModelNodeUpdatesExisting() {
        FlowNodeConfig existing = new FlowNodeConfig();
        existing.setId("cfg-2");
        existing.setTenantId(1L);
        existing.setModelId("model-2");
        existing.setNodeId("Approve_1");
        existing.setDelFlag("0");
        when(nodeConfigMapper.selectByModelKeyAndNode("model-2", "Approve_1", 1L)).thenReturn(existing);
        when(nodeConfigMapper.updateById(any(FlowNodeConfig.class))).thenReturn(1);

        FlowNodeConfig incoming = new FlowNodeConfig();
        incoming.setModelId("model-2");
        incoming.setNodeId("Approve_1");
        incoming.setRequireComment(false);

        assertThat(service.saveNodeConfig(incoming, null)).isTrue();
        verify(nodeConfigMapper).updateById(any(FlowNodeConfig.class));
        verify(nodeConfigMapper, never()).insert(any(FlowNodeConfig.class));
        assertThat(incoming.getId()).isEqualTo("cfg-2");
    }

    @Test
    void saveNewNodeInserts() {
        when(nodeConfigMapper.selectByModelKeyAndNode(eq("model-3"), eq("Approve_1"), eq(1L)))
                .thenReturn(null);
        when(nodeConfigMapper.insert(any(FlowNodeConfig.class))).thenAnswer(invocation -> {
            FlowNodeConfig cfg = invocation.getArgument(0);
            cfg.setId("generated-id");
            return 1;
        });

        FlowNodeConfig incoming = new FlowNodeConfig();
        incoming.setModelId("model-3");
        incoming.setNodeId("Approve_1");
        incoming.setRequireComment(true);

        assertThat(service.saveNodeConfig(incoming, null)).isTrue();
        verify(nodeConfigMapper).insert(any(FlowNodeConfig.class));
        verify(nodeConfigMapper, never()).updateById(any(FlowNodeConfig.class));
    }
}
