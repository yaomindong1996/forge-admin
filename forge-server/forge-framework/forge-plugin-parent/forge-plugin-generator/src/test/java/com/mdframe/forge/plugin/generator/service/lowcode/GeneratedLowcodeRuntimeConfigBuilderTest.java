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

@DisplayName("所有下载入口的低代码运行配置规范化")
class GeneratedLowcodeRuntimeConfigBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeneratedLowcodeRuntimeConfigBuilder builder =
            new GeneratedLowcodeRuntimeConfigBuilder(objectMapper);

    @Test
    @DisplayName("普通配置键派生下载快照键并保留未来协议字段")
    void derivesGeneratedKeyAndPreservesFutureFields() throws Exception {
        AiCrudConfig source = validConfig();
        source.setModelSchema("""
                {"schemaVersion":2,"fields":[],
                 "futureRelation":{"loader":{"api":"get@/ai/crud/sales_order/tree"}}}
                """);
        source.setOptions("""
                {"treeConfig":{"source":{"api":"get@/ai/crud/sales_order/tree"}},
                 "enableCustomQuery":true,
                 "futureOption":{"nested":{"enabled":true}},
                 "rowActions":[{"targetUrl":"/ai/crud-page/sales_order?mode=create"}]}
                """);

        AiCrudConfig generated = builder.build(source, "/sales/order");
        JsonNode modelSchema = objectMapper.readTree(generated.getModelSchema());
        JsonNode options = objectMapper.readTree(generated.getOptions());
        JsonNode apiConfig = objectMapper.readTree(generated.getApiConfig());

        assertEquals("sales_order", source.getConfigKey());
        assertEquals("generated_sales_order", generated.getConfigKey());
        assertEquals("sales_order", options.at("/codegen/sourceConfigKey").asText());
        assertEquals("generated_sales_order", options.at("/codegen/runtimeConfigKey").asText());
        assertEquals("get@/sales/order/tree", apiConfig.get("tree").asText());
        assertEquals("post@/sales/order/getById", apiConfig.get("detail").asText());
        assertFalse(apiConfig.has("exportTask"));
        assertFalse(apiConfig.has("exportTasks"));
        assertEquals("STATIC_MYBATIS_PLUS", options.at("/codegen/backendMode").asText());
        assertFalse(options.get("enableCustomQuery").asBoolean());
        assertTrue(options.at("/codegen/sourceEnableCustomQuery").asBoolean());
        assertTrue(options.at("/codegen/staticRuntimeOverrides/enableCustomQuery").isBoolean());
        assertEquals("get@/sales/order/tree",
                modelSchema.at("/futureRelation/loader/api").asText());
        assertEquals("/sales/order?mode=create",
                options.at("/rowActions/0/targetUrl").asText());
        assertTrue(options.at("/futureOption/nested/enabled").asBoolean());
        assertFalse(generated.getModelSchema().contains("/ai/crud/"));
    }

    @Test
    @DisplayName("已生成配置键保持稳定避免应用级入口二次改名")
    void keepsExistingGeneratedKey() {
        AiCrudConfig source = validConfig();
        source.setConfigKey("generated_procurement_order");
        source.setOptions("{\"codegen\":{\"sourceConfigKey\":\"sales_order\"}}");

        AiCrudConfig generated = builder.build(source, "/procurement/order");

        assertEquals("generated_procurement_order", generated.getConfigKey());
    }

    @Test
    @DisplayName("无法改写的平台通用接口失败关闭")
    void rejectsUnresolvedGenericRuntimeApi() {
        AiCrudConfig source = validConfig();
        source.setPageSchema("""
                {"layoutType":"simple-crud",
                 "external":{"api":"get@/ai/crud/sales_order_archive/page"}}
                """);

        BusinessException error = assertThrows(BusinessException.class,
                () -> builder.build(source, "/sales/order"));

        assertTrue(error.getMessage().contains("平台通用运行接口"));
    }

    private AiCrudConfig validConfig() {
        AiCrudConfig config = new AiCrudConfig();
        config.setConfigKey("sales_order");
        config.setTableName("biz_order");
        config.setTableComment("销售订单");
        config.setLayoutType("simple-crud");
        config.setModelSchema("{\"schemaVersion\":2,\"fields\":[]}");
        config.setPageSchema("{\"layoutType\":\"simple-crud\",\"zones\":[]}");
        config.setSearchSchema("[]");
        config.setColumnsSchema("[]");
        config.setEditSchema("[]");
        config.setApiConfig("{\"list\":\"get@/ai/crud/sales_order/page\"}");
        config.setOptions("{}");
        config.setDictConfig("[]");
        config.setDesensitizeConfig("{}");
        config.setEncryptConfig("{}");
        config.setTransConfig("{}");
        return config;
    }
}
