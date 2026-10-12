package com.mdframe.forge.plugin.ai.agent.engine.persistence;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatMessageToolCall;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.enums.AiChatMessageStatus;
import com.mdframe.forge.plugin.ai.chat.service.AiChatMessageToolCallService;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentChatPersisterResumeTest {

    private final AiChatRecordService records = mock(AiChatRecordService.class);
    private final AiChatMessageToolCallService tools = mock(AiChatMessageToolCallService.class);
    private final AgentChatPersister persister = new AgentChatPersister(
            records, tools, mock(AiChatSessionService.class));
    private AiChatRecord assistant;
    private AiChatMessageToolCall completed;
    private AiChatMessageToolCall waiting;

    @BeforeEach
    void setUp() {
        assistant = AiChatRecord.builder().id(91L).sessionId("fixture-session").tenantId(7L).userId(31L)
                .content("before ").reasoning("thought ").status(AiChatMessageStatus.WAITING_CONFIRM.getCode()).build();
        completed = AiChatMessageToolCall.builder().seq(0).toolResultJson("completed-result")
                .status(AiChatMessageStatus.SUCCESS.getCode()).build();
        waiting = AiChatMessageToolCall.builder().seq(1)
                .status(AiChatMessageStatus.WAITING_CONFIRM.getCode()).build();
        when(records.findByInterruptId("fixture-interrupt")).thenReturn(assistant);
        when(tools.listByRecord(91L)).thenReturn(List.of(completed, waiting));
    }

    @Test
    void should_appendAndReuseWaitingRow_when_confirmationContinuesExistingMessage() {
        persister.openForResume("fixture-interrupt", true);
        event(AgentEventType.TOOL_RESULT_TEXT_DELTA, "{\"content\":\"confirmed-result\"}");
        event(AgentEventType.TOOL_RESULT_END, "{\"success\":true}");
        event(AgentEventType.TEXT_BLOCK_DELTA, "{\"text\":\"after\"}");
        event(AgentEventType.THINKING_BLOCK_DELTA, "{\"text\":\"next\"}");
        event(AgentEventType.TOOL_CALL_START, "{\"tool\":\"next_tool\",\"args\":{}}");
        event(AgentEventType.TOOL_RESULT_END, "{\"success\":true}");
        event(AgentEventType.AGENT_END, "{}");

        assertThat(assistant.getContent()).isEqualTo("before after");
        assertThat(assistant.getReasoning()).isEqualTo("thought next");
        assertThat(assistant.getStatus()).isEqualTo(AiChatMessageStatus.DONE.getCode());
        assertThat(completed.getToolResultJson()).isEqualTo("completed-result");
        assertThat(waiting.getToolResultJson()).isEqualTo("confirmed-result");
        assertThat(waiting.getStatus()).isEqualTo(AiChatMessageStatus.SUCCESS.getCode());
        ArgumentCaptor<AiChatMessageToolCall> appended = ArgumentCaptor.forClass(AiChatMessageToolCall.class);
        verify(tools).save(appended.capture());
        assertThat(appended.getValue().getSeq()).isEqualTo(2);
        verify(tools, never()).removeByIds(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void should_keepPreviousResultsAndAbortWaitingOnly_when_userRejects() {
        persister.openForResume("fixture-interrupt", false);

        assertThat(assistant.getContent()).isEqualTo("before ");
        assertThat(completed.getStatus()).isEqualTo(AiChatMessageStatus.SUCCESS.getCode());
        assertThat(waiting.getStatus()).isEqualTo(AiChatMessageStatus.ABORTED.getCode());
        assertThat(assistant.getStatus()).isEqualTo(AiChatMessageStatus.ABORTED.getCode());
        verify(tools, never()).save(any());
        verify(tools, never()).removeByIds(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void should_recordFailureInsteadOfSuccess_when_toolResultHasSuccessFalse() {
        persister.openForResume("fixture-interrupt", true);
        event(AgentEventType.TOOL_RESULT_END, "{\"success\":false}");
        assertThat(waiting.getStatus()).isEqualTo(AiChatMessageStatus.ERROR.getCode());
        assertThat(waiting.getErrorMsg()).isEqualTo("工具执行失败");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"{}", "null", "[]", "{\"success\":", "{\"success\":\"true\"}", "{\"success\":1}"})
    void should_notMarkSuccess_when_toolEndHasNoValidBooleanResult(String data) {
        persister.openForResume("fixture-interrupt", true);
        event(AgentEventType.TOOL_RESULT_END, data);
        assertThat(waiting.getStatus()).isEqualTo(AiChatMessageStatus.ERROR.getCode());
        assertThat(waiting.getErrorMsg()).isEqualTo("工具未返回有效成功结果");
        assertThat(completed.getStatus()).isEqualTo(AiChatMessageStatus.SUCCESS.getCode());
    }

    @Test
    void should_failUnfinishedTool_when_nextToolStartsWithoutPriorResult() {
        persister.openForResume("fixture-interrupt", true);
        event(AgentEventType.TOOL_CALL_START, "{\"tool\":\"next_tool\",\"args\":{}}");
        assertThat(waiting.getStatus()).isEqualTo(AiChatMessageStatus.ERROR.getCode());
        assertThat(waiting.getErrorMsg()).isEqualTo("工具调用未正常结束");
        assertThat(completed.getStatus()).isEqualTo(AiChatMessageStatus.SUCCESS.getCode());
        ArgumentCaptor<AiChatMessageToolCall> next = ArgumentCaptor.forClass(AiChatMessageToolCall.class);
        verify(tools).save(next.capture());
        assertThat(next.getValue().getStatus()).isEqualTo(AiChatMessageStatus.RUNNING.getCode());
    }

    private void event(AgentEventType type, String data) {
        persister.onEvent(AgentEvent.of("fixture-session", 0, type, data));
    }
}
