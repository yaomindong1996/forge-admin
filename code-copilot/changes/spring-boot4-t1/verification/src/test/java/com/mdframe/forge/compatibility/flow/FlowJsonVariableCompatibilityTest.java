package com.mdframe.forge.compatibility.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowBpmn;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import java.util.Map;
import java.util.UUID;

import static com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 Flowable 8 装配保留旧 JsonNode 变量，不能在升级后静默变成序列化 Java 对象。 */
@SpringBootTest(classes = FlowableCompatibilityTest.ProbeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"flowable.database-schema-update=true", "flowable.async-executor-activate=false",
                "flowable.async-history-executor-activate=false", "flowable.check-process-definitions=false",
                "flowable.eventregistry.enabled=false", "flowable.variable-json-mapper=jackson2",
                "spring.main.banner-mode=off"})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class FlowJsonVariableCompatibilityTest {
    private final ProcessEngine engine;

    FlowJsonVariableCompatibilityTest(ProcessEngine engine) {
        this.engine = engine;
    }

    @Test
    void roundTripsLegacyJsonVariablesAndReadsHistoricJsonType() throws Exception {
        String tenant = "t1-" + UUID.randomUUID();
        engine.getRepositoryService().createDeployment().tenantId(tenant)
                .addString("t1-purchase.bpmn20.xml", SamplePurchaseOrderFlowBpmn.build()).deploy();
        var process = engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(MODEL_KEY,
                BUSINESS_TYPE + ":t1", Map.of(VAR_INITIATOR, "applicant-t1", VAR_DEPT_LEADER_ID, "leader-t1"), tenant);
        var json = new ObjectMapper().readTree("""
                {"sections":[{"field":"recordId","editable":false}],"recordId":"2100942720360046593"}
                """);
        engine.getRuntimeService().setVariable(process.getId(), "uiDocument", json);
        assertEquals(json, engine.getRuntimeService().getVariable(process.getId(), "uiDocument"));
        var historic = engine.getHistoryService().createHistoricVariableInstanceQuery()
                .processInstanceId(process.getId()).variableName("uiDocument").singleResult();
        assertNotNull(historic);
        assertEquals("json", historic.getVariableTypeName());
        assertEquals(json, historic.getValue());
    }
}
