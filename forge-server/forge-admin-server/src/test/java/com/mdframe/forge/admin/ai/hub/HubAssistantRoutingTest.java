package com.mdframe.forge.admin.ai.hub;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.mapper.AiAgentMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubAssistantRoutingTest {

    @Mock
    private HubAccess access;

    @Mock
    private AiAgentMapper agentMapper;

    @Mock
    private HubBindingLookup bindings;

    private HubAssistantRouting routing;

    @BeforeEach
    void setUp() {
        routing = new HubAssistantRouting(access, agentMapper, bindings);
    }

    @Test
    void routeShouldClassifyTodoQueryWithoutCallingAgents() {
        var actor = new HubAccess.Actor(1L, 100L, 10L);
        var route = routing.route(actor, "帮我看一下我的待办审批", null);

        assertEquals(HubAssistantRouting.Kind.TODO, route.kind());
        assertEquals(HubIntent.MY_TODOS, route.intent());
        assertNull(route.agent());
        assertNotNull(route.turnId());
    }

    @Test
    void routeShouldPreferGeneralAgentOnGreetingAndMatchSpecialistOnTopic() {
        AiAgent general = agent(1L, 1L, "general", "通用助手", "日常办公问答与文档总结");
        AiAgent hr = agent(2L, 1L, "hr_policy", "人事制度顾问", "解答员工请假、考勤与报销政策");
        when(agentMapper.selectEnabledAgents()).thenReturn(List.of(general, hr));
        when(bindings.findGeneralAgentCode(1L)).thenReturn(null);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(() -> SessionHelper.hasPermission(HubAccess.PERM_ENGINE_STREAM)).thenReturn(true);
            var actor = new HubAccess.Actor(1L, 100L, 10L);

            var greetingRoute = routing.route(actor, "你好", null);
            assertEquals(HubAssistantRouting.Kind.CHAT, greetingRoute.kind());
            assertEquals("general", greetingRoute.agent().code());

            var specialistRoute = routing.route(actor, "请帮我查一下人事制度顾问的请假考勤报销政策", null);
            assertEquals(HubAssistantRouting.Kind.CHAT, specialistRoute.kind());
            assertEquals("hr_policy", specialistRoute.agent().code());
            assertEquals(HubAssistantRouting.Stage.AGENT_MATCH, specialistRoute.stage());
        }
    }

    @Test
    void routeShouldClarifyWhenTenantHasNoEnabledAgents() {
        when(agentMapper.selectEnabledAgents()).thenReturn(List.of());

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(() -> SessionHelper.hasPermission(HubAccess.PERM_HUB_USE)).thenReturn(true);
            var actor = new HubAccess.Actor(1L, 100L, 10L);

            var route = routing.route(actor, "总结今天的工作", null);
            assertEquals(HubAssistantRouting.Kind.CLARIFY, route.kind());
            assertNull(route.agent());
        }
    }

    private AiAgent agent(Long id, Long tenantId, String code, String name, String desc) {
        AiAgent a = new AiAgent();
        a.setId(id);
        a.setTenantId(tenantId);
        a.setAgentCode(code);
        a.setAgentName(name);
        a.setDescription(desc);
        return a;
    }
}
