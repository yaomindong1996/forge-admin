package com.mdframe.forge.plugin.ai.coordination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.dto.AiModelSaveDTO;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelProviderManagerTest {

    @Mock
    private AiModelService modelService;

    @Mock
    private AiProviderService providerService;

    @Test
    void updateModelShouldSynchronizeOldAndNewProviders() {
        AiModelSaveDTO request = new AiModelSaveDTO();
        request.setId(100L);
        request.setProviderId(20L);

        AiModel existing = model(100L, 10L);
        AiModel updated = model(100L, 20L);
        when(modelService.getById(100L)).thenReturn(existing, updated);
        when(modelService.getByIdForUpdate(100L)).thenReturn(existing);
        when(modelService.getModelIdListByProviderId(10L)).thenReturn(List.of("old-model"));
        when(modelService.getDefaultModelId(10L)).thenReturn("old-model");
        when(modelService.getModelIdListByProviderId(20L)).thenReturn(List.of("new-model"));
        when(modelService.getDefaultModelId(20L)).thenReturn("new-model");

        manager(new ObjectMapper()).updateModel(request);

        InOrder lockOrder = inOrder(modelService, providerService);
        lockOrder.verify(modelService).getById(100L);
        lockOrder.verify(providerService).lockForModelSummary(10L);
        lockOrder.verify(providerService).lockForModelSummary(20L);
        lockOrder.verify(modelService).getByIdForUpdate(100L);
        verify(modelService).updateModel(request);
        verify(providerService).updateModelSummary(10L, "[\"old-model\"]", "old-model");
        verify(providerService).updateModelSummary(20L, "[\"new-model\"]", "new-model");
    }

    @Test
    void updateModelShouldSynchronizeProviderOnlyOnceWhenProviderIsUnchanged() {
        AiModelSaveDTO request = new AiModelSaveDTO();
        request.setId(100L);
        request.setProviderId(10L);

        AiModel persisted = model(100L, 10L);
        when(modelService.getByIdForUpdate(100L)).thenReturn(persisted);
        when(modelService.getById(100L)).thenReturn(persisted);
        when(modelService.getModelIdListByProviderId(10L)).thenReturn(List.of("model-a"));
        when(modelService.getDefaultModelId(10L)).thenReturn("model-a");

        manager(new ObjectMapper()).updateModel(request);

        verify(providerService).lockForModelSummary(10L);
        verify(providerService).updateModelSummary(10L, "[\"model-a\"]", "model-a");
    }

    @Test
    void deleteProviderShouldLockBeforeCheckingAndDeleting() {
        when(modelService.countByProviderId(10L)).thenReturn(0L);

        manager(new ObjectMapper()).deleteProvider(10L);

        InOrder order = inOrder(providerService, modelService);
        order.verify(providerService).lockForModelSummary(10L);
        order.verify(modelService).countByProviderId(10L);
        order.verify(providerService).deleteProvider(10L);
    }

    @Test
    void serializationFailureShouldAbortProviderSummaryUpdate() throws JsonProcessingException {
        ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        AiModelSaveDTO request = new AiModelSaveDTO();
        request.setProviderId(10L);
        List<String> modelIds = List.of("model-a");
        when(modelService.getModelIdListByProviderId(10L)).thenReturn(modelIds);
        when(modelService.getDefaultModelId(10L)).thenReturn("model-a");
        when(objectMapper.writeValueAsString(modelIds))
                .thenThrow(new JsonProcessingException("serialization failed") { });

        assertThrows(BusinessException.class, () -> manager(objectMapper).createModel(request));

        verify(providerService).lockForModelSummary(10L);
        verify(modelService).addModel(request);
        verify(providerService, never()).updateModelSummary(10L, null, "model-a");
    }

    private AiModelProviderManager manager(ObjectMapper objectMapper) {
        return new AiModelProviderManager(modelService, providerService, objectMapper);
    }

    private AiModel model(Long id, Long providerId) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setProviderId(providerId);
        return model;
    }
}
