package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 保证待办/已办列表不会回退到跨租户全表查询或“任意候选组均可见”的宽泛条件。
 */
class FlowTaskMapperSqlContractTest {

    private static final String RESOURCE = "mapper/FlowTaskMapper.xml";

    @Test
    void taskListsMustFilterTenantAndJoinDisplayUsers() throws IOException {
        String xml = resource();
        for (String statementId : List.of("selectTodoTasks", "selectDoneTasks", "selectStartedTasks", "selectCandidateTasks")) {
            String statement = statement(xml, statementId);
            assertTrue(statement.contains("t.tenant_id = #{tenantId}"),
                    () -> statementId + " must filter by the request tenant");
            assertTrue(statement.contains("TaskListUserJoins"),
                    () -> statementId + " must use the SQL user/model joins");
        }
        assertTrue(xml.contains("CONCAT(assignee_user.id, '') = NULLIF(t.assignee, '')"),
                "TaskListUserJoins must use portable id-to-text equality");
        assertFalse(xml.contains("REGEXP"), "FlowTaskMapper must not use MySQL-only REGEXP");
        assertFalse(xml.contains("UNSIGNED"), "FlowTaskMapper must not use MySQL-only UNSIGNED cast");
    }

    @Test
    void taskModelJoinMustResolveNameWhenProcessDefKeyIsDefinitionId() throws IOException {
        String xml = resource();
        assertTrue(xml.contains("ACT_RE_PROCDEF task_procdef"));
        assertTrue(xml.contains("task_procdef.KEY_"));
        assertFalse(xml.contains("COALESCE(NULLIF(m.model_name, ''), t.process_def_key)"));
    }

    @Test
    void candidateListMustStayUnassignedAndAvoidFlowableFullList() throws IOException {
        String xml = resource();
        String statement = statement(xml, "selectCandidateTasks");
        assertTrue(statement.contains("t.status = 0"));
        assertTrue(statement.contains("t.assignee IS NULL OR t.assignee = ''"));
        assertTrue(statement.contains("t.candidate_users"));
    }

    @Test
    void todoMustNotTreatAnyNonEmptyCandidateGroupAsVisible() throws IOException {
        String xml = resource();
        String statement = statement(xml, "selectTodoTasks");
        assertFalse(statement.matches("(?s).*OR\\s*\\(\\s*t\\.candidate_groups\\s+IS\\s+NOT\\s+NULL\\s+AND\\s+t\\.candidate_groups\\s*!=\\s*''\\s*\\).*"),
                "todo candidate group filtering must verify the current user's group membership");
        assertTrue(xml.contains("tc.candidate_type = 'GROUP'"),
                "todo candidate group filtering must use normalized candidate relations");
        assertTrue(xml.contains("tc.candidate_value IN"),
                "todo candidate groups must bind the resolved session memberships");
        assertTrue(xml.contains("NOT EXISTS"),
                "legacy CSV matching must be gated behind the absence of normalized relations");
        assertFalse(xml.contains("sys_user_org_role current_user_role"),
                "todo pagination must not rebuild role memberships for every task row");
        assertFalse(xml.contains("sys_user_org current_user_org"),
                "todo pagination must not rebuild organization memberships for every task row");
    }

    @Test
    void actionableListsMustRequireRuntimeEngineTask() throws IOException {
        String xml = resource();
        for (String statementId : List.of("selectTodoTasks", "selectCandidateTasks", "countWorkspaceTodo")) {
            String statement = statement(xml, statementId);
            assertTrue(statement.contains("RequireRuntimeTaskExists") || statement.contains("ACT_RU_TASK"),
                    () -> statementId + " must ignore phantom rows missing from ACT_RU_TASK");
        }
        assertTrue(xml.contains("RequireRuntimeTaskExists"),
                "runtime task existence check should stay reusable");
    }

