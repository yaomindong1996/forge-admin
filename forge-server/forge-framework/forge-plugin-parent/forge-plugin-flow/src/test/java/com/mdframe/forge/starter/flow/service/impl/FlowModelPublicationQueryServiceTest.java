package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.mapper.FlowModelVersionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FlowModelPublicationQueryServiceTest {
    @Test
    void emptyPageSkipsQueryAndForeignTenantFailsBeforeSnapshotLookup() {
        var mapper = mock(FlowModelVersionMapper.class);
        var service = new FlowModelPublicationQueryService(mapper);
        service.enrich(List.of());
        verifyNoInteractions(mapper);
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(1L);
            var model = new FlowModel();
            model.setTenantId(2L);
            assertThatThrownBy(() -> service.enrich(List.of(model))).isInstanceOf(BusinessException.class);
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void onePageUsesOneTrustedTenantBatch() {
        var mapper = mock(FlowModelVersionMapper.class);
        var service = new FlowModelPublicationQueryService(mapper);
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(1L);
            var model = new FlowModel();
            model.setId("A");
            model.setTenantId(1L);
            when(mapper.selectPublicationSnapshots(List.of("A"), 1L)).thenReturn(List.of());
            service.enrich(List.of(model));
            verify(mapper, times(1)).selectPublicationSnapshots(List.of("A"), 1L);
        }
    }
}
