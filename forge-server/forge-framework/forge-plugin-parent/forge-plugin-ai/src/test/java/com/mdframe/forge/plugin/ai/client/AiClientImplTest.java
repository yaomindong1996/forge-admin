package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.memory.DbChatMemory;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import com.mdframe.forge.plugin.ai.health.AiModelFailureClassifier;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiClientImplTest {

    @Mock
    private AiInvocationResolver invocationResolver;

    @Mock
    private DbChatMemory dbChatMemory;

    @Mock
    private AiChatRecordService recordService;

    @Mock
    private AiChatSessionService sessionService;

    @Mock
    private AiProviderAdapterRegistry adapterRegistry;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ContextInjector contextInjector;

    @Mock
    private AiModelInvocationRecorder invocationRecorder;

    private ChatClientCache chatClientCache;
    private AiClientImpl client;
    private AiProvider provider;

    @BeforeEach
    void setUp() {
        chatClientCache = spy(new ChatClientCache(adapterRegistry));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(chatClientCache).createSessionClient(any(), anyString(), any());
        client = new AiClientImpl(
                invocationResolver,
                dbChatMemory,
                recordService,
                sessionService,
                chatClientCache,
                contextInjector,
                invocationRecorder,
                new AiModelFailureClassifier()
        );
        provider = nativeProvider();
        when(invocationResolver.resolve(eq("copilot"), any(), any(), any(), any()))
                .thenReturn(resolvedInvocation(provider));
        when(contextInjector.injectContext(anyString(), eq("copilot")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adapterRegistry.createChatModel(any(AiProvider.class), any(AiModelRuntimeOptions.class)))
                .thenReturn(chatModel);
    }

    @Test
    void nativeProviderShouldUseUnifiedSynchronousCallChain() {
        when(chatModel.call(any(Prompt.class))).thenReturn(response("native answer", Map.of()));

        AiClientResponse result;
        try (MockedStatic<SessionHelper> session = mockSession()) {
            result = client.call(request());
        }

        assertFalse(result.isFallback());
        assertEquals("native answer", result.getContent());
        ArgumentCaptor<AiProvider> providerCaptor = ArgumentCaptor.forClass(AiProvider.class);
        ArgumentCaptor<AiModelRuntimeOptions> optionsCaptor =
                ArgumentCaptor.forClass(AiModelRuntimeOptions.class);
        verify(adapterRegistry).createChatModel(providerCaptor.capture(), optionsCaptor.capture());
        assertEquals(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode(),
                providerCaptor.getValue().getAdapterCode());
        assertEquals("qwen-plus", optionsCaptor.getValue().model());
        assertEquals(0.4D, optionsCaptor.getValue().temperature());
        assertEquals(512, optionsCaptor.getValue().maxTokens());
    }

    @Test
    void nativeProviderStreamShouldExposeAndPersistReasoningContent() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                response("", Map.of("reasoningContent", "thinking")),
                response("native answer", Map.of())
        ));

        List<String> chunks;
        try (MockedStatic<SessionHelper> session = mockSession()) {
            chunks = client.stream(request()).collectList().block();
        }

        String output = String.join("", chunks == null ? List.of() : chunks);
        assertTrue(output.contains("思考过程"));
        assertTrue(output.contains("thinking"));
        assertTrue(output.contains("native answer"));
        ArgumentCaptor<AiChatRecord> recordCaptor = ArgumentCaptor.forClass(AiChatRecord.class);
        verify(recordService, timeout(2000).atLeast(2)).save(recordCaptor.capture());
        AiChatRecord assistantRecord = recordCaptor.getAllValues().stream()
                .filter(record -> "assistant".equals(record.getRole()))
                .findFirst()
                .orElseThrow();
        assertTrue(assistantRecord.getContent().contains("【思考过程】\nthinking"));
        assertTrue(assistantRecord.getContent().contains("【回复内容】\nnative answer"));
    }

    private MockedStatic<SessionHelper> mockSession() {
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getUserId).thenReturn(100L);
        session.when(SessionHelper::getTenantId).thenReturn(1L);
        return session;
    }

    private AiClientRequest request() {
        AiClientRequest request = new AiClientRequest();
        request.setAgentCode("copilot");
        request.setMessage("hello");
        request.setSessionId("session-1");
        return request;
    }

    private AiInvocationResolver.ResolvedInvocation resolvedInvocation(AiProvider resolvedProvider) {
        AiAgent agent = new AiAgent();
        agent.setAgentCode("copilot");
        agent.setSystemPrompt("system prompt");
        agent.setProviderId(resolvedProvider.getId());
        agent.setModelName("qwen-plus");
        agent.setTemperature(new BigDecimal("0.4"));
        agent.setMaxTokens(512);
        return new AiInvocationResolver.ResolvedInvocation(
                agent, resolvedProvider, "qwen-plus", 0.4D, 512);
    }

    private AiProvider nativeProvider() {
        AiProvider nativeProvider = new AiProvider();
        nativeProvider.setId(10L);
        nativeProvider.setTenantId(1L);
        nativeProvider.setProviderName("DashScope Native");
        nativeProvider.setProviderType("alibaba");
        nativeProvider.setAdapterCode(AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode());
        return nativeProvider;
    }

    private ChatResponse response(String content, Map<String, Object> metadata) {
        AssistantMessage message = AssistantMessage.builder()
                .content(content)
                .properties(metadata)
                .build();
        return new ChatResponse(List.of(new Generation(message)));
    }
}
