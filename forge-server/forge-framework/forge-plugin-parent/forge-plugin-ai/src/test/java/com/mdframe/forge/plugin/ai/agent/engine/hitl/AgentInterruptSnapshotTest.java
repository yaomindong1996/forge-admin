package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentInterruptSnapshotTest {

    @Test
    void should_preservePendingCursorTranscriptAndReasoning_when_jsonRoundTrips() {
        ReactContext original = context();
        original.setImageFileIds(List.of(101L));
        original.setFileUrlResolver(id -> "https://fixture.invalid/old-signed-url");
        original.setHistory(List.of(new UserMessage("earlier conversation")));
        original.beginToolTurn(AssistantMessage.builder().content("tool preamble")
                .properties(Map.of("reasoningContent", "thought\n\t\"fixture\"", "apiKey", "metadata-secret"))
                .toolCalls(List.of(call("first"), call("second"))).build());
        original.addToolResult(original.pendingToolCall(), "first-result");

        String serialized = JSON.toJSONString(AgentInterruptSnapshot.from(original, UUID.randomUUID().toString()));
        ReactContext restored = JSON.parseObject(serialized, AgentInterruptSnapshot.class).restore();

        assertThat(serialized).doesNotContain("old-signed-url", "metadata-secret", "chatModel", "boundTools");
        assertThat(restored.getChatModel()).isNull();
        assertThat(restored.getFileUrlResolver()).isNull();
        assertThat(restored.getBoundTools()).isEmpty();
        assertThat(restored.getImageFileIds()).containsExactly(101L);
        assertThat(restored.getPendingToolIndex()).isEqualTo(1);
        assertThat(restored.pendingToolCall().id()).isEqualTo("second");
        assertThat(restored.getTurnIndex()).isEqualTo(1);
        assertThat(restored.getMaxIters()).isEqualTo(3);
        assertThat(AgentMessageSnapshot.reasoning(restored.getToolMessages().get(0)))
                .isEqualTo("thought\n\t\"fixture\"");
        restored.addToolResult(restored.pendingToolCall(), "second-result");
        restored.completeToolTurn();
        assertThat(restored.getTurnIndex()).isEqualTo(2);
        assertThat(restored.getToolMessages()).hasSize(3);
    }

    @Test
    void should_rejectWrongVersionCursorOrTranscript_when_snapshotIsInvalid() {
        ReactContext context = context();
        context.beginToolTurn(AssistantMessage.builder().content("").toolCalls(List.of(call("pending"))).build());
        var snapshot = AgentInterruptSnapshot.from(context, UUID.randomUUID().toString());
        snapshot.setVersion(1);
        assertThatThrownBy(snapshot::restore).hasMessageContaining("无效");
        snapshot.setVersion(2);
        snapshot.setPendingToolIndex(-1);
        assertThatThrownBy(snapshot::restore).hasMessageContaining("无效");
        snapshot.setPendingToolIndex(1);
        assertThatThrownBy(snapshot::restore).hasMessageContaining("无效");
        snapshot.setPendingToolIndex(0);
        snapshot.setPendingToolCalls(List.of(AgentMessageSnapshot.Call.from(call("replaced"))));
        assertThatThrownBy(snapshot::restore).hasMessageContaining("不匹配");
    }

    @Test
    void should_rejectSignedUrlHistoryRatherThanLosingMedia_when_unknownHistoricalMediaExists() {
        var media = new Media(MimeTypeUtils.IMAGE_PNG, URI.create("https://fixture.invalid/signed-url"));
        assertThatThrownBy(() -> AgentMessageSnapshot.from(UserMessage.builder().text("image")
                .media(List.of(media)).build())).hasMessageContaining("媒体");
    }

    @Test
    void should_rejectDuplicateIdsBeforeAnyCall_when_oneModelTurnIsInvalid() {
        ReactContext context = context();
        assertThatThrownBy(() -> context.beginToolTurn(AssistantMessage.builder().content("")
                .toolCalls(List.of(call("duplicate"), call("duplicate"))).build()))
                .hasMessageContaining("重复");
        assertThat(context.getToolMessages()).isEmpty();
        assertThat(context.pendingToolCall()).isNull();
    }

    private AssistantMessage.ToolCall call(String id) {
        return new AssistantMessage.ToolCall(id, "function", "lookup", "{}");
    }

    private ReactContext context() {
        ReactContext context = new ReactContext();
        context.setAgentCode("fixture-agent");
        context.setAgentId(21L);
        context.setUserId(31L);
        context.setTenantId(7L);
        context.setSessionId("fixture-session");
        context.setTurnIndex(1);
        context.setMaxIters(3);
        return context;
    }
}
