package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.VersionCleanupDTO;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelVersionMapper;
import com.mdframe.forge.starter.flow.vo.VersionCleanupVO;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowModelVersionCleanupTest {

    private FlowModelVersionMapper versionMapper;
    private FlowModelMapper modelMapper;
    private ProcessInstanceQuery instanceQuery;
    private FlowModelVersionServiceImpl service;
    private MockedStatic<SessionHelper> sessionHelper;

    @BeforeEach
    void setUp() {
        versionMapper = mock(FlowModelVersionMapper.class);
        modelMapper = mock(FlowModelMapper.class);
        RuntimeService runtimeService = mock(RuntimeService.class);
        instanceQuery = mock(ProcessInstanceQuery.class, RETURNS_SELF);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(instanceQuery);
        service = new FlowModelVersionServiceImpl(modelMapper);
        ReflectionTestUtils.setField(service, "baseMapper", versionMapper);
        ReflectionTestUtils.setField(service, "runtimeService", runtimeService);
        sessionHelper = mockStatic(SessionHelper.class);
        sessionHelper.when(SessionHelper::getTenantId).thenReturn(1L);

        FlowModel model = new FlowModel();
        model.setId("model-1");
        model.setVersion(4);
        model.setProcessDefinitionId("def-4");
        when(modelMapper.selectByIdAndTenant("model-1", 1L)).thenReturn(model);
    }

    @AfterEach
    void tearDown() {
        sessionHelper.close();
    }

    @Test
    void cleansOldReleaseVersionsButKeepsCurrentAndRunningOnes() {
        when(versionMapper.selectCleanupCandidates("model-1", 1L)).thenReturn(List.of(
                version("v1", 1, "def-1"), version("v2", 2, "def-2"),
                version("v3", 3, "def-3"), version("v4", 4, "def-4")));
        when(instanceQuery.processDefinitionId(anyString())).thenReturn(instanceQuery);
        when(instanceQuery.count()).thenReturn(1L, 0L);
        when(versionMapper.logicalDeleteByIdAndTenant("v1", 1L)).thenReturn(1);

        VersionCleanupDTO dto = new VersionCleanupDTO();
        dto.setModelId("model-1");
        dto.setRetainLatest(2);
        VersionCleanupVO result = service.cleanupVersions(dto);

        // v4/v3 在保留数内；v2 被运行中实例引用；v1 为旧的「正式发布」版本，应被清理
        assertThat(result.getDeleted()).isEqualTo(1);
        assertThat(result.getSkippedRunning()).isEqualTo(1);
        assertThat(result.getSkippedProtected()).isZero();
        verify(versionMapper).logicalDeleteByIdAndTenant("v1", 1L);
        verify(versionMapper, never()).logicalDeleteByIdAndTenant("v2", 1L);
    }

    @Test
    void refusesToDeleteTheCurrentVersion() {
        when(versionMapper.getVersionDetail("v4", 1L)).thenReturn(version("v4", 4, "def-4"));

        assertThatThrownBy(() -> service.deleteVersion("v4")).hasMessage("当前版本不可删除");
        verify(versionMapper, never()).logicalDeleteByIdAndTenant("v4", 1L);
    }

    private FlowModelVersion version(String id, int number, String definitionId) {
        FlowModelVersion version = new FlowModelVersion();
        version.setId(id);
        version.setModelId("model-1");
        version.setVersion(number);
        version.setVersionTag("release");
        version.setProcessDefinitionId(definitionId);
        version.setCreateTime(LocalDateTime.of(2026, 11, 14, 10, number));
        return version;
    }
}
