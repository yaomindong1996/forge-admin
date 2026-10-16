package com.mdframe.forge.compatibility.flow;

import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowBpmn;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.TestConstructor;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition.*;
import static org.junit.jupiter.api.Assertions.*;

/** 只验证引擎和当前真实 BPMN，不替代 Forge 回调 / MySQL 存量迁移验收。 */
@SpringBootTest(classes = FlowableCompatibilityTest.ProbeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "flowable.database-schema-update=true",
                "flowable.async-executor-activate=false",
                "flowable.async-history-executor-activate=false",
                "flowable.check-process-definitions=false",
                "flowable.eventregistry.enabled=false",
                "spring.main.banner-mode=off"
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class FlowableCompatibilityTest {

    private final ProcessEngine engine;
    private final RepositoryService repository;
    private final RuntimeService runtime;
    private final TaskService tasks;
    private final HistoryService history;
    private String tenant;

    FlowableCompatibilityTest(ProcessEngine engine) {
        this.engine = engine;
        this.repository = engine.getRepositoryService();
        this.runtime = engine.getRuntimeService();
        this.tasks = engine.getTaskService();
        this.history = engine.getHistoryService();
    }

    @BeforeEach
    void deployRealPurchaseBpmn() {
        tenant = "t0-" + UUID.randomUUID();
        repository.createDeployment().tenantId(tenant)
                .addString("purchase.bpmn20.xml", SamplePurchaseOrderFlowBpmn.build()).deploy();
    }

    @Test
    void bootsWithRequestedVersionAndRetainsInternalExpressionApi() {
        assertEquals(System.getProperty("probe.boot.version"), SpringBootVersion.getVersion());
        assertTrue(engine.getProcessEngineConfiguration() instanceof ProcessEngineConfigurationImpl);
        ExpressionManager manager = ((ProcessEngineConfigurationImpl)
                engine.getProcessEngineConfiguration()).getExpressionManager();
        assertNotNull(manager);
        assertNotNull(manager.createExpression("${deptLeaderId}"));
    }

    @Test
    void approvesThroughAllNodesAndParallelCountersign() {
        String processId = start();
        assertNode(processId, NODE_DEPT_LEADER_APPROVE);
        completeCurrent(processId, ACTION_APPROVE);
        assertNode(processId, NODE_ENGINEERING_MANAGER_APPROVE);
        completeCurrent(processId, ACTION_APPROVE);
        assertEquals(2, active(processId).size());
        assertTrue(active(processId).stream().allMatch(task ->
                NODE_PURCHASE_COUNTERSIGN.equals(task.getTaskDefinitionKey())));
        completeAll(processId, ACTION_APPROVE);
        assertFinished(processId);
        assertEquals(4, history.createHistoricTaskInstanceQuery()
                .processInstanceId(processId).finished().count());
    }

    @Test
    void rejectsToApplicantAndResubmitsWithoutDuplicateTasks() {
        String processId = start();
        completeCurrent(processId, ACTION_REJECT);
        assertNode(processId, NODE_APPLICANT_MODIFY);
        assertEquals("applicant-t0", active(processId).get(0).getAssignee());
        completeCurrent(processId, ACTION_APPROVE);
        assertNode(processId, NODE_DEPT_LEADER_APPROVE);
        completeCurrent(processId, ACTION_APPROVE);
        completeCurrent(processId, ACTION_APPROVE);
        completeAll(processId, ACTION_APPROVE);
        assertFinished(processId);
    }

    @Test
    void rejectsCountersignAndTerminatesFromApplicantNode() {
        String processId = start();
        completeCurrent(processId, ACTION_APPROVE);
        completeCurrent(processId, ACTION_APPROVE);
        assertEquals(2, active(processId).size());
        tasks.complete(active(processId).get(0).getId(), decision(ACTION_REJECT));
        assertNode(processId, NODE_APPLICANT_MODIFY);
        completeCurrent(processId, ACTION_REJECT);
        assertFinished(processId);
    }

    @Test
    void transfersCurrentTaskAndRetainsBusinessKeyAndHistory() {
        String processId = start();
        Task task = active(processId).get(0);
        tasks.addComment(task.getId(), processId, "T0 合成转办意见");
        tasks.setAssignee(task.getId(), "delegate-t0");
        assertEquals("delegate-t0", active(processId).get(0).getAssignee());
        assertEquals(BUSINESS_TYPE + ":t0-record", runtime.createProcessInstanceQuery()
                .processInstanceId(processId).singleResult().getBusinessKey());
        assertEquals(1, tasks.getProcessInstanceComments(processId).size());
        completeCurrent(processId, ACTION_APPROVE);
        assertNode(processId, NODE_ENGINEERING_MANAGER_APPROVE);
    }

    @Test
    void keepsTenantScopedDeploymentTaskAndProcessQueries() {
        String processId = start();
        assertEquals(1, tasks.createTaskQuery().processInstanceId(processId).taskTenantId(tenant).count());
        assertEquals(0, tasks.createTaskQuery().processInstanceId(processId)
                .taskTenantId("other-t0-tenant").count());
        assertEquals(0, runtime.createProcessInstanceQuery().processInstanceId(processId)
                .processInstanceTenantId("other-t0-tenant").count());
        assertEquals(1, repository.createProcessDefinitionQuery().processDefinitionKey(MODEL_KEY)
                .processDefinitionTenantId(tenant).count());
    }

    @Test
    void parsesRealFieldPermissionsAndDefaultGatewayWithCurrentInternalSourceApi() {
        BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(new BytesStreamSource(
                SamplePurchaseOrderFlowBpmn.build().getBytes(StandardCharsets.UTF_8)), false, true);
        UserTask leader = (UserTask) model.getMainProcess().getFlowElement(NODE_DEPT_LEADER_APPROVE);
        String namespace = "http://flowable.org/bpmn";
        assertEquals(FORM_KEY, leader.getFormKey());
        assertEquals(nodeFieldPermissionsJson(NODE_DEPT_LEADER_APPROVE),
                leader.getAttributeValue(namespace, "formFieldPermissions"));
        assertEquals("true", leader.getAttributeValue(namespace, "requireComment"));
        ExclusiveGateway gateway = (ExclusiveGateway) model.getMainProcess()
                .getFlowElement("Gateway_dept_result");
        SequenceFlow defaultFlow = (SequenceFlow) model.getMainProcess()
                .getFlowElement(gateway.getDefaultFlow());
        assertNull(defaultFlow.getConditionExpression());
    }

    @Test
    void preservesDynamicDocumentVariablesAndLargeStringIds() {
        String processId = start();
        Map<String, Object> document = Map.of("sections", List.of(Map.of(
                "id", "t0-section", "fields", List.of(
                        Map.of("field", "amountCent", "editable", true),
                        Map.of("field", "recordId", "editable", false)))));
        runtime.setVariable(processId, "uiDocument", document);
        assertEquals(document, runtime.getVariable(processId, "uiDocument"));
        assertEquals("9007199254740993", runtime.getVariable(processId, VAR_RECORD_ID));
    }

    private String start() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(VAR_INITIATOR, "applicant-t0");
        variables.put(VAR_DEPT_LEADER_ID, "leader-t0");
        variables.put(VAR_ENGINEERING_MANAGER_ID, "manager-t0");
        variables.put(VAR_COUNTERSIGN_USER_LIST, List.of("signer-a-t0", "signer-b-t0"));
        variables.put(VAR_APPROVED, true);
        variables.put(VAR_APPROVAL_RESULT, ACTION_APPROVE);
        variables.put(VAR_RECORD_ID, "9007199254740993");
        return runtime.startProcessInstanceByKeyAndTenantId(
                MODEL_KEY, BUSINESS_TYPE + ":t0-record", variables, tenant).getId();
    }

    private List<Task> active(String processId) {
        return tasks.createTaskQuery().processInstanceId(processId).list();
    }

    private void assertNode(String processId, String node) {
        assertEquals(1, active(processId).size());
        assertEquals(node, active(processId).get(0).getTaskDefinitionKey());
    }

    private Map<String, Object> decision(String action) {
        return Map.of(VAR_APPROVAL_RESULT, action, VAR_APPROVED, ACTION_APPROVE.equals(action));
    }

    private void completeCurrent(String processId, String action) {
        assertEquals(1, active(processId).size());
        tasks.complete(active(processId).get(0).getId(), decision(action));
    }

    private void completeAll(String processId, String action) {
        for (Task task : active(processId)) {
            tasks.complete(task.getId(), decision(action));
        }
    }

    private void assertFinished(String processId) {
        assertEquals(0, active(processId).size());
        assertEquals(0, runtime.createProcessInstanceQuery().processInstanceId(processId).count());
        assertNotNull(history.createHistoricProcessInstanceQuery().processInstanceId(processId)
                .finished().singleResult());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class ProbeApplication {
        @Bean
        DataSource isolatedDataSource() {
            // 强制内存 H2；不读取生产 application-dev 或外部 MySQL / Redis 地址。
            return new SimpleDriverDataSource(new org.h2.Driver(),
                    "jdbc:h2:mem:forge_boot4_t0;DB_CLOSE_DELAY=-1", "sa", "");
        }
    }
}
