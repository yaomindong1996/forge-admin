package com.mdframe.forge.plugin.generator.businessprocess.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessValidationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("businessProcessJson schema validator")
class BusinessProcessSchemaValidatorTest {

    private BusinessProcessSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BusinessProcessSchemaValidator(new ObjectMapper());
    }

    @Test
    @DisplayName("normalization produces stable canonical json and hash")
    void normalizationProducesStableHash() {
        BusinessProcessSchema first = validator.normalize(validSchema());
        BusinessProcessSchema reordered = validator.normalize(reorderedValidSchema());

        assertEquals(validator.canonicalJson(first), validator.canonicalJson(reordered));
        assertEquals(validator.schemaHash(first), validator.schemaHash(reordered));

        reordered.setProcessCode("invoice_created_update");
        assertNotEquals(validator.schemaHash(first), validator.schemaHash(reordered));
    }

    @Test
    @DisplayName("legacy alphabetic result ports migrate to business order")
    void legacyResultPortsUseBusinessOrder() throws IOException {
        BusinessProcessSchema schema = validator.normalize(
                resource("businessprocess/manual-approval.json").replace(
                        "[\"APPROVED\", \"REJECTED\", \"CANCELED\", \"FAILED\"]",
                        "[\"APPROVED\", \"CANCELED\", \"FAILED\", \"REJECTED\"]"));

        assertEquals(List.of("APPROVED", "REJECTED", "CANCELED", "FAILED"),
                schema.getNodes().stream()
                        .filter(node -> "approval_purchase".equals(node.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getPorts());
    }

    @Test
    @DisplayName("valid graph and governed dependencies pass")
    void validGraphPasses() {
        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(validSchema()), validContext());

        assertTrue(result.isValid());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    @DisplayName("three frozen protocol examples pass governed validation")
    void frozenProtocolExamplesPassValidation() throws IOException {
        for (String fixture : Set.of("manual-approval.json", "event-approval.json", "schedule-reminder.json")) {
            BusinessProcessSchema schema = validator.normalize(resource("businessprocess/" + fixture));
            BusinessProcessValidationContext context = frozenExampleContext()
                    .setExpectedProcessCode(schema.getProcessCode());

            BusinessProcessValidationVO result = validator.validate(schema, context);

            assertTrue(result.isValid(), fixture + " issues: " + result.getIssues());
        }
    }

    @Test
    @DisplayName("low-code approval requires an independent flow status field")
    void lowcodeApprovalRequiresIndependentFlowStatus() throws IOException {
        String lowcodeApproval = resource("businessprocess/manual-approval.json")
                .replace("\"formMode\": \"BUSINESS_CODE_FORM\"", "\"formMode\": \"BUSINESS_OBJECT_FORM\"");
        BusinessProcessSchema missingStatus = validator.normalize(lowcodeApproval);
        BusinessProcessValidationContext context = frozenExampleContext()
                .setExpectedProcessCode(missingStatus.getProcessCode());

        BusinessProcessValidationVO invalid = validator.validate(missingStatus, context);

        assertTrue(invalid.hasError("APPROVAL_FLOW_STATUS_REQUIRED"));

        BusinessProcessSchema businessStatus = validator.normalize(lowcodeApproval.replace(
                "\"versionPolicy\": \"PINNED_AT_APPLICATION_PUBLISH\",",
                "\"versionPolicy\": \"PINNED_AT_APPLICATION_PUBLISH\", \"statusField\": \"status\","));
        assertTrue(validator.validate(businessStatus, context)
                .hasError("APPROVAL_FLOW_STATUS_REQUIRED"));

        BusinessProcessSchema configured = validator.normalize(lowcodeApproval.replace(
                "\"versionPolicy\": \"PINNED_AT_APPLICATION_PUBLISH\",",
                "\"versionPolicy\": \"PINNED_AT_APPLICATION_PUBLISH\", \"statusField\": \"flowStatus\","));
        BusinessProcessValidationVO valid = validator.validate(configured, context);

        assertFalse(valid.hasError("APPROVAL_FLOW_STATUS_REQUIRED"));
    }

    @Test
    @DisplayName("condition branches require one default branch and complete structured rules")
    void conditionBranchesRequireCompleteStructuredRules() throws IOException {
        String invalid = resource("businessprocess/schedule-reminder.json")
                .replace("\"rules\": [{\"source\": \"context\", \"field\": \"daysUntilDue\", \"operator\": \"LT\", \"value\": 0}]",
                        "\"rules\": []")
                .replace("{\"port\": \"DUE_SOON\", \"isDefault\": true}",
                        "{\"port\": \"DUE_SOON\", \"condition\": {\"operator\": \"AND\", \"rules\": []}}");

        BusinessProcessSchema schema = validator.normalize(invalid);
        BusinessProcessValidationVO result = validator.validate(schema,
                frozenExampleContext().setExpectedProcessCode(schema.getProcessCode()));

        assertTrue(result.hasError("CONDITION_RULE_REQUIRED"));
        assertTrue(result.hasError("CONDITION_DEFAULT_INVALID"));
    }

    @Test
    @DisplayName("condition node requires a judgment branch in addition to the default branch")
    void conditionRequiresJudgmentAndDefaultBranches() {
        String invalid = validSchema()
                .replace(
                        "{\"id\":\"update\",\"type\":\"ACTION\",\"name\":\"更新状态\",\"config\":{\"actionType\":\"UPDATE_RECORD\",\"objectCode\":\"order\",\"fieldMappings\":[{\"field\":\"status\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}]}}",
                        "{\"id\":\"update\",\"type\":\"CONDITION\",\"name\":\"条件判断\",\"ports\":[\"OTHERWISE\"],\"config\":{\"branches\":[{\"port\":\"OTHERWISE\",\"label\":\"其他情况\",\"isDefault\":true}]}}")
                .replace(
                        "{\"id\":\"e2\",\"source\":\"update\",\"target\":\"end\",\"sourcePort\":\"NEXT\"}",
                        "{\"id\":\"e2\",\"source\":\"update\",\"target\":\"end\",\"sourcePort\":\"OTHERWISE\",\"isDefault\":true}");

        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(invalid), validContext());

        assertTrue(result.hasError("CONDITION_BRANCH_COUNT_INVALID"));
    }

    @Test
    @DisplayName("malformed json and duplicate keys are rejected before validation")
    void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.normalize("[]"));
        assertThrows(IllegalArgumentException.class, () -> validator.normalize(
                "{\"schemaVersion\":\"1.0\",\"schemaVersion\":\"2.0\"}"));
        assertThrows(IllegalArgumentException.class, () -> validator.normalize(
                validSchema().replace("\"1900000000000001001\"", "1900000000000001001")));
    }

    @Test
    @DisplayName("multiple starts cycles dangling edges and invalid ports fail closed")
    void invalidGraphFailsClosed() {
        String invalid = validSchema()
                .replace("{\"id\":\"end\",\"type\":\"END\"",
                        "{\"id\":\"start_2\",\"type\":\"START_EVENT\",\"name\":\"重复开始\",\"config\":{\"eventType\":\"RECORD_UPDATED\"}},"
                                + "{\"id\":\"end\",\"type\":\"END\"")
                .replace("{\"id\":\"e2\",\"source\":\"update\",\"target\":\"end\",\"sourcePort\":\"NEXT\"}",
                        "{\"id\":\"e2\",\"source\":\"update\",\"target\":\"missing\",\"sourcePort\":\"INVALID\"},"
                                + "{\"id\":\"e3\",\"source\":\"update\",\"target\":\"start\",\"sourcePort\":\"NEXT\"}");

        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(invalid), validContext());

        assertFalse(result.isValid());
        assertTrue(result.hasError("START_NODE_COUNT"));
        assertTrue(result.hasError("EDGE_TARGET_MISSING"));
        assertTrue(result.hasError("EDGE_PORT_INVALID"));
        assertTrue(result.hasError("GRAPH_CYCLE"));
    }

    @Test
    @DisplayName("update/create record actions require at least one configured field mapping")
    void actionFieldMappingsAreRequired() {
        String invalid = validSchema().replace(
                "\"fieldMappings\":[{\"field\":\"status\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}]",
                "\"fieldMappings\":[{\"field\":\"\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}]");

        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(invalid), validContext());

        assertTrue(result.hasError("ACTION_FIELD_MAPPING_REQUIRED"));
    }

    @Test
    @DisplayName("unreachable nodes and nodes without an end path are rejected")
    void reachabilityAndEndPathAreRequired() {
        String invalid = validSchema().replace(
                "{\"id\":\"end\",\"type\":\"END\",\"name\":\"完成\",\"config\":{\"result\":\"SUCCESS\"}}",
                "{\"id\":\"end\",\"type\":\"END\",\"name\":\"完成\",\"config\":{\"result\":\"SUCCESS\"}},"
                        + "{\"id\":\"orphan\",\"type\":\"ACTION\",\"name\":\"孤立节点\",\"config\":{\"actionType\":\"UPDATE_RECORD\",\"objectCode\":\"order\",\"fieldMappings\":[]}}");

        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(invalid), validContext());

        assertTrue(result.hasError("NODE_UNREACHABLE"));
        assertTrue(result.hasError("END_PATH_MISSING"));
    }

    @Test
    @DisplayName("unknown nodes and missing end nodes are rejected")
    void unknownNodeAndMissingEndAreRejected() {
        BusinessProcessSchema invalid = validator.normalize(validSchema());
        invalid.getNodes().stream()
                .filter(node -> "update".equals(node.getId()))
                .findFirst()
                .orElseThrow()
                .setType("CUSTOM");
        invalid.getNodes().removeIf(node -> "END".equals(node.getType()));
        invalid.getEdges().removeIf(edge -> "end".equals(edge.getTarget()));

        BusinessProcessValidationVO result = validator.validate(
                invalid, validContext());

        assertTrue(result.hasError("NODE_TYPE_UNKNOWN"));
        assertTrue(result.hasError("END_NODE_REQUIRED"));
    }

    @Test
    @DisplayName("nested secret keys free urls and webhook actions are rejected")
    void sensitiveConfigurationIsRejected() {
        String invalid = validSchema().replace(
                "\"fieldMappings\":[{\"field\":\"status\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}]",
                "\"fieldMappings\":[{\"field\":\"status\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}],"
                        + "\"credentials\":{\"accessToken\":\"plain\"},\"endpoint\":\"https://example.invalid/hook\"")
                .replace("\"actionType\":\"UPDATE_RECORD\"", "\"actionType\":\"WEBHOOK\"");

        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(invalid), validContext());

        assertTrue(result.hasError("SENSITIVE_KEY"));
        assertTrue(result.hasError("FREE_URL_FORBIDDEN"));
        assertTrue(result.hasError("ACTION_TYPE_UNSUPPORTED"));
    }

    @Test
    @DisplayName("missing object fields and unpublished dependencies fail closed")
    void staleDependenciesFailClosed() {
        BusinessProcessValidationContext context = validContext()
                .setObjectIdsByCode(Map.of("other", "9"))
                .setPublishedObjectVersionIdsByCode(Map.of("other", "90"))
                .setFieldsByObjectCode(Map.of("order", Set.of("id")));

        BusinessProcessValidationVO result = validator.validate(
                validator.normalize(validSchema()), context);

        assertTrue(result.hasError("SUBJECT_OBJECT_UNAVAILABLE"));
        assertTrue(result.hasError("FIELD_UNAVAILABLE"));
    }

    @Test
    @DisplayName("recursive subprocess and unavailable capability bridge are rejected")
    void recursiveSubprocessAndUnavailableCapabilityAreRejected() {
        BusinessProcessSchema subProcess = validator.normalize(subProcessSchema());
        BusinessProcessValidationContext subProcessContext = validContext()
                .setPublishedSubProcessCodes(Set.of("child_process"))
                .setSubProcessDependencies(Map.of("child_process", Set.of("order_created_update")));
        BusinessProcessValidationVO recursion = validator.validate(subProcess, subProcessContext);

        BusinessProcessSchema capability = validator.normalize(capabilitySchema());
        BusinessProcessValidationContext capabilityContext = validContext()
                .setAvailableCapabilityCodes(Set.of("order.sync"))
                .setCapabilityBridgeAvailable(false);
        BusinessProcessValidationVO unavailable = validator.validate(capability, capabilityContext);

        assertTrue(recursion.hasError("SUB_PROCESS_RECURSION"));
        assertTrue(unavailable.hasError("CAPABILITY_BRIDGE_UNAVAILABLE"));
    }

    private BusinessProcessValidationContext validContext() {
        return new BusinessProcessValidationContext()
                .setExpectedProcessCode("order_created_update")
                .setObjectIdsByCode(Map.of("order", "1900000000000001001"))
                .setPublishedObjectVersionIdsByCode(Map.of("order", "2900000000000001001"))
                .setFieldsByObjectCode(Map.of("order", Set.of("id", "status")))
                .setKnownPermissions(Set.of("ai:businessProcess:start"));
    }

    private BusinessProcessValidationContext frozenExampleContext() {
        return new BusinessProcessValidationContext()
                .setObjectIdsByCode(Map.of("sample_purchase_order", "1900000000000001001"))
                .setPublishedObjectVersionIdsByCode(
                        Map.of("sample_purchase_order", "2900000000000001001"))
                .setFieldsByObjectCode(Map.of("sample_purchase_order", Set.of(
                        "id", "status", "orderNo", "title", "amountCent", "autoSubmit",
                        "applicantId", "expectedArrivalDate", "ownerId")))
                .setAvailableFlowModelKeys(Set.of("sample_purchase_order_approval"))
                .setAvailableFormAssetKeys(Set.of("sample_purchase_order_approval_form"))
                .setAvailableMessageTemplateCodes(Set.of(
                        "purchase_approval_approved", "purchase_overdue_notice", "purchase_due_notice"))
                .setKnownPermissions(Set.of("ai:businessProcess:start"));
    }

    private String validSchema() {
        return """
                {
                  "schemaVersion":"1.0",
                  "processCode":"order_created_update",
                  "subject":{"objectId":"1900000000000001001","objectCode":"order","objectVersionId":null,"recordIdSource":"EVENT_RECORD"},
                  "nodes":[
                    {"id":"start","type":"START_EVENT","name":"订单新增","config":{"eventType":"RECORD_CREATED","condition":{"operator":"AND","rules":[{"source":"record","field":"status","operator":"EQ","value":"DRAFT"}]}}},
                    {"id":"update","type":"ACTION","name":"更新状态","config":{"actionType":"UPDATE_RECORD","objectCode":"order","fieldMappings":[{"field":"status","valueSource":"CONSTANT","value":"OPEN"}]}},
                    {"id":"end","type":"END","name":"完成","config":{"result":"SUCCESS"}}
                  ],
                  "edges":[
                    {"id":"e1","source":"start","target":"update","sourcePort":"NEXT"},
                    {"id":"e2","source":"update","target":"end","sourcePort":"NEXT"}
                  ],
                  "policies":{"approvalConcurrency":"ONE_ACTIVE_PER_BUSINESS_KEY","maxSubProcessDepth":5,"retry":{"mode":"LIMITED","maxAttempts":3,"backoffSeconds":[30,120,600]}},
                  "dependencies":{"objects":["order"],"flowModels":[],"formAssets":[],"businessActions":[],"messageTemplates":[],"capabilities":[],"subProcesses":[]}
                }
                """;
    }

    private String reorderedValidSchema() {
        return """
                {"processCode":"order_created_update","schemaVersion":"1.0",
                 "subject":{"recordIdSource":"EVENT_RECORD","objectVersionId":null,"objectCode":"order","objectId":"1900000000000001001"},
                 "nodes":[
                   {"config":{"result":"SUCCESS"},"name":"完成","type":"END","id":"end"},
                   {"name":"更新状态","type":"ACTION","id":"update","config":{"fieldMappings":[{"value":"OPEN","valueSource":"CONSTANT","field":"status"}],"objectCode":"order","actionType":"UPDATE_RECORD"}},
                   {"name":"订单新增","id":"start","type":"START_EVENT","config":{"condition":{"rules":[{"value":"DRAFT","operator":"EQ","field":"status","source":"record"}],"operator":"AND"},"eventType":"RECORD_CREATED"}}
                 ],
                 "edges":[
                   {"target":"end","id":"e2","sourcePort":"NEXT","source":"update"},
                   {"sourcePort":"NEXT","target":"update","source":"start","id":"e1"}
                 ],
                 "dependencies":{"subProcesses":[],"capabilities":[],"messageTemplates":[],"businessActions":[],"formAssets":[],"flowModels":[],"objects":["order"]},
                 "policies":{"retry":{"backoffSeconds":[30,120,600],"maxAttempts":3,"mode":"LIMITED"},"maxSubProcessDepth":5,"approvalConcurrency":"ONE_ACTIVE_PER_BUSINESS_KEY"}}
                """;
    }

    private String subProcessSchema() {
        return validSchema()
                .replace("{\"id\":\"update\",\"type\":\"ACTION\",\"name\":\"更新状态\",\"config\":{\"actionType\":\"UPDATE_RECORD\",\"objectCode\":\"order\",\"fieldMappings\":[{\"field\":\"status\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}]}}",
                        "{\"id\":\"update\",\"type\":\"SUB_PROCESS\",\"name\":\"调用子流程\",\"config\":{\"processCode\":\"child_process\"}}")
                .replace("\"subProcesses\":[]", "\"subProcesses\":[\"child_process\"]");
    }

    private String capabilitySchema() {
        return validSchema()
                .replace("\"actionType\":\"UPDATE_RECORD\",\"objectCode\":\"order\",\"fieldMappings\":[{\"field\":\"status\",\"valueSource\":\"CONSTANT\",\"value\":\"OPEN\"}]",
                        "\"actionType\":\"INVOKE_CAPABILITY\",\"capabilityCode\":\"order.sync\"")
                .replace("\"capabilities\":[]", "\"capabilities\":[\"order.sync\"]");
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "找不到测试资源: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
