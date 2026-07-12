package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.chat.memory.DbChatMemory;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import com.mdframe.forge.plugin.ai.health.AiModelFailureCategory;
import com.mdframe.forge.plugin.ai.health.AiModelFailureClassifier;
import com.mdframe.forge.plugin.ai.health.AiModelHealthLease;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationObservation;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.routing.RouteDecision;
import com.mdframe.forge.plugin.ai.routing.RoutedInvocation;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteReason;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteSource;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiClientRoutingGovernanceTest {

    @Mock private AiInvocationResolver invocationResolver;
    @Mock private DbChatMemory chatMemory;
    @Mock private AiChatRecordService recordService;
    @Mock private AiChatSessionService sessionService;
    @Mock private AiProviderAdapterRegistry adapterRegistry;
    @Mock private ChatModel chatModel;
    @Mock private ContextInjector contextInjector;
    @Mock private AiModelInvocationRecorder invocationRecorder;
    @Mock private AiModelHealthLease lease;

    private AiClientImpl client;

    @BeforeEach
    void setUp() {
        ChatClientCache cache = spy(new ChatClientCache(adapterRegistry));
        org.mockito.Mockito.lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(cache).createSessionClient(any(), anyString(), any());
        client = new AiClientImpl(invocationResolver, chatMemory, recordService,
                sessionService, cache, contextInjector, invocationRecorder,
                new AiModelFailureClassifier());
        when(invocationResolver.resolve(anyString(), any(), any(), any(), any()))
                .thenReturn(resolved());
        when(contextInjector.injectContext(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(adapterRegistry.createChatModel(any(), any())).thenReturn(chatModel);
    }

    @Test
    void synchronousFailureShouldCallModelAndAuditExactlyOnce() {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new IllegalStateException(new IOException("network failed")));

        AiClientResponse response;
        try (MockedStatic<SessionHelper> session = mockSession()) {
            response = client.call(request());
        }

        assertTrue(response.isFallback());
        verify(chatModel, times(1)).call(any(Prompt.class));
        verify(lease, times(1)).failure(AiModelFailureCategory.NETWORK);
        ArgumentCaptor<AiInvocationObservation> observation =
                ArgumentCaptor.forClass(AiInvocationObservation.class);
        verify(invocationRecorder, times(1)).record(observation.capture());
        assertTrue(observation.getValue().dispatched());
    }

    @Test
    void preparationFailureShouldAbortLeaseWithoutCallingModel() {
        when(adapterRegistry.createChatModel(any(), any()))
                .thenThrow(new IllegalStateException("adapter init failed"));

        AiClientResponse response;
        try (MockedStatic<SessionHelper> session = mockSession()) {
            response = client.call(request());
        }

        assertTrue(response.isFallback());
        verify(lease, times(1)).abort();
        verify(chatModel, times(0)).call(any(Prompt.class));
        verify(invocationRecorder, times(1)).record(any());
    }

    @Test
    void streamErrorShouldFailHealthAndAuditExactlyOnce() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.error(
                new IllegalStateException(new IOException("stream network failed"))));

        try (MockedStatic<SessionHelper> session = mockSession()) {
            assertThrows(IllegalStateException.class,
                    () -> client.stream(request()).blockLast());
        }

        verify(chatModel, times(1)).stream(any(Prompt.class));
        verify(lease, timeout(1000).times(1)).failure(AiModelFailureCategory.NETWORK);
        verify(invocationRecorder, timeout(1000).times(1)).record(any());
    }

    @Test
    void streamCancellationShouldCancelHealthAndAuditExactlyOnce() {
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.never());

        try (MockedStatic<SessionHelper> session = mockSession()) {
            Disposable subscription = client.stream(request()).subscribe();
            subscription.dispose();
        }

        verify(chatModel, times(1)).stream(any(Prompt.class));
        verify(lease, timeout(1000).times(1)).cancel();
        verify(invocationRecorder, timeout(1000).times(1)).record(any());
    }

    private AiInvocationResolver.ResolvedInvocation resolved() {
        AiAgent agent = new AiAgent();
        agent.setTenantId(1L);
        agent.setAgentCode("copilot");
        agent.setSystemPrompt("system");
        AiProvider provider = new AiProvider();
        provider.setId(10L);
        provider.setTenantId(1L);
        provider.setAdapterCode("openai_compatible");
        AiModel model = new AiModel();
        model.setId(20L);
        model.setTenantId(1L);
        model.setProviderId(10L);
        model.setModelId("test-model");
        RouteDecision decision = new RouteDecision(provider, model,
                AiModelRouteSource.PINNED, AiModelRouteReason.PINNED_MODEL,
                null, List.of());
        return new AiInvocationResolver.ResolvedInvocation(agent, provider,
                model.getModelId(), 0.7D, 256,
                new RoutedInvocation(decision, lease));
    }

    private AiClientRequest request() {
        AiClientRequest request = new AiClientRequest();
        request.setAgentCode("copilot");
        request.setSessionId("session-1");
        request.setMessage("hello");
        return request;
    }

    private MockedStatic<SessionHelper> mockSession() {
        MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
        session.when(SessionHelper::getTenantId).thenReturn(1L);
        session.when(SessionHelper::getUserId).thenReturn(2L);
        return session;
    }
}
