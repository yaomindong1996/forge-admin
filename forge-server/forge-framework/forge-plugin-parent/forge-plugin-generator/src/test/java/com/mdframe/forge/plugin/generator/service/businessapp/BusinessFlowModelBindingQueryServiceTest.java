package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowModelBindingsQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BusinessFlowModelBindingQueryServiceTest {
    @Test
    void missingSessionCannotFallBackToAnotherTenant() {
        var mapper = mock(BusinessBindingMapper.class);
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(null);
            var query = new BusinessFlowModelBindingsQueryDTO();
            query.setModelKeys(List.of("A"));
            assertThatThrownBy(() -> new BusinessFlowModelBindingQueryService(mapper).list(query))
                    .isInstanceOf(BusinessException.class);
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void keysAreTrimmedDeduplicatedAndQueriedOnceInSessionTenant() {
        var mapper = mock(BusinessBindingMapper.class);
        try (var session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(7L);
            var query = new BusinessFlowModelBindingsQueryDTO();
            query.setModelKeys(List.of(" A ", "B", "A"));
            when(mapper.selectFlowBindingsByModelKeys(anyLong(), anyList(), anyList())).thenReturn(List.of());
            new BusinessFlowModelBindingQueryService(mapper).list(query);
            verify(mapper, times(1)).selectFlowBindingsByModelKeys(
                    eq(7L),
                    eq(List.of("A", "B")),
                    eq(List.of("%\"flowModelKey\"%A%", "%\"flowModelKey\"%B%")));
            verifyNoMoreInteractions(mapper);
        }
    }
}
