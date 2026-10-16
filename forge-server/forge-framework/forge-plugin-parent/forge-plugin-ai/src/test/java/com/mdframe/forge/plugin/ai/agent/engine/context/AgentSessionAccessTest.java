package com.mdframe.forge.plugin.ai.agent.engine.context;

import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentSessionAccessTest {
    private final AiChatSessionService sessions = mock(AiChatSessionService.class);
    private final AgentSessionAccess access = new AgentSessionAccess(sessions);

    @Test
    void requiresLoginBeforeLookingUpSession() {
        assertThatThrownBy(() -> access.requireOwned("fixture")).hasMessageContaining("登录");
        verifyNoInteractions(sessions);
    }

    @Test
    void refusesForeignUsersTenantsMissingAndDeletedSessions() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            for (AiChatSession session : new AiChatSession[]{owned(32L, 7L, "0"),
                    owned(31L, 9L, "0"), owned(31L, 7L, "1"), null}) {
                when(sessions.getById("fixture")).thenReturn(session);
                assertThatThrownBy(() -> access.requireOwned("fixture")).hasMessageContaining("无权");
            }
        }
    }

    @Test
    void bindsSubscriptionKeyAndCreationToTrustedCaller() {
        var session = owned(31L, 7L, "0");
        when(sessions.getById("fixture")).thenReturn(session);
        when(sessions.getOrCreate("fixture", 31L, 7L, "agent", "first message")).thenReturn(session);
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            assertThat(access.requireOwned("fixture"))
                    .isEqualTo(new AgentSessionAccess.SessionKey("fixture", 7L, 31L));
            access.prepare("fixture", "agent", "first message");
        }
        verify(sessions).getOrCreate("fixture", 31L, 7L, "agent", "first message");
    }

    @Test
    void invalidIdsNeverAccessStorage() {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            for (String id : new String[]{null, " ", "x".repeat(65)}) {
                assertThatThrownBy(() -> access.requireOwned(id)).hasMessageContaining("会话标识");
            }
        }
        verifyNoInteractions(sessions);
    }

    @Test
    void duplicateGlobalIdNeverReclaimsForeignRowOrLeaksDatabaseDetails() {
        var service = spy(new AiChatSessionService(mock(AiChatRecordService.class)));
        doReturn(null).when(service).getById("fixture");
        doThrow(new DuplicateKeyException("fixture-sensitive-db-diagnostic")).when(service).save(any());
        assertThatThrownBy(() -> service.getOrCreate("fixture", 31L, 7L, "agent", "title"))
                .hasMessageContaining("已被占用").hasMessageNotContaining("fixture-sensitive-db-diagnostic");
        verify(service, times(1)).getById("fixture");
        verify(service, never()).updateById(any());
    }

    @Test
    void existingForeignOrDeletedSessionCannotBeTouched() {
        var service = spy(new AiChatSessionService(mock(AiChatRecordService.class)));
        for (AiChatSession session : new AiChatSession[]{owned(32L, 7L, "0"),
                owned(31L, 9L, "0"), owned(31L, 7L, "1")}) {
            doReturn(session).when(service).getById("fixture");
            assertThatThrownBy(() -> service.getOrCreate("fixture", 31L, 7L, "agent", "title"))
                    .hasMessageContaining("无权");
        }
        verify(service, never()).save(any());
        verify(service, never()).update(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test
    void newOwnedSessionKeepsFirstMessageTitle() {
        var service = spy(new AiChatSessionService(mock(AiChatRecordService.class)));
        doReturn(null).when(service).getById("fixture");
        doReturn(true).when(service).save(any());
        var session = service.getOrCreate("fixture", 31L, 7L, "agent", "first message");
        assertThat(session.getSessionName()).isEqualTo("first message");
        assertThat(session.getUserId()).isEqualTo(31L);
        assertThat(session.getTenantId()).isEqualTo(7L);
    }

    private static AiChatSession owned(Long userId, Long tenantId, String status) {
        return AiChatSession.builder().id("fixture").userId(userId).tenantId(tenantId).status(status).build();
    }

    private static LoginUser user(Long userId, Long tenantId) {
        var user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        return user;
    }
}
