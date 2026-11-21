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

    @Test
    void routeShouldNotFallbackToInternalJsonGeneratorsForCasualMessage() {
        AiAgent crud = agent(104L, 1L, "crud_config_builder", "CRUD配置生成器", "根据自然语言描述生成CRUD配置JSON");
        AiAgent screen = agent(1L, 1L, "dashboard_generator", "大屏生成助手", "生成数据可视化大屏布局JSON");
        when(agentMapper.selectEnabledAgents()).thenReturn(List.of(crud, screen));
        when(bindings.findGeneralAgentCode(1L)).thenReturn(null);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(() -> SessionHelper.hasPermission(HubAccess.PERM_HUB_USE)).thenReturn(true);
            var actor = new HubAccess.Actor(1L, 100L, 10L);

            var route = routing.route(actor, "11", null);
            assertEquals(HubAssistantRouting.Kind.CLARIFY, route.kind());
            assertNull(route.agent());
        }
    }

    @Test
    void routeShouldPreferGeneralAssistantCodeOverInternalGenerators() {
        AiAgent crud = agent(104L, 1L, "crud_config_builder", "CRUD配置生成器", "生成CRUD配置JSON");
        AiAgent general = agent(200L, 1L, "general_assistant", "通用助手", "日常问答");
        when(agentMapper.selectEnabledAgents()).thenReturn(List.of(crud, general));
        when(bindings.findGeneralAgentCode(1L)).thenReturn(null);

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(() -> SessionHelper.hasPermission(HubAccess.PERM_HUB_USE)).thenReturn(true);
            var actor = new HubAccess.Actor(1L, 100L, 10L);

            var route = routing.route(actor, "11", null);
            assertEquals(HubAssistantRouting.Kind.CHAT, route.kind());
            assertEquals("general_assistant", route.agent().code());
        }
    }

    @Test
    void routeShouldRouteHowToMenuQuestionToGeneralAndNeverCodegenBuilder() {
        AiAgent schemaBuilder = agent(1002L, 1L, "codegen_schema_builder", "代码生成Schema构建器",
                "根据自然语言描述推断数据模型Schema，支持菜单配置表结构生成");
        AiAgent columnAdvisor = agent(1001L, 1L, "codegen_column_advisor", "代码生成字段顾问",
                "根据数据库字段信息推荐Java类型、表单组件和配置");
        AiAgent bpmnBuilder = agent(1003L, 1L, "flow_bpmn_builder", "流程BPMN生成助手", "生成BPMN流程配置");
        AiAgent general = agent(200L, 1L, "general_assistant", "通用助手", "回答日常办公与平台使用问答");
        when(agentMapper.selectEnabledAgents()).thenReturn(List.of(schemaBuilder, columnAdvisor, bpmnBuilder, general));
        when(bindings.findGeneralAgentCode(1L)).thenReturn("general_assistant");

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(() -> SessionHelper.hasPermission(HubAccess.PERM_HUB_USE)).thenReturn(true);
            var actor = new HubAccess.Actor(1L, 100L, 10L);

            var route = routing.route(actor, "怎么配置菜单", "codegen_schema_builder");
            assertEquals(HubAssistantRouting.Kind.CHAT, route.kind());
            assertEquals("general_assistant", route.agent().code());
        }
    }

    @Test
    void catalogShouldExcludeInternalGeneratorsAndPutGeneralFirst() {
        AiAgent schemaBuilder = agent(1002L, 1L, "codegen_schema_builder", "代码生成Schema构建器", "推断Schema");
        AiAgent crudBuilder = agent(104L, 1L, "crud_config_builder", "CRUD配置生成器", "生成CRUD JSON");
        AiAgent hr = agent(2L, 1L, "hr_policy", "A人事顾问", "请假考勤政策");
        AiAgent general = agent(200L, 1L, "general_assistant", "通用助手", "日常问答");
        when(access.current()).thenReturn(new HubAccess.Actor(1L, 100L, 10L));
        when(agentMapper.selectEnabledAgents()).thenReturn(List.of(schemaBuilder, crudBuilder, hr, general));
        when(bindings.findGeneralAgentCode(1L)).thenReturn("general_assistant");

        var catalog = routing.catalog();
        assertEquals(List.of("general_assistant", "hr_policy"),
                catalog.stream().map(HubAssistantRouting.Agent::code).toList());
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
