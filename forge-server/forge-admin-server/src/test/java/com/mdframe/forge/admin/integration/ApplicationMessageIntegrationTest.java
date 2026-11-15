package com.mdframe.forge.admin.integration;

import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessNode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessMessageChannel;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessMessageChannelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessMessageChannelService;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessMessageExecutor;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationMessageIntegrationTest {
    BusinessMessageChannelMapper mapper = mock(BusinessMessageChannelMapper.class);
    BusinessMessageChannelService channels = new BusinessMessageChannelService(mapper);
    MessageService messages = mock(MessageService.class);
    BusinessProcessMapper processMapper = mock(BusinessProcessMapper.class);
    BusinessProcessMessageExecutor executor = new BusinessProcessMessageExecutor(channels, messages, processMapper);
    MockedStatic<SessionHelper> session;
    AiBusinessProcessRun run = new AiBusinessProcessRun();
    BusinessProcessNode node = new BusinessProcessNode();
    @BeforeEach void setup() {
        session = mockStatic(SessionHelper.class); session.when(SessionHelper::getTenantId).thenReturn(7L);
        run.setId(9L); run.setApplicationId(11L); run.setTenantId(7L); run.setActorUserId(8L);
        node.setId("notice"); node.setConfig(Map.of("messageTemplateCode", "legal_notice", "useApplicationCollaboration", true));
        when(messages.resolveEnabledTemplateCode("legal_notice")).thenReturn("legal_notice");
    }
    @AfterEach void close() { session.close(); com.mdframe.forge.starter.tenant.context.TenantContextHolder.clear(); }
    AiBusinessMessageChannel channel(int enabled, Long tenant, String ref) {
        AiBusinessMessageChannel c = new AiBusinessMessageChannel(); c.setTenantId(tenant); c.setChannelCode("app_11_collaboration");
        c.setChannelType("COLLABORATION"); c.setStatus(enabled); c.setChannelConfigRef(ref); return c;
    }
    @Test void missingBindingDoesNotFallBackToWeb() {
        assertThrows(BusinessException.class, () -> executor.execute(run, node)); verify(messages, never()).send(any());
    }
    @Test void disabledOrCrossTenantOrInvalidReferenceFailsClosed() {
        for (var c : new AiBusinessMessageChannel[] {channel(0, 7L, "22"), channel(1, 99L, "22"), channel(1, 7L, null)}) {
            when(mapper.selectByChannelCode(7L, "app_11_collaboration")).thenReturn(c);
            assertThrows(BusinessException.class, () -> executor.execute(run, node));
        }
        verify(messages, never()).send(any());
    }
    @Test void usesPersistedConnectionAndStableNodeIdempotencyKey() {
        when(mapper.selectByChannelCode(7L, "app_11_collaboration")).thenReturn(channel(1, 7L, "22"));
        executor.execute(run, node);
        verify(messages).send(argThat((MessageSendRequestDTO r) -> r.getConnectionId().equals(22L)
                && r.getChannel().equals("COLLABORATION") && r.getUserIds().equals(java.util.Set.of(8L))
                && r.getIdempotencyKey().equals("business-process:9:notice")));
    }
    @Test void legacyNodeUsesWebAndDoesNotReadEnterpriseChannel() {
        node.setConfig(Map.of("messageTemplateCode", "legal_notice")); executor.execute(run, node);
        verifyNoInteractions(mapper);
        verify(messages).send(argThat((MessageSendRequestDTO r) -> r.getChannel().equals("WEB") && r.getConnectionId() == null));
    }
    @Test void rejectsMissingActorAndCrossTenantRun() {
        run.setActorUserId(null); assertThrows(BusinessException.class, () -> executor.execute(run, node));
        run.setActorUserId(8L); run.setTenantId(88L); assertThrows(BusinessException.class, () -> executor.execute(run, node));
        verifyNoInteractions(messages, mapper);
    }
    @Test void approvalCallbackWorksInsideVerifiedTenantScopeWithoutInteractiveSession() {
        session.when(SessionHelper::getTenantId).thenReturn(null);
        com.mdframe.forge.starter.tenant.context.TenantContextHolder.setTenantId(7L);
        when(mapper.selectByChannelCode(7L, "app_11_collaboration")).thenReturn(channel(1, 7L, "22"));
        assertDoesNotThrow(() -> executor.execute(run, node));
        verify(messages).send(any());
    }
    @Test void missingOrIgnoredTenantNeverFallsBackToDefaultTenant() {
        session.when(SessionHelper::getTenantId).thenReturn(null);
        assertThrows(BusinessException.class, () -> executor.execute(run, node));
        com.mdframe.forge.starter.tenant.context.TenantContextHolder.setTenantId(7L);
        com.mdframe.forge.starter.tenant.context.TenantContextHolder.setIgnore(true);
        assertThrows(BusinessException.class, () -> executor.execute(run, node));
        verifyNoInteractions(messages, mapper);
    }
    @Test void disabledTemplateCannotCreateAnEmptyWebMessage() {
        node.setConfig(Map.of("messageTemplateCode", "legal_notice"));
        when(messages.resolveEnabledTemplateCode("legal_notice")).thenReturn(null);
        BusinessException error = assertThrows(BusinessException.class, () -> executor.execute(run, node));
        assertTrue(error.getMessage().contains("模板"));
        verify(messages, never()).send(any());
        verifyNoInteractions(mapper);
    }
    @Test void malformedCollaborationFlagCannotSilentlySendWeb() {
        for (Object flag : new Object[] {"true", "false", 1, 0}) {
            node.setConfig(Map.of("messageTemplateCode", "legal_notice", "useApplicationCollaboration", flag));
            assertThrows(BusinessException.class, () -> executor.execute(run, node));
        }
        verify(messages, never()).send(any());
    }
    @Test void applicationChannelTypeDriftFailsClosedForLegacyExecutorsToo() {
        AiBusinessMessageChannel altered = channel(1, 7L, "22");
        altered.setChannelType("INTERNAL");
        when(mapper.selectByChannelCode(7L, "app_11_collaboration")).thenReturn(altered);
        assertThrows(BusinessException.class, () -> channels.resolveChannel("app_11_collaboration"));
        verify(messages, never()).send(any());
    }
    @Test void missingNodeIdCannotShareAnIdempotencyKey() {
        node.setConfig(Map.of("messageTemplateCode", "legal_notice"));
        node.setId(null);
        assertThrows(BusinessException.class, () -> executor.execute(run, node));
        node.setId(" ");
        assertThrows(BusinessException.class, () -> executor.execute(run, node));
        verifyNoInteractions(messages, mapper);
    }
}
