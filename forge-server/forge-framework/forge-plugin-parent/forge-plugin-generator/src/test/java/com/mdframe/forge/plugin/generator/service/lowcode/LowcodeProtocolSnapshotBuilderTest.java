package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("低代码代码包协议快照")
class LowcodeProtocolSnapshotBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LowcodeProtocolSnapshotBuilder builder = new LowcodeProtocolSnapshotBuilder(objectMapper, org.mockito.Mockito.mock(com.mdframe.forge.plugin.generator.service.printing.PrintCodegenContributor.class));

    @Test
    @DisplayName("未来新增的嵌套协议字段无需模板白名单即可原样保留")
    void preservesFutureNestedProtocolFields() throws Exception {
        AiCrudConfig config = validConfig();
        config.setModelSchema("""
                {"schemaVersion":2,"tableName":"biz_order","fields":[],
                 "futureFeature":{"nested":{"enabled":true,"mode":"NEXT"}}}
                """);
        config.setPageSchema("""
                {"layoutType":"simple-crud","zones":[],
                 "futurePageCapability":{"panels":[{"type":"timeline"}]}}
                """);
        config.setOptions("""
                {"formDesignerSchema":{"forms":[]},
                 "futureRuntimeOption":{"strategy":"automatic"},
                 "codegen":{"sourceConfigKey":"order_manage"}}
                """);

        LowcodeProtocolSnapshotBuilder.ProtocolArtifacts artifacts = builder.build(config);
        JsonNode frontend = objectMapper.readTree(artifacts.frontendRuntimeConfig());
        JsonNode protocol = objectMapper.readTree(artifacts.protocolSnapshot());
        JsonNode coverage = objectMapper.readTree(artifacts.coverageReport());

        assertTrue(frontend.at("/modelSchema/futureFeature/nested/enabled").asBoolean());
        assertEquals("timeline", frontend.at("/pageSchema/futurePageCapability/panels/0/type").asText());
        assertEquals("automatic", frontend.at("/options/futureRuntimeOption/strategy").asText());
        assertEquals("shared-frontend-static-backend-compiler",
                protocol.at("/autoAdaptation/strategy").asText());
        assertEquals("order_manage", protocol.get("sourceConfigKey").asText());
        assertEquals("STATIC_COMPILED", coverage.get("status").asText());
        assertFalse(coverage.get("futureCompatibility").get("downloadEntryChangeRequired").asBoolean());
        assertTrue(coverage.get("unsupported").isEmpty());
        assertTrue(coverage.get("requiresExtension").isEmpty());
    }

    @Test
    @DisplayName("字段查询事件随表单发布快照完整保留并声明运行时覆盖")
    void preservesManagedFieldEvents() throws Exception {
        AiCrudConfig config = validConfig();
        config.setOptions("""
                {"formDesignerSchema":{"settings":{"governance":{"fieldEvents":[{
                  "id":"query_contact","trigger":"BLUR","sourceField":"mobile",
                  "sourceType":"EXTERNAL_API","sourceKey":"crm/contact_lookup",
                  "debounceMs":0,"paramMappings":[{"param":"mobile","source":"FORM_FIELD","field":"mobile"}],
                  "resultMode":"ROOT","resultMappings":[{"from":"contact.name","to":"contactName","whenMissing":"CLEAR"}]
                }]}}}}
                """);

        LowcodeProtocolSnapshotBuilder.ProtocolArtifacts artifacts = builder.build(config);
        JsonNode frontend = objectMapper.readTree(artifacts.frontendRuntimeConfig());
        JsonNode coverage = objectMapper.readTree(artifacts.coverageReport());

        assertEquals("query_contact", frontend.at(
                "/options/formDesignerSchema/settings/governance/fieldEvents/0/id").asText());
        assertTrue(coverage.get("capabilities").toString().contains(
                "/options/formDesignerSchema/settings/governance/fieldEvents"));
        assertTrue(coverage.get("capabilities").toString().contains("QUERY_SOURCE_EVENT_COMPILED"));
    }

    @Test
    @DisplayName("离线草稿治理配置随表单发布快照保留并声明共享运行时覆盖")
    void preservesOfflineDraftGovernance() throws Exception {
        AiCrudConfig config = validConfig();
        config.setOptions("""
                {"formDesignerSchema":{"settings":{"governance":{"offlineDraft":{
                  "enabled":true,"formCode":"presale_form",
                  "replayActionCode":"submit_presale","recordVersionField":"updateTime"
                }}}}}
                """);

        LowcodeProtocolSnapshotBuilder.ProtocolArtifacts artifacts = builder.build(config);
        JsonNode frontend = objectMapper.readTree(artifacts.frontendRuntimeConfig());
        JsonNode coverage = objectMapper.readTree(artifacts.coverageReport());

        assertTrue(frontend.at(
                "/options/formDesignerSchema/settings/governance/offlineDraft/enabled").asBoolean());
        assertEquals("submit_presale", frontend.at(
                "/options/formDesignerSchema/settings/governance/offlineDraft/replayActionCode").asText());
        assertTrue(coverage.get("capabilities").toString().contains(
                "/options/formDesignerSchema/settings/governance/offlineDraft"));
        assertTrue(coverage.get("capabilities").toString().contains(
                "OFFLINE_DRAFT_GOVERNANCE_PRESERVED"));
    }

    @Test
    @DisplayName("低代码配置缺少权威模型或页面协议时失败关闭")
    void rejectsMissingAuthoritativeSchema() {
        AiCrudConfig config = validConfig();
        config.setPageSchema(null);

        BusinessException error = assertThrows(BusinessException.class, () -> builder.build(config));

        assertTrue(error.getMessage().contains("modelSchema 或 pageSchema"));
    }

    @Test
    @DisplayName("协议字段类型不正确时不返回不完整快照")
    void rejectsInvalidProtocolShape() {
        AiCrudConfig config = validConfig();
        config.setColumnsSchema("{}");

        BusinessException error = assertThrows(BusinessException.class, () -> builder.build(config));

        assertTrue(error.getMessage().contains("columnsSchema 必须是 JSON 数组"));
    }

    @Test
    @DisplayName("静态后端未内建的公式和组合查询明确标记业务扩展")
    void reportsStaticBackendExtensionRequirements() throws Exception {
        AiCrudConfig config = validConfig();
        config.setModelSchema("""
                {"schemaVersion":2,"tableName":"biz_order",
                 "fields":[{"field":"totalAmount","columnName":"total_amount",
                            "formulaConfig":{"mode":"STORED","expression":"price*quantity"}}]}
                """);
        config.setOptions("""
                {"enableCustomQuery":false,
                 "codegen":{"sourceConfigKey":"sales_order","sourceEnableCustomQuery":true}}
                """);

        JsonNode coverage = objectMapper.readTree(builder.build(config).coverageReport());

        assertEquals("STATIC_COMPILED_WITH_EXTENSIONS", coverage.get("status").asText());
        assertEquals(2, coverage.get("requiresExtension").size());
        assertEquals("REQUIRES_EXTENSION",
                coverage.at("/requiresExtension/0/status").asText());
    }

    private AiCrudConfig validConfig() {
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("generated_sales_order");
        config.setTableName("biz_order");
        config.setTableComment("销售订单");
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        config.setPublishStatus("PUBLISHED");
        config.setLayoutType("simple-crud");
        config.setModelSchema("{\"schemaVersion\":2,\"tableName\":\"biz_order\",\"fields\":[]}");
        config.setPageSchema("{\"layoutType\":\"simple-crud\",\"zones\":[]}");
        config.setSearchSchema("[]");
        config.setColumnsSchema("[]");
        config.setEditSchema("[]");
        config.setApiConfig("{\"list\":\"get@/sales/order/page\"}");
        config.setOptions("{}");
        config.setDictConfig("[]");
        config.setDesensitizeConfig("{}");
        config.setEncryptConfig("{}");
        config.setTransConfig("{}");
        return config;
    }
}