    @Test
    void startedListMustShowOneRepresentativeTaskPerProcess() throws IOException {
        String xml = resource();
        String statement = statement(xml, "selectStartedTasks");
        assertTrue(statement.contains("StartedInstanceRepresentativeTask"),
                "my started list must collapse history and parallel tasks into one row per process instance");
        int include = statement.indexOf("StartedInstanceRepresentativeTask");
        int statusFilter = statement.indexOf("t.status = #{status}");
        assertTrue(include >= 0 && statusFilter > include,
                "status filtering must apply to the chosen representative task, not before choosing it");
        int start = xml.indexOf("<sql id=\"StartedInstanceRepresentativeTask\">");
        String fragment = xml.substring(start, xml.indexOf("</sql>", start));
        assertTrue(fragment.contains("newer.process_instance_id = t.process_instance_id"));
        assertTrue(fragment.contains("newer.status IN (0, 1)"), "pending tasks must win over finished history");
        assertTrue(fragment.contains("newer.create_time &gt; t.create_time"), "latest task wins among the same kind");
        assertTrue(fragment.contains("newer.id &gt; t.id"), "ties need a deterministic winner so a process never shows twice");
        assertTrue(statement.contains("MIN(first_task.create_time)") && statement.contains("AS process_start_time"),
                "a process card must show when it was submitted, not when its current step arrived");
    }

    @Test
    void startedRunningCountMustCountProcessesNotParallelTasks() throws IOException {
        String statement = statement(resource(), "countWorkspaceStartedRunning");
        assertTrue(statement.contains("COUNT(DISTINCT t.process_instance_id)"),
                "parallel or countersign tasks of one process must count as one running process");
        assertTrue(statement.contains("RequireRuntimeTaskExists"),
                "phantom task rows left by ended processes must not count as running");
    }

    @Test
    void participantLookupMustNotTreatGroupIdsAsUserIds() throws IOException {
        String statement = statement(resource(), "countProcessParticipant");
        assertTrue(statement.contains("CsvTokenContains") || statement.contains("candidate_users"),
                "participant lookup must still match candidate_users via portable CSV predicate");
        assertFalse(statement.contains("candidate_groups"),
                "candidate group identifiers require group membership resolution and must not be compared to user ids");
    }

    @Test
    void monitorTaskSummaryMustBeTenantScopedAndBatchable() throws IOException {
        String statement = statement(resource(), "selectActiveTaskSummaries");
        assertTrue(statement.contains("tenant_id = #{tenantId}"));
        assertTrue(statement.contains("process_instance_id IN"));
        assertTrue(statement.contains("<foreach"));
        assertTrue(statement.contains("status IN (0, 1)"));
    }

    @Test
    void taskMutationLockMustBindTenantInsideForUpdateQuery() throws IOException {
        String xml = resource();
        String statement = statement(xml, "selectByTaskIdForUpdateAndTenant");
        assertTrue(statement.contains("t.task_id = #{taskId}"));
        assertTrue(statement.contains("t.tenant_id = #{tenantId}"));
        assertTrue(statement.contains("FOR UPDATE"));
    }

    @Test
    void taskMutationUpdatesMustBindTenantInsideUpdatePredicate() throws IOException {
        String statement = updateStatement(resource(), "updateByTaskIdAndTenant");
        assertTrue(statement.contains("WHERE task_id = #{taskId}"));
        assertTrue(statement.contains("AND tenant_id = #{tenantId}"));
        assertTrue(statement.contains("#{task.status}"));
    }

    @Test
    void historyQueryMustBeTenantScopedAndStableForPagination() throws IOException {
        String statement = statement(resource(), "selectHistoryTasks");
        assertTrue(statement.contains("t.process_instance_id = #{processInstanceId}"));
        assertTrue(statement.contains("t.tenant_id = #{tenantId}"));
        assertTrue(statement.contains("ORDER BY t.create_time ASC, t.id ASC"));
    }

    private String resource() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + RESOURCE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String statement(String xml, String id) {
        String startToken = "<select id=\"" + id + "\"";
        int start = xml.indexOf(startToken);
        assertTrue(start >= 0, () -> "Missing mapper statement: " + id);
        int end = xml.indexOf("</select>", start);
        assertTrue(end > start, () -> "Unclosed mapper statement: " + id);
        return xml.substring(start, end);
    }

    private String updateStatement(String xml, String id) {
        String startToken = "<update id=\"" + id + "\"";
        int start = xml.indexOf(startToken);
        assertTrue(start >= 0, () -> "Missing mapper statement: " + id);
        int end = xml.indexOf("</update>", start);
        assertTrue(end > start, () -> "Unclosed mapper statement: " + id);
        return xml.substring(start, end);
    }
}
