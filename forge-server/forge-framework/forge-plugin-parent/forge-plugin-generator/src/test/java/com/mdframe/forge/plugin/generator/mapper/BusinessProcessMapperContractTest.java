package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcessVersion;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessRunDetailVO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessRunVO;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessObjectProcessVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessProcess mapper contract")
class BusinessProcessMapperContractTest {

    @Test
    @DisplayName("process definition uses primary-key tombstone logic delete")
    void processDefinitionUsesPrimaryKeyTombstone() throws NoSuchFieldException {
        TableLogic tableLogic = AiBusinessProcess.class.getDeclaredField("delFlag")
                .getAnnotation(TableLogic.class);

        assertNotNull(tableLogic);
        assertEquals("0", tableLogic.value());
        assertEquals("id", tableLogic.delval());
    }

    @Test
    @DisplayName("definition queries fail closed on tenant application subject and deleted rows")
    void definitionQueriesFailClosed() throws IOException {
        String xml = resource("mapper/BusinessProcessMapper.xml");
        String page = statement(xml, "select", "selectProcessPage");

        assertTrue(xml.contains("p.tenant_id = #{tenantId}"));
        assertTrue(xml.contains("p.application_id = #{applicationId}"));
        assertTrue(xml.contains("a.status = 1"));
        assertTrue(xml.contains("ao.object_id = p.subject_object_id"));
        assertTrue(xml.contains("o.object_code = p.subject_object_code"));
        assertTrue(xml.contains("p.del_flag = 0"));
        assertTrue(page.contains("List_Columns"));
        assertFalse(page.contains("Base_Columns"));
    }

    @Test
    @DisplayName("object process summary is tenant scoped and extracts the real start node")
    void objectProcessSummaryIsTenantScoped() throws IOException, NoSuchFieldException {
        String query = statement(resource("mapper/BusinessProcessMapper.xml"),
                "select", "selectBySubjectObjectCode");

        assertEquals(String.class, BusinessObjectProcessVO.class.getDeclaredField("id").getType());
        assertTrue(query.contains("CAST(p.id AS CHAR)"));
        assertTrue(query.contains("p.tenant_id = #{tenantId}"));
        assertTrue(query.contains("p.subject_object_code = #{objectCode}"));
        assertTrue(query.contains("p.del_flag = 0"));
        // 起点投影已迁至服务端规范化器，Mapper 仅提供受租户约束的草稿输入。
        assertTrue(query.contains("p.draft_schema_json AS draftSchemaJson"));
        assertTrue(query.contains("Active_Application_And_Subject"));
    }

    @Test
    @DisplayName("draft save is hash guarded and delete writes row id")
    void draftSaveAndDeleteUseCasContracts() throws IOException {
        String xml = resource("mapper/BusinessProcessMapper.xml");

        assertTrue(xml.contains("draft_schema_hash = #{expectedSchemaHash}"));
        assertTrue(xml.contains("subject_object_id = #{subjectObjectId}"));
        assertTrue(xml.contains("<update id=\"updateBasicInfo\">"));
        assertTrue(xml.contains("<update id=\"updateStatus\">"));
        assertTrue(xml.contains("<update id=\"updateDesignStatus\">"));
        assertTrue(xml.contains("SET del_flag = id"));
        assertTrue(xml.contains("update_by = #{updateBy}"));
    }

    @Test
    @DisplayName("deletion run check is tenant scoped and only counts active records")
    void deletionRunCheckIsTenantScopedAndActiveOnly() throws IOException {
        String runXml = resource("mapper/BusinessProcessRunMapper.xml");

        assertTrue(runXml.contains("<select id=\"countByProcessId\""));
        assertTrue(runXml.contains("tenant_id = #{tenantId}"));
        assertTrue(runXml.contains("process_id = #{processId}"));
        assertTrue(runXml.contains("status IN ('PENDING', 'RUNNING', 'WAITING')"));
    }

    @Test
    @DisplayName("published version uses primary-key tombstone and exposes no update SQL")
    void publishedVersionIsImmutable() throws IOException, NoSuchFieldException {
        TableLogic tableLogic = AiBusinessProcessVersion.class.getDeclaredField("delFlag")
                .getAnnotation(TableLogic.class);
        String xml = resource("mapper/BusinessProcessVersionMapper.xml");

        assertNotNull(tableLogic);
        assertEquals("0", tableLogic.value());
        assertEquals("id", tableLogic.delval());
        assertTrue(xml.contains("<insert id=\"insertImmutable\">"));
        assertTrue(xml.contains("tenant_id = #{tenantId}"));
        assertTrue(xml.contains("del_flag = 0"));
        assertTrue(xml.contains("AND 1 = 0"));
        assertFalse(xml.contains("<update"));
        assertFalse(xml.contains("UPDATE ai_business_process_version"));
    }

    @Test
    @DisplayName("application publish locks definitions and projects immutable versions")
    void applicationPublishUsesLockedProjectionContracts() throws IOException {
        String processXml = resource("mapper/BusinessProcessMapper.xml");
        String versionXml = resource("mapper/BusinessProcessVersionMapper.xml");
        String publishLock = statement(processXml, "select", "selectForPublish");
        String appVersion = statement(
                versionXml, "select", "selectPublishedForApplicationVersion");

        assertTrue(publishLock.contains("p.status = 1"));
        assertTrue(publishLock.contains("FOR UPDATE"));
        assertTrue(processXml.contains("<update id=\"updatePublishedProjection\">"));
        assertTrue(processXml.contains("draft_schema_hash = #{schemaHash}"));
        assertTrue(processXml.contains("<update id=\"clearPublishedProjectionExcept\">"));
        assertTrue(appVersion.contains("application_version = #{applicationVersion}"));
        assertTrue(appVersion.contains("del_flag = 0"));
    }

