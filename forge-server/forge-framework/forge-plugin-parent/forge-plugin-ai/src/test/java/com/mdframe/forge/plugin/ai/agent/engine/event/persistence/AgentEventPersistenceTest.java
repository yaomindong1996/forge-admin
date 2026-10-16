package com.mdframe.forge.plugin.ai.agent.engine.event.persistence;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.event.mapper.AgentEventMapper;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentEventPersistenceTest {
    private final AgentEventMapper mapper = mock(AgentEventMapper.class);
    private final AtomicLong ids = new AtomicLong(9007199254740992L);
    private final IdentifierGenerator generator = entity -> ids.incrementAndGet();
    private final AgentEventPersistence persistence = new AgentEventPersistence(mapper, generator);
    private final List<AgentEventEntity> stored = new ArrayList<>();

    @BeforeEach
    void setup() {
        when(mapper.insertBatch(any())).thenAnswer(call -> {
            List<AgentEventEntity> batch = call.getArgument(0);
            stored.addAll(batch);
            return batch.size();
        });
    }

    @AfterEach
    void cleanup() {
        persistence.flush();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Test
    void should_useTrustedSynchronousIdentityAndFrameworkIds_when_shortRunEnds() throws Exception {
        assertThat(AgentEventPersistence.class.getMethod("persist", AgentEvent.class)
                .isAnnotationPresent(Async.class)).isFalse();
        assertThat(AgentEventPersistence.class.isAnnotationPresent(Async.class)).isFalse();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            emit("short", AgentEventType.AGENT_START, "{}");
            emit("short", AgentEventType.TEXT_BLOCK_DELTA, "{\"text\":\"合成内容\"}");
            verifyNoInteractions(mapper);
            emit("short", AgentEventType.AGENT_END, "{}");
            persistence.flush();
        }
        verify(mapper, times(1)).insertBatch(any());
        assertThat(stored).extracting(AgentEventEntity::getEventType)
                .containsExactly("AGENT_START", "TEXT_BLOCK_DELTA", "AGENT_END");
        assertThat(stored).extracting(AgentEventEntity::getId)
                .containsExactly(9007199254740993L, 9007199254740994L, 9007199254740995L);
        assertThat(stored).allSatisfy(row -> {
            assertThat(row.getTenantId()).isEqualTo(7L);
            assertThat(row.getCreateTime()).isNotNull();
            assertThat(row.getSessionId()).isEqualTo("short");
        });
    }

    @Test
    void should_flushAtBoundAndKeepOrder_when_streamExceedsOneBatch() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            for (int index = 0; index < 20; index++) {
                emit("long", AgentEventType.TEXT_BLOCK_DELTA, String.valueOf(index));
            }
            assertThat(stored).hasSize(20);
            emit("long", AgentEventType.TEXT_BLOCK_DELTA, "20");
            assertThat(stored).hasSize(20);
            persistence.flush();
            persistence.flush();
        }
        assertThat(stored).extracting(AgentEventEntity::getEventData)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 21).mapToObj(String::valueOf).toList());
        verify(mapper, times(2)).insertBatch(any());
    }

    @Test
    void should_flushWaitingTerminalWithoutReplaying_when_confirmationPausesRun() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            emit("confirm", AgentEventType.TOOL_CALL_START, "{}");
            emit("confirm", AgentEventType.REQUIRE_USER_CONFIRM, "{\"interruptId\":\"synthetic\"}");
            assertThat(stored).hasSize(2);
            persistence.flush();
        }
        verify(mapper, times(1)).insertBatch(any());
    }

    @Test
    void should_flushOldSessionBeforeNewOne_when_sameTrustedOwnerReusesThread() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            emit("first", AgentEventType.TEXT_BLOCK_DELTA, "first");
            emit("second", AgentEventType.TEXT_BLOCK_DELTA, "second");
            assertThat(stored).extracting(AgentEventEntity::getSessionId).containsExactly("first");
            persistence.flush();
        }
        assertThat(stored).extracting(AgentEventEntity::getSessionId).containsExactly("first", "second");
        verify(mapper, times(2)).insertBatch(any());
    }

    @Test
    void should_refuseMissingIdentityBeforeBuffering() {
        assertThatThrownBy(() -> emit("missing", AgentEventType.AGENT_START, "{}"))
                .hasMessageContaining("登录");
        verifyNoInteractions(mapper);
    }

    @Test
    void should_refuseForeignUserOrTenant_withoutFlushingOwnedBufferUnderNewIdentity() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            emit("owned", AgentEventType.AGENT_START, "{}");
            for (LoginUser other : List.of(user(32L, 7L), user(31L, 9L))) {
                try (var foreign = LoginUserContextHolder.open(other)) {
                    assertThatThrownBy(() -> emit("foreign", AgentEventType.AGENT_START, "{}"))
                            .hasMessageContaining("身份发生变化");
                }
            }
            verifyNoInteractions(mapper);
            persistence.flush();
        }
        assertThat(stored).hasSize(1).allMatch(row -> row.getTenantId().equals(7L));
    }

    @Test
    void should_removeUnflushableBuffer_when_ownerContextIsAlreadyGone() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            emit("old", AgentEventType.AGENT_START, "{}");
        }
        assertThatThrownBy(persistence::flush).hasMessageContaining("登录");
        try (var ignored = LoginUserContextHolder.open(user(41L, 9L))) {
            emit("new", AgentEventType.AGENT_END, "{}");
        }
        assertThat(stored).hasSize(1).allMatch(row -> row.getTenantId().equals(9L));
    }

    @Test
    void should_clearFailedBatchWithoutAutoReplay_when_databaseWriteFails() {
        doThrow(new IllegalStateException("synthetic-sensitive-sql-body"))
                .doAnswer(call -> {
                    List<AgentEventEntity> batch = call.getArgument(0);
                    stored.addAll(batch);
                    return batch.size();
                }).when(mapper).insertBatch(any());
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            emit("failed", AgentEventType.AGENT_END, "{}");
            persistence.flush();
        }
        try (var ignored = LoginUserContextHolder.open(user(41L, 9L))) {
            emit("new", AgentEventType.AGENT_END, "{}");
        }
        verify(mapper, times(2)).insertBatch(any());
        assertThat(stored).hasSize(1).allMatch(row -> row.getSessionId().equals("new"));
    }

    @Test
    void should_refuseMalformedEvents_beforeStorage() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            assertThatThrownBy(() -> persistence.persist(null)).hasMessageContaining("标识无效");
            assertThatThrownBy(() -> emit(" ", AgentEventType.AGENT_END, "{}")).hasMessageContaining("标识无效");
            assertThatThrownBy(() -> emit("x".repeat(65), AgentEventType.AGENT_END, "{}"))
                    .hasMessageContaining("标识无效");
            assertThatThrownBy(() -> emit("valid", null, "{}")).hasMessageContaining("标识无效");
        }
        verifyNoInteractions(mapper);
    }

    private void emit(String sessionId, AgentEventType type, String data) {
        persistence.persist(AgentEvent.of(sessionId, 0, type, data));
    }

    static LoginUser user(Long userId, Long tenantId) {
        var user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        return user;
    }
}
