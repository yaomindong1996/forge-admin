package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AgentInterruptStoreTest {

    private final AgentRedisFixture redis = new AgentRedisFixture();
    private final InterruptStore store = new InterruptStore(redis.redis);

    @Test
    void should_storeOnlyV2AndConsumeOnce_when_snapshotIsValid() {
        String id = store.save(context());
        assertThat(redis.data.keySet()).containsExactly("agent:interrupt:v2:" + id);
        var state = store.get(id);
        assertThat(store.consume(id, state)).isTrue();
        assertThat(store.consume(id, state)).isFalse();
        assertThat(store.get(id)).isNull();
        assertThat(redis.consumes).hasValue(1);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_compareExactSnapshotWithRedis6CompatibleScript_when_claimingConfirmation() {
        String id = store.save(context());
        var state = store.get(id);
        redis.data.put("agent:interrupt:v2:" + id, state.rawValue() + " ");
        assertThat(store.consume(id, state)).isFalse();
        assertThat(redis.data).hasSize(1);
        ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass((Class) RedisScript.class);
        verify(redis.redis).execute(script.capture(), anyList(), anyString());
        assertThat(script.getValue().getScriptAsString()).contains("'GET'", "ARGV[1]", "'DEL'")
                .doesNotContain("GETDEL");
    }

    @Test
    void should_notReadArbitraryKeys_when_interruptIdIsNotCanonicalUuid() {
        assertThat(store.get("arbitrary-redis-key")).isNull();
        assertThat(store.get(null)).isNull();
        verifyNoInteractions(redis.redis);
    }

    @Test
    void should_preserveCorruptDataAndReturnSafeError_when_snapshotCannotBeRestored() {
        String id = store.save(context());
        redis.data.put("agent:interrupt:v2:" + id, "{\"version\":1,\"diagnostic\":\"fixture-secret\"}");
        assertThatThrownBy(() -> store.get(id)).hasMessageContaining("无效").hasMessageNotContaining("fixture-secret");
        assertThat(redis.consumes).hasValue(0);
        assertThat(redis.data).hasSize(1);
    }

    private ReactContext context() {
        ReactContext context = new ReactContext();
        context.setAgentCode("fixture-agent");
        context.setAgentId(21L);
        context.setTenantId(7L);
        context.setUserId(31L);
        context.setSessionId("fixture-session");
        context.setMaxIters(3);
        context.beginToolTurn(AssistantMessage.builder().content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "lookup", "{}"))).build());
        return context;
    }
}