    @Test
    @DisplayName("process run transitions are tenant state node and correlation guarded")
    void processRunTransitionsUseStrongCas() throws IOException {
        String xml = resource("mapper/BusinessProcessRunMapper.xml");

        assertTrue(xml.contains("tenant_id = #{tenantId}"));
        assertTrue(xml.contains("status = #{expectedStatus}"));
        assertTrue(xml.contains("current_node_id = #{expectedCurrentNodeId}"));
        assertTrue(xml.contains("flow_process_instance_id = #{expectedProcessInstanceId}"));
        assertTrue(xml.contains("status = 'WAITING'"));
        assertTrue(xml.contains("retry_count &lt; #{maxRetryCount}"));
        assertFalse(xml.contains("DELETE FROM ai_business_process_run"));
    }

    @Test
    @DisplayName("active process lookup is batched tenant scoped and status constrained")
    void activeProcessLookupIsBatchedAndScoped() throws IOException {
        String query = statement(resource("mapper/BusinessProcessRunMapper.xml"),
                "select", "selectActiveByBusinessKeys");

        assertTrue(query.contains("tenant_id = #{tenantId}"));
        assertTrue(query.contains("status IN ('PENDING', 'RUNNING', 'WAITING')"));
        assertTrue(query.contains("collection=\"businessKeys\""));
        assertFalse(query.contains("LIMIT"));
    }

    @Test
    @DisplayName("started process lookup includes terminal success but keeps failures retryable")
    void startedProcessLookupIsBatchedAndExcludesFailures() throws IOException {
        String query = statement(resource("mapper/BusinessProcessRunMapper.xml"),
                "select", "selectStartedByBusinessKeys");

        assertTrue(query.contains("tenant_id = #{tenantId}"));
        assertTrue(query.contains("'SUCCESS'"));
        assertTrue(query.contains("'CANCELED'"));
        assertFalse(query.contains("'FAILED'"));
        assertTrue(query.contains("collection=\"businessKeys\""));
        assertFalse(query.contains("LIMIT"));
    }

    @Test
    @DisplayName("detail flow history can recover the latest correlated process run")
    void latestBusinessProcessHistoryIsTenantAndBusinessKeyScoped() throws IOException {
        String query = statement(resource("mapper/BusinessProcessRunMapper.xml"),
                "select", "selectLatestByBusinessKey");

        assertTrue(query.contains("tenant_id = #{tenantId}"));
        assertTrue(query.contains("business_key = #{businessKey}"));
        assertTrue(query.contains("flow_process_instance_id IS NOT NULL"));
        assertTrue(query.contains("ORDER BY create_time DESC, id DESC"));
        assertTrue(query.contains("LIMIT 1"));
    }

    @Test
    @DisplayName("node attempts are claimed once and waiting callbacks match correlation")
    void nodeAttemptsUseStrongCas() throws IOException {
        String xml = resource("mapper/BusinessProcessNodeRunMapper.xml");

        assertTrue(xml.contains("<insert id=\"insertAttempt\">"));
        assertTrue(xml.contains("AND status = 'PENDING'"));
        assertTrue(xml.contains("AND status = #{expectedStatus}"));
        assertTrue(xml.contains("correlation_id = #{expectedCorrelationId}"));
        assertFalse(xml.contains("DELETE FROM ai_business_process_node_run"));
    }

    @Test
    @DisplayName("run list and timeline expose only safe summaries with string ids")
    void runQueriesExposeSafeSummariesAndStringIds() throws IOException, NoSuchFieldException {
        String runPage = statement(resource("mapper/BusinessProcessRunMapper.xml"), "select", "selectRunPage");
        String timeline = statement(resource("mapper/BusinessProcessNodeRunMapper.xml"), "select", "selectTimeline");

        assertEquals(String.class, BusinessProcessRunVO.class.getDeclaredField("id").getType());
        assertEquals(String.class, BusinessProcessRunVO.class.getDeclaredField("processVersionId").getType());
        assertEquals(String.class, BusinessProcessRunDetailVO.NodeRunVO.class.getDeclaredField("id").getType());
        assertTrue(runPage.contains("CAST(r.id AS CHAR)"));
        assertTrue(runPage.contains("r.tenant_id = #{tenantId}"));
        assertFalse(runPage.contains("context_snapshot"));
        assertFalse(runPage.contains("idempotency_key"));
        assertFalse(runPage.contains("source_event_id"));
        assertTrue(timeline.contains("Timeline_Columns"));
        assertFalse(timeline.contains("Base_Columns"));
    }

    @Test
    @DisplayName("node timeline retry and approval association remain tenant and run scoped")
    void nodeTimelineQueriesAreTenantAndRunScoped() throws IOException {
        String xml = resource("mapper/BusinessProcessNodeRunMapper.xml");

        assertTrue(xml.contains("<select id=\"selectTimeline\""));
        assertTrue(xml.contains("<select id=\"selectWaitingByCorrelation\""));
        assertTrue(xml.contains("<select id=\"selectRetryableAttempts\""));
        assertTrue(xml.contains("tenant_id = #{tenantId}"));
        assertTrue(xml.contains("run_id = #{runId}"));
        assertTrue(xml.contains("correlation_id = #{correlationId}"));
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "找不到 Mapper XML: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String statement(String xml, String tag, String id) {
        String opening = "<" + tag + " id=\"" + id + "\"";
        int start = xml.indexOf(opening);
        int end = xml.indexOf("</" + tag + ">", start);
        assertTrue(start >= 0 && end > start, "找不到 Mapper statement: " + id);
        return xml.substring(start, end);
    }
}
