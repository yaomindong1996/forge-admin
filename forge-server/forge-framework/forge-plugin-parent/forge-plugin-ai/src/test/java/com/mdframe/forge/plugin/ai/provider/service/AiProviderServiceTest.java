package com.mdframe.forge.plugin.ai.provider.service;

import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderSaveDTO;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderTestDTO;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderCacheEvictionScheduler;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderServiceTest {

    private static final String REAL_SECRET = "sk-real-sensitive-7890";

    @Mock
    private AiProviderMapper providerMapper;

    @Mock
    private AiProviderAdapterRegistry adapterRegistry;

    @Mock
    private AiProviderCacheEvictionScheduler evictionScheduler;

    @Mock
    private ChatModel chatModel;

    private AiProviderService service;

    @BeforeEach
    void setUp() {
        service = new AiProviderService(adapterRegistry, evictionScheduler);
        ReflectionTestUtils.setField(service, "baseMapper", providerMapper);
    }

    @Test
    void createShouldDefaultMissingAdapterToCompatible() {
        when(providerMapper.insert(any(AiProvider.class))).thenReturn(1);
        AiProviderSaveDTO request = completeSaveRequest();
        request.setAdapterCode(null);

        service.createProvider(request);

        ArgumentCaptor<AiProvider> captor = ArgumentCaptor.forClass(AiProvider.class);
        verify(providerMapper).insert(captor.capture());
        assertEquals(AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode(), captor.getValue().getAdapterCode());
    }

    @Test
    void createShouldRejectExplicitBlankAdapter() {
        AiProviderSaveDTO request = completeSaveRequest();
        request.setAdapterCode("   ");

        assertThrows(BusinessException.class, () -> service.createProvider(request));

        verify(providerMapper, never()).insert(any(AiProvider.class));
    }

    @Test
    void updateShouldPreserveAdapterAndSecretWhenMaskIsSubmitted() {
        AiProvider persisted = persistedProvider();
        when(providerMapper.selectById(10L)).thenReturn(persisted);
        when(providerMapper.updateById(any(AiProvider.class))).thenReturn(1);
        AiProviderSaveDTO request = completeSaveRequest();
        request.setId(10L);
        request.setAdapterCode(null);
        request.setApiKey("sk-r****7890");
        request.setBaseUrl("https://dashscope.aliyuncs.com");

        service.updateProvider(request);

        ArgumentCaptor<AiProvider> captor = ArgumentCaptor.forClass(AiProvider.class);
        verify(providerMapper).updateById(captor.capture());
        assertEquals(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode(), captor.getValue().getAdapterCode());
        assertEquals(REAL_SECRET, captor.getValue().getApiKey());
        verify(evictionScheduler).scheduleAfterCommit(captor.getValue());
    }

    @Test
    void updateShouldReplaceSecretWhenPlaintextIsSubmitted() {
        AiProvider persisted = persistedProvider();
        when(providerMapper.selectById(10L)).thenReturn(persisted);
        when(providerMapper.updateById(any(AiProvider.class))).thenReturn(1);
        AiProviderSaveDTO request = completeSaveRequest();
        request.setId(10L);
        request.setAdapterCode(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode());
        request.setApiKey("sk-new-secret-1234");
        request.setBaseUrl("https://dashscope.aliyuncs.com");

        service.updateProvider(request);

        ArgumentCaptor<AiProvider> captor = ArgumentCaptor.forClass(AiProvider.class);
        verify(providerMapper).updateById(captor.capture());
        assertEquals("sk-new-secret-1234", captor.getValue().getApiKey());
    }

    @Test
    void updateShouldRejectExplicitBlankAdapter() {
        when(providerMapper.selectById(10L)).thenReturn(persistedProvider());
        AiProviderSaveDTO request = completeSaveRequest();
        request.setId(10L);
        request.setAdapterCode("");

        assertThrows(BusinessException.class, () -> service.updateProvider(request));

        verify(providerMapper, never()).updateById(any(AiProvider.class));
        verify(evictionScheduler, never()).scheduleAfterCommit(any());
    }

    @Test
    void updateFailureShouldNotScheduleEviction() {
        when(providerMapper.selectById(10L)).thenReturn(persistedProvider());
        when(providerMapper.updateById(any(AiProvider.class))).thenReturn(0);
        AiProviderSaveDTO request = completeSaveRequest();
        request.setId(10L);
        request.setAdapterCode(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode());
        request.setBaseUrl("https://dashscope.aliyuncs.com");

        assertThrows(BusinessException.class, () -> service.updateProvider(request));

        verify(evictionScheduler, never()).scheduleAfterCommit(any());
    }

    @Test
    void savedConnectionTestShouldLoadRealSecretById() {
        AiProvider persisted = persistedProvider();
        when(providerMapper.selectById(10L)).thenReturn(persisted);
        when(adapterRegistry.createChatModel(any(AiProvider.class), any(AiModelRuntimeOptions.class)))
                .thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(response("OK", "thinking"));
        AiProviderTestDTO request = new AiProviderTestDTO();
        request.setId(10L);

        String result = service.testConnection(request);

        ArgumentCaptor<AiProvider> providerCaptor = ArgumentCaptor.forClass(AiProvider.class);
        verify(adapterRegistry).createChatModel(providerCaptor.capture(), any(AiModelRuntimeOptions.class));
        assertEquals(REAL_SECRET, providerCaptor.getValue().getApiKey());
        assertTrue(result.contains("OK"));
        assertTrue(result.contains("thinking"));
    }

    @Test
    void unsavedConnectionTestShouldUseOnlyInlineConfiguration() {
        when(adapterRegistry.createChatModel(any(AiProvider.class), any(AiModelRuntimeOptions.class)))
                .thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class))).thenReturn(response("OK", ""));
        AiProviderTestDTO request = new AiProviderTestDTO();
        request.setProviderName("Inline DashScope");
        request.setProviderType("alibaba");
        request.setAdapterCode(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode());
        request.setApiKey("sk-inline-secret");
        request.setBaseUrl("https://dashscope.aliyuncs.com");
        request.setDefaultModel("qwen-plus");

        String result = service.testConnection(request);

        verify(providerMapper, never()).selectById(any());
        assertTrue(result.contains("OK"));
    }

    @Test
    void mixedSavedAndInlineConnectionTestShouldFailBeforeModelCreation() {
        AiProviderTestDTO request = new AiProviderTestDTO();
        request.setId(10L);
        request.setApiKey("sk-inline-secret");

        assertThrows(BusinessException.class, () -> service.testConnection(request));

        verify(adapterRegistry, never()).createChatModel(any(), any());
        verify(providerMapper, never()).selectById(any());
    }

    @Test
    void sdkFailureShouldNotExposeSensitiveMessage() {
        AiProvider persisted = persistedProvider();
        when(providerMapper.selectById(10L)).thenReturn(persisted);
        when(adapterRegistry.createChatModel(any(), any())).thenReturn(chatModel);
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new IllegalStateException("request failed with key " + REAL_SECRET));
        AiProviderTestDTO request = new AiProviderTestDTO();
        request.setId(10L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.testConnection(request));

        assertFalse(exception.getMessage().contains(REAL_SECRET));
        assertEquals("连接失败，请检查供应商配置和网络状态", exception.getMessage());
    }

    @Test
    void deleteShouldEvictLoadedProviderAfterPersistenceSucceeds() {
        AiProvider persisted = persistedProvider();
        when(providerMapper.selectById(10L)).thenReturn(persisted);
        when(providerMapper.deleteById(10L)).thenReturn(1);

        service.deleteProvider(10L);

        verify(evictionScheduler).scheduleAfterCommit(persisted);
    }

    @Test
    void safeViewShouldContainMaskedSecretOnly() {
        AiProvider persisted = persistedProvider();

        String masked = service.toSafeView(persisted).getApiKey();

        assertEquals("sk-r****7890", masked);
        assertFalse(masked.contains("sensitive"));
    }

    private AiProviderSaveDTO completeSaveRequest() {
        AiProviderSaveDTO request = new AiProviderSaveDTO();
        request.setProviderName("DashScope");
        request.setProviderType("alibaba");
        request.setAdapterCode(AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode());
        request.setApiKey(REAL_SECRET);
        request.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode");
        request.setDefaultModel("qwen-plus");
        request.setStatus("0");
        request.setIsDefault("0");
        return request;
    }

    private AiProvider persistedProvider() {
        AiProvider provider = new AiProvider();
        provider.setId(10L);
        provider.setTenantId(1L);
        provider.setProviderName("DashScope");
        provider.setProviderType("alibaba");
        provider.setAdapterCode(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode());
        provider.setApiKey(REAL_SECRET);
        provider.setBaseUrl("https://dashscope.aliyuncs.com");
        provider.setDefaultModel("qwen-plus");
        provider.setStatus("0");
        provider.setIsDefault("0");
        return provider;
    }

    private ChatResponse response(String content, String reasoningContent) {
        AssistantMessage message = AssistantMessage.builder()
                .content(content)
                .properties(Map.<String, Object>of("reasoningContent", reasoningContent))
                .build();
        Generation generation = new Generation(message);
        return new ChatResponse(java.util.List.of(generation));
    }
}
