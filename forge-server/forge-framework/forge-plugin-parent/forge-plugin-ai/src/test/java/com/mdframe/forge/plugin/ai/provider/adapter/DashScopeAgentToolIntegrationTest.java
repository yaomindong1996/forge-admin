package com.mdframe.forge.plugin.ai.provider.adapter;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactLoop;
import com.mdframe.forge.plugin.ai.agent.engine.lease.AgentLeaseFixture;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentLoopTaskExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionEngine;
import com.mdframe.forge.plugin.ai.agent.engine.service.AgentRuntimeFactory;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.provider.dashscope.DashScopeNativeChatModel;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 真实原生 SSE → ReAct 权限链 → 合成工具 → tool 消息 → 最终响应；只访问 loopback。 */
class DashScopeAgentToolIntegrationTest {

    private AiLoopbackFixture http;
    private AgentRedisFixture redis;
    private InterruptStore interrupts;
    private PermissionEngine permissions;
    private AgentEventPublisher publisher;
    private AgentRuntimeFactory runtime;
    private ThreadPoolTaskExecutor pool;
    private ReactAgent agent;
    private ChatModel model;
    private List<AgentTool> currentBindings;

    @BeforeEach
    void setUp() throws Exception {
        http = new AiLoopbackFixture();
        redis = new AgentRedisFixture();
        interrupts = new InterruptStore(redis.redis);
        permissions = mock(PermissionEngine.class);
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ALLOW);
        publisher = mock(AgentEventPublisher.class);
        runtime = mock(AgentRuntimeFactory.class);
        pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(2);
        pool.setQueueCapacity(8);
        pool.setThreadNamePrefix("fixture-agent-");
        pool.initialize();
        var executor = new AgentLoopTaskExecutor(pool, new AgentExecutionTaskDecorator());
        var toolExecutor = new AgentToolExecutor(permissions, interrupts, publisher);
        var loop = new ReactLoop(publisher, mock(AiModelInvocationRecorder.class), toolExecutor, executor);
        agent = new ReactAgent(loop, interrupts, runtime, publisher, new AgentLeaseFixture().registry());
        model = new DashScopeNativeChatModel(http.baseUrl(), "fixture-private-credential",
                ToolCallingChatOptions.builder().model("fixture-model").temperature(0.42).maxTokens(64).build());
        doAnswer(invocation -> {
            ReactContext context = invocation.getArgument(0);
            context.setChatModel(model);
            context.setBoundTools(currentBindings);
            context.setAgent(configuration());
            // 真实 factory 会重载当前权限模式；快照不保存运行时权限配置。
            context.setToolGroupMode("all");
            return null;
        }).when(runtime).restore(any(ReactContext.class));
    }

    @AfterEach
    void tearDown() {
        pool.shutdown();
        http.close();
        assertThat(LoginUserContextHolder.current()).isNull();
        TenantContextHolder.clear();
    }

    @Test
    void should_completeRoundTrip_when_nativeToolArgumentsArriveInFragments() {
        AtomicInteger calls = new AtomicInteger();
        ReactContext context = context(List.of(tool("lookup", calls)));
        http.respondOnce(200, frame("null", "call-1", "lookup", "{\"value\":")
                + frame("tool_calls", "call-1", "", "7}"), true);

        List<AgentEvent> events = run(context);

        assertThat(calls).hasValue(1);
        var first = http.request().body();
        assertThat(first.path("parameters").path("tools").get(0).path("function").path("name").asText())
                .isEqualTo("lookup");
        assertThat(first.path("parameters").path("temperature").asDouble()).isEqualTo(0.42);
        assertThat(first.path("parameters").path("max_tokens").asInt()).isEqualTo(64);
        var followUp = http.request().body().path("input").path("messages");
        assertThat(followUp.get(1).path("role").asText()).isEqualTo("assistant");
        assertThat(followUp.get(1).path("tool_calls").get(0).path("id").asText()).isEqualTo("call-1");
        assertThat(followUp.get(2).path("role").asText()).isEqualTo("tool");
        assertThat(followUp.get(2).path("tool_call_id").asText()).isEqualTo("call-1");
        assertThat(followUp.get(2).path("content").asText()).isEqualTo("fixture-result-7");
        assertThat(events).extracting(AgentEvent::getEventType)
                .contains(AgentEventType.TOOL_CALL_START, AgentEventType.TOOL_RESULT_END, AgentEventType.AGENT_END);
        assertThat(http.request()).isNull();
        assertThat(((ToolCallingChatOptions) model.getOptions()).getToolCallbacks()).isNullOrEmpty();
    }

    @Test
    void should_keepEveryCallId_when_oneRoundRequestsMultipleTools() {
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        ReactContext context = context(List.of(tool("lookup", first), tool("second", second)));
        http.respondOnce(200, twoTools("lookup", "second"), true);

        run(context);

        assertThat(first).hasValue(1);
        assertThat(second).hasValue(1);
        http.request();
        var messages = http.request().body().path("input").path("messages");
        assertThat(messages.get(1).path("tool_calls")).hasSize(2);
        assertThat(messages.get(2).path("tool_call_id").asText()).isEqualTo("call-1");
        assertThat(messages.get(3).path("tool_call_id").asText()).isEqualTo("call-2");
    }

    @Test
    void should_accumulateRealTranscript_when_nativeAgentUsesMultipleRounds() {
        AtomicInteger calls = new AtomicInteger();
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{\"value\":1}"), true);
        http.respondOnce(200, frame("tool_calls", "call-2", "lookup", "{\"value\":2}"), true);
        run(context(List.of(tool("lookup", calls))));
        assertThat(calls).hasValue(2);
        http.request();
        http.request();
        var third = http.request().body().path("input").path("messages");
        assertThat(third).hasSize(5);
        assertThat(third.get(2).path("tool_call_id").asText()).isEqualTo("call-1");
        assertThat(third.get(4).path("tool_call_id").asText()).isEqualTo("call-2");
        assertThat(third.get(4).path("content").asText()).isEqualTo("fixture-result-2");
    }

    @Test
    void should_keepIncrementalReasoningAndValidJson_when_toolResponseIncludesControlCharacters() {
        AtomicInteger calls = new AtomicInteger();
        String first = frame("null", "call-1", "lookup", "{\"value\":");
        String last = frame("tool_calls", "call-1", "", "1}");
        http.respondOnce(200, withReasoning(first, "first\n") + withReasoning(last, "second\t\"thought\""), true);
        List<AgentEvent> events = run(context(List.of(tool("lookup", calls))));
        http.request();
        var assistant = http.request().body().path("input").path("messages").get(1);
        assertThat(assistant.path("reasoning_content").asText()).isEqualTo("first\nsecond\t\"thought\"");
        assertThat(events).filteredOn(event -> event.getEventType() == AgentEventType.THINKING_BLOCK_DELTA)
                .extracting(event -> JSON.parseObject(event.getData()).getString("text"))
                .containsExactly("first\n", "second\t\"thought\"");
        assertThat(calls).hasValue(1);
    }

    @Test
    void should_preserveIdAndModelOptions_when_openAiUsesTheSameToolLoop() {
        useOpenAi();
        AtomicInteger calls = new AtomicInteger();
        http.respondOnce(200, AiLoopbackFixture.openAiToolFrame(0, "tool_calls", "oai-1", "lookup", "{}")
                + "data: [DONE]\n\n", true);
        run(context(List.of(tool("lookup", calls))));
        assertThat(calls).hasValue(1);
        var first = http.request().body();
        assertThat(first.path("tools").get(0).path("function").path("name").asText()).isEqualTo("lookup");
        assertThat(first.path("temperature").asDouble()).isEqualTo(0.42);
        assertThat(first.path("max_tokens").asInt()).isEqualTo(64);
        var messages = http.request().body().path("messages");
        assertThat(messages.get(1).path("tool_calls").get(0).path("id").asText()).isEqualTo("oai-1");
        assertThat(messages.get(2).path("role").asText()).isEqualTo("tool");
        assertThat(messages.get(2).path("tool_call_id").asText()).isEqualTo("oai-1");
        assertThat(http.request()).isNull();
    }

    @Test
    void should_notSendCachedForeignTools_when_currentAgentHasNoBindings() {
        ToolCallback foreign = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("foreign_default").description("fixture")
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String input) {
                throw new AssertionError("SDK 不得执行默认工具");
            }
        };
        for (boolean openAi : List.of(false, true)) {
            if (openAi) {
                useOpenAi();
            }
            var options = ((ToolCallingChatOptions) model.getOptions()).mutate()
                    .toolCallbacks(List.of(foreign)).build();
            model = openAi ? OpenAiChatModel.builder().options((OpenAiChatOptions) options).build()
                    : new DashScopeNativeChatModel(http.baseUrl(), "fixture-private-credential", options);
            run(context(List.of()));
            var request = http.request().body();
            assertThat((openAi ? request : request.path("parameters")).path("tools").size()).isZero();
            assertThat(((ToolCallingChatOptions) model.getOptions()).getToolCallbacks()).containsExactly(foreign);
        }
    }

    @Test
    void should_resumeWithoutReplayingModel_when_openAiRequestsConfirmation() {
        useOpenAi();
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, AiLoopbackFixture.openAiToolFrame(0, "tool_calls", "oai-1", "lookup", "{}")
                + "data: [DONE]\n\n", true);
        String id = interruptId(run(context(currentBindings)));
        http.request();
        resume(id, true);
        assertThat(calls).hasValue(1);
        assertThat(http.request().body().path("messages").get(2).path("tool_call_id").asText()).isEqualTo("oai-1");
        assertThat(http.request()).isNull();
    }

    @Test
    void should_notExecute_when_permissionDeniesTheBoundTool() {
        AtomicInteger calls = new AtomicInteger();
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.DENY);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);

        List<AgentEvent> events = run(context(List.of(tool("lookup", calls))));

        assertThat(calls).hasValue(0);
        assertThat(events).extracting(AgentEvent::getEventType).contains(AgentEventType.ALL_TOOLS_DENIED);
        http.request();
        assertThat(http.request().body().path("input").path("messages").get(2).path("role").asText())
                .isEqualTo("tool");
    }

    @Test
    void should_notFallBackToRegistry_when_modelRequestsAnUnboundTool() {
        AtomicInteger calls = new AtomicInteger();
        http.respondOnce(200, frame("tool_calls", "call-1", "not_bound", "{}"), true);

        List<AgentEvent> events = run(context(List.of(tool("lookup", calls))));

        assertThat(calls).hasValue(0);
        assertThat(events).extracting(AgentEvent::getEventType).contains(AgentEventType.ALL_TOOLS_DENIED);
    }

    @Test
    void should_rejectIncompleteOrDuplicateJson_when_argumentsAreInvalid() {
        for (String args : List.of("[]", "null", "{} {}", "{\"value\":1,\"value\":2}")) {
            AtomicInteger calls = new AtomicInteger();
            http.respondOnce(200, frame("tool_calls", "call-" + UUID.randomUUID(), "lookup", args), true);
            run(context(List.of(tool("lookup", calls))));
            assertThat(calls).hasValue(0);
            http.request();
            var toolMessage = http.request().body().path("input").path("messages").get(2);
            assertThat(toolMessage.path("content").asText()).contains("JSON");
        }
    }

    @Test
    void should_resumeOnlyThePendingTool_when_secondCallNeedsConfirmation() {
        AtomicInteger lookupCalls = new AtomicInteger();
        AtomicInteger submitCalls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", lookupCalls), tool("submit_fixture", submitCalls));
        when(permissions.decide(anyLong(), org.mockito.ArgumentMatchers.eq("submit_fixture"), anyString()))
                .thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, twoTools("lookup", "submit_fixture"), true);
        String interruptId = interruptId(run(context(currentBindings)));

        assertThat(lookupCalls).hasValue(1);
        assertThat(submitCalls).hasValue(0);
        String saved = redis.data.values().iterator().next();
        assertThat(saved).doesNotContain("fixture-private-credential", "chatModel", "boundTools", "permissions");
        assertThat(interrupts.get(interruptId).getContext().getPendingToolIndex()).isEqualTo(1);
        http.request();
        assertThat(http.request()).isNull();

        List<AgentEvent> resumed = resume(interruptId, true);

        assertThat(lookupCalls).hasValue(1);
        assertThat(submitCalls).hasValue(1);
        assertThat(redis.consumes).hasValue(1);
        assertThat(resumed).extracting(AgentEvent::getEventType).contains(AgentEventType.USER_CONFIRM_RESULT);
        var followUp = http.request().body().path("input").path("messages");
        assertThat(followUp.get(2).path("tool_call_id").asText()).isEqualTo("call-1");
        assertThat(followUp.get(3).path("tool_call_id").asText()).isEqualTo("call-2");
        assertThat(resume(interruptId, true)).extracting(AgentEvent::getData).anyMatch(data -> data.contains("过期"));
        assertThat(submitCalls).hasValue(1);
        assertThat(http.request()).isNull();
    }

    @Test
    void should_preserveInterrupt_when_anotherUserOrTenantAttemptsToResume() {
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        String interruptId = interruptId(run(context(currentBindings)));

        for (LoginUser foreign : List.of(user(99L, 7L), user(31L, 9L))) {
            try (var ignored = LoginUserContextHolder.open(foreign)) {
                assertThatThrownBy(() -> agent.resume(interruptId, true)).hasMessageContaining("无权");
            }
            assertThat(redis.data).hasSize(1);
        }
        resume(interruptId, true);
        assertThat(calls).hasValue(1);
    }

    @Test
    void should_notExecuteOrCallModel_when_userRejectsConfirmation() {
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        String id = interruptId(run(context(currentBindings)));
        http.request();

        assertThat(resume(id, false)).extracting(AgentEvent::getData).contains("{\"aborted\":true}");

        assertThat(calls).hasValue(0);
        assertThat(http.request()).isNull();
    }

    @Test
    void should_recheckBindings_when_configurationIsRevokedBeforeConfirmation() {
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        String id = interruptId(run(context(currentBindings)));
        currentBindings = List.of();

        List<AgentEvent> resumed = resume(id, true);

        assertThat(calls).hasValue(0);
        assertThat(resumed).extracting(AgentEvent::getEventType).contains(AgentEventType.ALL_TOOLS_DENIED);
    }

    @Test
    void should_notBypassNewDenial_when_userConfirmsPreviouslyAllowedRiskyTool() {
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        String id = interruptId(run(context(currentBindings)));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.DENY);
        assertThat(resume(id, true)).extracting(AgentEvent::getEventType).contains(AgentEventType.ALL_TOOLS_DENIED);
        assertThat(calls).hasValue(0);
    }

    @Test
    void should_requireSeparateConfirmationForEachRiskyCall_when_oneRoundHasTwoRiskyTools() {
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        currentBindings = List.of(tool("first", first), tool("second", second));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, twoTools("first", "second"), true);
        String firstId = interruptId(run(context(currentBindings)));
        String secondId = interruptId(resume(firstId, true));
        assertThat(first).hasValue(1);
        assertThat(second).hasValue(0);
        assertThat(secondId).isNotEqualTo(firstId);
        resume(secondId, true);
        assertThat(first).hasValue(1);
        assertThat(second).hasValue(1);
        assertThat(redis.consumes).hasValue(2);
        http.request();
        var messages = http.request().body().path("input").path("messages");
        assertThat(messages.get(2).path("tool_call_id").asText()).isEqualTo("call-1");
        assertThat(messages.get(3).path("tool_call_id").asText()).isEqualTo("call-2");
        assertThat(http.request()).isNull();
    }

    @Test
    void should_executeOnlyOnce_when_twoConfirmationsRaceForTheSameSnapshot() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        String id = interruptId(run(context(currentBindings)));
        // 恢复配置已在分布式占用之后；让两请求先读到同一快照，再竞争占用和 CAS。
        redis.readBarrier = new CountDownLatch(2);
        var requestPool = new ThreadPoolTaskExecutor();
        requestPool.setCorePoolSize(2);
        requestPool.setMaxPoolSize(2);
        requestPool.setQueueCapacity(2);
        requestPool.initialize();
        try {
            var first = requestPool.submit(() -> confirmOnce(id));
            var second = requestPool.submit(() -> confirmOnce(id));
            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(calls).hasValue(1);
            assertThat(redis.consumes).hasValue(1);
        } finally {
            requestPool.shutdown();
        }
    }

    @Test
    void should_stopBeforePendingTool_when_currentIterationLimitWasReduced() {
        AtomicInteger calls = new AtomicInteger();
        currentBindings = List.of(tool("lookup", calls));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenReturn(PermissionDecision.ASK);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        String id = interruptId(run(context(currentBindings)));
        doAnswer(invocation -> {
            ReactContext context = invocation.getArgument(0);
            context.setMaxIters(context.getTurnIndex());
            return null;
        }).when(runtime).restore(any(ReactContext.class));
        assertThat(resume(id, true)).extracting(AgentEvent::getEventType).contains(AgentEventType.EXCEED_MAX_ITERS);
        assertThat(calls).hasValue(0);
        http.request();
        assertThat(http.request()).isNull();
    }

    @Test
    void should_terminateReadWithoutExecution_when_ownerCancelsTheInFlightStream() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        http.pauseOnce(gate);
        AtomicInteger calls = new AtomicInteger();
        ReactContext context = context(List.of(tool("lookup", calls)));
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var result = agent.execute(context).collectList().toFuture();
            try {
                assertThat(http.awaitRequest()).isNotNull();
                try (var foreign = LoginUserContextHolder.open(user(99L, 7L))) {
                    assertThatThrownBy(() -> agent.cancel(context.getSessionId())).hasMessageContaining("无权");
                }
                agent.cancel(context.getSessionId());
                assertThat(result.get(2, TimeUnit.SECONDS)).extracting(AgentEvent::getData)
                        .contains("{\"aborted\":true}");
            } finally {
                gate.countDown();
            }
        }
        assertThat(calls).hasValue(0);
    }

    @Test
    void should_notOpenNewPersistence_when_anotherRequestCompetesForActiveSession() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        http.pauseOnce(gate);
        AtomicInteger firstOpen = new AtomicInteger();
        AtomicInteger competingOpen = new AtomicInteger();
        ReactContext context = context(List.of());
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var result = agent.execute(context, current -> firstOpen.incrementAndGet()).collectList().toFuture();
            try {
                assertThat(http.awaitRequest()).isNotNull();
                assertThatThrownBy(() -> agent.execute(context, current -> competingOpen.incrementAndGet()))
                        .hasMessageContaining("正在处理");
                assertThat(firstOpen).hasValue(1);
                assertThat(competingOpen).hasValue(0);
                agent.cancel(context.getSessionId());
                result.get(2, TimeUnit.SECONDS);
            } finally {
                gate.countDown();
            }
        }
    }

    @Test
    void should_releaseSessionSlot_when_preLoopCallbackFails() {
        ReactContext context = context(List.of());
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            assertThatThrownBy(() -> agent.execute(context, current -> {
                throw new IllegalStateException("fixture pre-loop error");
            })).hasMessage("fixture pre-loop error");
            assertThat(agent.execute(context).collectList().block(Duration.ofSeconds(8))).isNotEmpty();
        }
        assertThat(http.request()).isNotNull();
        assertThat(http.request()).isNull();
    }

    @Test
    void should_notExecutePartialCalls_when_nativeStreamEndsWithoutToolFinish() {
        AtomicInteger calls = new AtomicInteger();
        http.respondOnce(200, frame("null", "call-1", "lookup", "{\"value\":"), true);

        List<AgentEvent> events = run(context(List.of(tool("lookup", calls))));

        assertThat(calls).hasValue(0);
        assertThat(events).filteredOn(event -> event.getEventType() == AgentEventType.MODEL_CALL_END)
                .extracting(AgentEvent::getData).anyMatch(data -> data.contains("error"));
    }

    @Test
    void should_notExecuteEvenValidJson_when_streamStopsOnLengthOrHasNoCallId() {
        for (String finish : List.of("length", "stop")) {
            AtomicInteger calls = new AtomicInteger();
            http.respondOnce(200, frame(finish, "call-1", "lookup", "{}"), true);
            run(context(List.of(tool("lookup", calls))));
            assertThat(calls).hasValue(0);
            http.request();
            assertThat(http.request()).isNull();
        }
        AtomicInteger calls = new AtomicInteger();
        http.respondOnce(200, frame("tool_calls", "", "lookup", "{}"), true);
        run(context(List.of(tool("lookup", calls))));
        assertThat(calls).hasValue(0);
    }

    @Test
    void should_returnSafeErrorAndToolResponse_when_boundToolThrows() {
        AtomicInteger calls = new AtomicInteger();
        AgentTool failing = tool("lookup", calls);
        doThrow(new IllegalStateException("fixture-secret-diagnostic")).when(failing).execute(any(), any());
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        List<AgentEvent> events = run(context(List.of(failing)));
        assertThat(events).extracting(AgentEvent::getData).noneMatch(data ->
                data != null && data.contains("fixture-secret-diagnostic"));
        assertThat(events).filteredOn(event -> event.getEventType() == AgentEventType.TOOL_RESULT_END)
                .extracting(AgentEvent::getData).anyMatch(data -> data.contains("\"success\":false"));
        http.request();
        assertThat(http.request().body().path("input").path("messages").get(2).path("content").asText())
                .contains("工具执行失败").doesNotContain("fixture-secret-diagnostic");
    }

    @Test
    void should_notInvokeTool_when_cancelledAfterDeclarationBeforeExecution() {
        AtomicInteger calls = new AtomicInteger();
        ReactContext context = context(List.of(tool("lookup", calls)));
        when(permissions.decide(anyLong(), anyString(), anyString())).thenAnswer(invocation -> {
            context.setCancelled(true);
            return PermissionDecision.ALLOW;
        });
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);
        assertThat(run(context)).extracting(AgentEvent::getData).contains("{\"aborted\":true}");
        assertThat(calls).hasValue(0);
    }

    @Test
    void should_failClosed_when_aLaterModelRoundReusesAnAlreadyExecutedCallId() {
        AtomicInteger calls = new AtomicInteger();
        http.respondOnce(200, frame("tool_calls", "reused", "lookup", "{}"), true);
        http.respondOnce(200, frame("tool_calls", "reused", "lookup", "{}"), true);

        List<AgentEvent> events = run(context(List.of(tool("lookup", calls))));

        assertThat(calls).hasValue(1);
        assertThat(events).filteredOn(event -> event.getEventType() == AgentEventType.AGENT_END)
                .extracting(AgentEvent::getData).anyMatch(data -> data.contains("error"));
    }

    @Test
    void should_stopAtTheOriginalBudget_when_aToolTurnUsesTheLastIteration() {
        AtomicInteger calls = new AtomicInteger();
        ReactContext context = context(List.of(tool("lookup", calls)));
        context.setMaxIters(1);
        http.respondOnce(200, frame("tool_calls", "call-1", "lookup", "{}"), true);

        List<AgentEvent> events = run(context);

        assertThat(calls).hasValue(1);
        assertThat(events).extracting(AgentEvent::getEventType).contains(AgentEventType.EXCEED_MAX_ITERS);
        http.request();
        assertThat(http.request()).isNull();
    }

    private List<AgentEvent> run(ReactContext context) {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            return agent.execute(context).collectList().block(Duration.ofSeconds(8));
        }
    }

    private boolean confirmOnce(String id) {
        try {
            resume(id, true);
            return true;
        } catch (com.mdframe.forge.starter.core.exception.BusinessException ex) {
            assertThat(ex.getCode()).isEqualTo(409);
            return false;
        }
    }

    private void useOpenAi() {
        AiProvider provider = new AiProvider();
        provider.setAdapterCode("openai_compatible");
        provider.setApiKey("fixture-private-credential");
        provider.setBaseUrl(http.baseUrl() + "/v1");
        model = new OpenAiCompatibleProviderAdapter().createChatModel(provider,
                new AiModelRuntimeOptions("fixture-model", 0.42, 64));
    }

    private List<AgentEvent> resume(String id, boolean confirmed) {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            return agent.resume(id, confirmed).collectList().block(Duration.ofSeconds(8));
        }
    }

    private String interruptId(List<AgentEvent> events) {
        AgentEvent confirmation = events.stream()
                .filter(event -> event.getEventType() == AgentEventType.REQUIRE_USER_CONFIRM).findFirst().orElseThrow();
        return JSON.parseObject(confirmation.getData()).getString("interruptId");
    }

    private ReactContext context(List<AgentTool> tools) {
        ReactContext context = new ReactContext();
        context.setAgentCode("fixture-agent");
        context.setAgentId(21L);
        context.setTenantId(7L);
        context.setUserId(31L);
        context.setSessionId("fixture-" + UUID.randomUUID());
        context.setMaxIters(3);
        context.setUserMessage("fixture question");
        context.setToolGroupMode("all");
        context.setAgent(configuration());
        context.setChatModel(model);
        context.setBoundTools(tools);
        return context;
    }

    private AiAgent configuration() {
        AiAgent configuration = new AiAgent();
        configuration.setId(21L);
        configuration.setTenantId(7L);
        configuration.setAgentCode("fixture-agent");
        configuration.setKnowledgeIds("[501]");
        return configuration;
    }

    private AgentTool tool(String name, AtomicInteger calls) {
        AgentTool tool = mock(AgentTool.class);
        when(tool.getKey()).thenReturn(name);
        when(tool.getDescription()).thenReturn("fixture tool");
        when(tool.getParametersSchema()).thenReturn("{\"type\":\"object\",\"properties\":{}}");
        when(tool.execute(any(), any())).thenAnswer(invocation -> {
            assertThat(SessionHelper.getUserId()).isEqualTo(31L);
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
            calls.incrementAndGet();
            Map<String, Object> arguments = invocation.getArgument(0);
            return ToolResult.text("fixture-result-" + arguments.getOrDefault("value", "empty"));
        });
        return tool;
    }

    private LoginUser user(Long userId, Long tenantId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        return user;
    }

    private String frame(String finish, String id, String name, String args) {
        return AiLoopbackFixture.nativeToolFrame(finish, id, name, args);
    }

    private String withReasoning(String frame, String reasoning) {
        var json = JSON.parseObject(frame.substring("data: ".length()).trim());
        json.getJSONObject("output").getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").put("reasoning_content", reasoning);
        return "data: " + json.toJSONString() + "\n\n";
    }

    private String twoTools(String first, String second) {
        return AiLoopbackFixture.nativeToolFrame(0, "null", "call-1", first, "{}")
                + AiLoopbackFixture.nativeToolFrame(1, "tool_calls", "call-2", second, "{}");
    }
}
