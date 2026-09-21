package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessRun;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessRunMapper;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowPrintAccessPolicyTest {

    private static final PrintActor ACTOR = new PrintActor(1L, 7L, 3L);
    private static final PrintSourceRequest SOURCE = new PrintSourceRequest(
            2L, PrintSourceType.LOWCODE, "purchase-page", null, "purchase");
    private final FlowPrintAccessPolicy policy = new FlowPrintAccessPolicy(new ObjectMapper());

    @Test
    void separatesTodoDoneAndStartedActors() {
        policy.authorize(ACTOR, PrintScene.FLOW_TODO, context("task-1", 0, "7", null, "9", null, null));
        policy.authorize(ACTOR, PrintScene.FLOW_TODO, context("task-1", 1, null, null, "9", null, null));
        policy.authorize(ACTOR, PrintScene.FLOW_DONE, context("task-1", 2, "7", null, "9", null, null));
        policy.authorize(ACTOR, PrintScene.FLOW_DONE, context("task-1", 3, "9", "7", "9", null, null));
        policy.authorize(ACTOR, PrintScene.FLOW_STARTED, context(null, null, null, null, "7", null, null));

        assertThatThrownBy(() -> policy.authorize(ACTOR, PrintScene.FLOW_TODO,
                context("task-1", 2, "7", null, "9", null, null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> policy.authorize(ACTOR, PrintScene.FLOW_DONE,
                context("task-1", 2, "9", null, "9", null, null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> policy.authorize(ACTOR, PrintScene.FLOW_STARTED,
                context(null, null, null, null, "9", null, null))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void restrictsTemplatesAndFieldsToNodeReadableSubset() {
        var refs = List.of(
                new AuthorizedPrintContext.VersionRef(10L, 101L, true, 0),
                new AuthorizedPrintContext.VersionRef(20L, 201L, false, 1));
        String permissions = """
                {"version":2,"fields":[
                  {"scope":"main","field":"title","readable":true},
                  {"scope":"main","field":"secret","readable":false},
                  {"scope":"child","childKey":"items","childField":"name","readable":true},
                  {"scope":"child","childKey":"items","childField":"cost","readable":false}
                ],"children":[{"childKey":"items","readable":true}]}
                """;
        var context = context("task-1", 0, "7", null, "9", permissions, "RESTRICT", "20,999");

        assertThat(policy.templates(refs, context)).extracting(AuthorizedPrintContext.VersionRef::templateId)
                .containsExactly(20L);

        var catalog = new PrintFieldCatalogVO(List.of(
                new PrintFieldCatalogVO.Field("main.title", "标题", "TEXT"),
                new PrintFieldCatalogVO.Field("main.secret", "秘密", "TEXT"),
                new PrintFieldCatalogVO.Field("children.items", "明细", "COLLECTION"),
                new PrintFieldCatalogVO.Field("children.items.name", "名称", "TEXT"),
                new PrintFieldCatalogVO.Field("children.items.cost", "成本", "MONEY"),
                new PrintFieldCatalogVO.Field("flow.history", "审批记录", "COLLECTION")));
        assertThat(policy.fields(catalog, context).fields()).extracting(PrintFieldCatalogVO.Field::path)
                .containsExactly("main.title", "children.items", "children.items.name", "flow.history");
    }

    @Test
    void resolvesServerFlowIdentityAndRejectsClientMismatch() {
        @SuppressWarnings("unchecked") ObjectProvider<FlowClient> clients = mock(ObjectProvider.class);
        FlowClient client = mock(FlowClient.class);
        BusinessProcessRunMapper runs = mock(BusinessProcessRunMapper.class);
        when(clients.getIfAvailable()).thenReturn(client);
        when(client.getTaskDetail("task-1")).thenReturn(FlowResult.success(Map.of(
                "id", "task-1", "processInstanceId", "pi-1", "businessKey", "purchase:r1",
                "processDefKey", "purchase-flow", "taskDefKey", "approve", "status", 0, "assignee", "7")));
        when(client.getProcessFormInfo("pi-1", "purchase:r1", "purchase-flow", "task-1", "approve"))
                .thenReturn(FlowResult.success(Map.ofEntries(
                        Map.entry("processInstanceId", "pi-1"),
                        Map.entry("businessKey", "purchase:r1"),
                        Map.entry("objectCode", "purchase"),
                        Map.entry("recordId", "r1"),
                        Map.entry("taskDefKey", "approve"),
                        Map.entry("startUserId", "9"))));
        AiBusinessProcessRun run = new AiBusinessProcessRun();
        run.setId(99L);
        run.setApplicationId(2L);
        run.setFlowProcessInstanceId("pi-1");
        run.setBusinessKey("purchase:r1");
        run.setSubjectObjectCode("purchase");
        run.setSubjectRecordId("r1");
        when(runs.selectRunById(1L, 99L)).thenReturn(run);
        var resolver = new FlowPrintContextResolver(clients, runs);
        var valid = new PrintRecordRequest(SOURCE, "r1", PrintScene.FLOW_TODO,
                "task-1", "pi-1", 99L);

        assertThat(resolver.resolve(ACTOR, valid)).satisfies(context -> {
            assertThat(context.processInstanceId()).isEqualTo("pi-1");
            assertThat(context.processRunId()).isEqualTo(99L);
            assertThat(context.recordId()).isEqualTo("r1");
        });

        var forged = new PrintRecordRequest(SOURCE, "r1", PrintScene.FLOW_TODO,
                "task-1", "pi-forged", 99L);
        assertThatThrownBy(() -> resolver.resolve(ACTOR, forged)).isInstanceOf(RuntimeException.class);

        var codeSource = new PrintSourceRequest(2L, PrintSourceType.CODE, null, "purchase-form", "purchase");
        var codeWithoutServerForm = new PrintRecordRequest(codeSource, "r1", PrintScene.FLOW_TODO,
                "task-1", "pi-1", 99L);
        assertThatThrownBy(() -> resolver.resolve(ACTOR, codeWithoutServerForm)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void preservesEachHistoryTaskInsteadOfMergingCountersignOrRetryRows() {
        @SuppressWarnings("unchecked") ObjectProvider<FlowClient> clients = mock(ObjectProvider.class);
        FlowClient client = mock(FlowClient.class);
        when(clients.getIfAvailable()).thenReturn(client);
        when(client.getProcessHistoryPage("pi-1", 1, 1000)).thenReturn(FlowResult.success(Map.of(
                "records", List.of(
                        Map.of("taskId", "task-a", "taskName", "会签", "action", "approve"),
                        Map.of("taskId", "task-b", "taskName", "会签", "action", "approve")))));

        Map<String, Object> flow = new FlowPrintHistoryAdapter(clients).load(
                context("task-1", 2, "7", null, "9", null, null));

        @SuppressWarnings("unchecked") List<Map<String, Object>> history =
                (List<Map<String, Object>>) flow.get("history");
        assertThat(history).hasSize(2);
        assertThat(history).extracting(item -> item.get("taskId"))
                .containsExactly("task-a", "task-b");
    }

    private FlowPrintContextResolver.Context context(String taskId,
                                                     Integer status,
                                                     String assignee,
                                                     String owner,
                                                     String starter,
                                                     String permissions,
                                                     String templatePolicy,
                                                     String... templateIds) {
        return new FlowPrintContextResolver.Context(
                "purchase:r1", "purchase", "r1", "pi-1", 99L, taskId, "approve", status,
                assignee, owner, starter, null, permissions, templatePolicy,
                templateIds.length == 0 ? null : templateIds[0]);
    }
}
