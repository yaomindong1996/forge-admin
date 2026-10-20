package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.impl.FlowModelPublicationQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowModelControllerCatalogTest {

    @Test
    void defaultListKeepsPublishedOnlyCompatibility() {
        FlowModelService service = mock(FlowModelService.class);
        FlowModel deployed = new FlowModel();
        when(service.getEnabledModels("approval")).thenReturn(List.of(deployed));
        FlowModelController controller = new FlowModelController(service, mock(FlowModelPublicationQueryService.class));

        var response = controller.list("approval", null, false);

        assertThat(response.getData()).containsExactly(deployed);
        verify(service).getEnabledModels("approval");
        verify(service, never()).getModels("approval", null);
    }

    @Test
    void includeDraftUsesDesignCatalog() {
        FlowModelService service = mock(FlowModelService.class);
        FlowModel draft = new FlowModel();
        when(service.getModels("approval", null)).thenReturn(List.of(draft));
        FlowModelController controller = new FlowModelController(service, mock(FlowModelPublicationQueryService.class));

        var response = controller.list("approval", null, true);

        assertThat(response.getData()).containsExactly(draft);
        verify(service).getModels("approval", null);
        verify(service, never()).getEnabledModels("approval");
    }
}
