package com.mdframe.forge.plugin.capability.controlplane.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityMapper;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityVersionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapabilityOpenApiDocumentServiceTest {

    @Test
    void shouldInstantiateThroughSpringConstructorInjection() {
        new ApplicationContextRunner()
                .withUserConfiguration(DocumentServiceConfiguration.class)
                .withBean(AiCapabilityMapper.class, () -> mock(AiCapabilityMapper.class))
                .withBean(AiCapabilityVersionMapper.class,
                        () -> mock(AiCapabilityVersionMapper.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues(
                        "forge.capability.identity.openapi-resource=https://forge.example.com/openapi")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CapabilityOpenApiDocumentService.class);
                });
    }

    @Test
    void shouldGenerateUserOnlyFlowActionOpenApiDocument() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiCapabilityMapper capabilityMapper = mock(AiCapabilityMapper.class);
        AiCapabilityVersionMapper versionMapper = mock(AiCapabilityVersionMapper.class);
        CapabilityOpenApiDocumentService service = new CapabilityOpenApiDocumentService(
                capabilityMapper, versionMapper, objectMapper);
        AiCapability capability = new AiCapability();
        capability.setId(10L);
        capability.setCapabilityCode("flow.order.start");
        capability.setCapabilityName("订单流程启动");
        capability.setDescription("启动订单审批流程");
        capability.setCurrentVersion("1.0.0");
        capability.setPublishStatus("PUBLISHED");
        capability.setEnabled(1);
        AiCapabilityVersion version = new AiCapabilityVersion();
        version.setVersion("1.0.0");
        version.setStatus("PUBLISHED");
        version.setBehavior("FLOW");
        version.setSourceType("FLOW_ACTION");
        version.setRequiredActorType("USER");
        version.setSchemaChecksum("sha256-checksum");
        version.setInputSchema("{\"type\":\"object\",\"properties\":{\"recordId\":{\"type\":\"string\"}}}");
        version.setOutputSchema("{\"type\":\"object\",\"properties\":{\"processInstanceId\":{\"type\":\"string\"}}}");
        when(capabilityMapper.selectTenantById(1L, 10L)).thenReturn(capability);
        when(versionMapper.selectVersion(1L, 10L, "1.0.0")).thenReturn(version);

        CapabilityOpenApiDocumentService.CapabilityOpenApiDocument document =
                service.generate(1L, 10L);
        JsonNode json = objectMapper.readTree(document.content());

        assertThat(document.filename()).isEqualTo("flow.order.start-1.0.0-openapi.json");
        assertThat(json.path("openapi").asText()).isEqualTo("3.1.0");
        JsonNode operation = json.path("paths")
                .path("/openapi/v1/capabilities/flow.order.start/invoke").path("post");
        assertThat(operation.path("security").toString()).contains("bearerAuth");
        assertThat(operation.path("security").toString()).doesNotContain("hmacSignature");
        assertThat(operation.path("parameters").get(0).path("required").asBoolean()).isTrue();
        JsonNode responseCodes = json.at("/components/schemas/CapabilityResponse/properties/code/enum");
        assertThat(responseCodes).hasSize(10);
        assertThat(responseCodes.toString())
                .contains("SUCCESS", "UNAUTHORIZED", "FORBIDDEN", "RESOURCE_NOT_FOUND",
                        "SCHEMA_INVALID", "CONFLICT");
        assertThat(json.path("x-forge-required-actor-type").asText()).isEqualTo("USER");
    }

    @Test
    void shouldGenerateDetailedMarkdownFromImmutablePublishedContract() {
        ObjectMapper objectMapper = new ObjectMapper();
        AiCapabilityMapper capabilityMapper = mock(AiCapabilityMapper.class);
        AiCapabilityVersionMapper versionMapper = mock(AiCapabilityVersionMapper.class);
        CapabilityOpenApiDocumentService service = new CapabilityOpenApiDocumentService(
                capabilityMapper, versionMapper, objectMapper, "https://forge.example.com/openapi");
        AiCapability capability = new AiCapability();
        capability.setId(10L);
        capability.setCapabilityCode("system.flow.process.start.invoice_approval");
        capability.setCapabilityName("启动发票审批");
        capability.setDescription("供 ERP 发起已发布发票审批流程");
        capability.setCurrentVersion("1.0.0");
        capability.setPublishStatus("PUBLISHED");
        capability.setEnabled(1);
        AiCapabilityVersion version = new AiCapabilityVersion();
        version.setVersion("1.0.0");
        version.setStatus("PUBLISHED");
        version.setBehavior("ACTION");
        version.setRiskLevel("MEDIUM");
        version.setSourceType("SYSTEM_SERVICE");
        version.setRequiredActorType("USER");
        version.setSchemaChecksum("sha256-checksum");
        version.setInputSchema("""
                {"type":"object","additionalProperties":false,
                 "properties":{
                   "businessKey":{"type":"string","maxLength":128,"description":"外围业务唯一键"},
                   "variables":{"type":"object","additionalProperties":false,"properties":{
                     "amount":{"type":"number","description":"发票金额，单位元"}}},
                   "idempotencyKey":{"type":"string","description":"旧版 Body 幂等字段"}},
                 "required":["businessKey","variables","idempotencyKey"]}
                """);
        version.setOutputSchema("""
                {"type":"object","properties":{
                  "processInstanceId":{"type":"string","description":"流程实例 ID"},
                  "correlationId":{"type":"string","description":"调用追踪标识"}},
                 "required":["processInstanceId","correlationId"]}
                """);
        version.setPolicySnapshot("""
                {"permission":"ai:businessFlow:start",
                 "platformPermission":"ai:capability:flow-action:invoke",
                 "documentation":{
                   "businessRules":["流程模型和部署信息在发布时固定。"],
                   "requestNotes":["variables 只能包含发布版本允许的变量。"],
                   "responseNotes":["排障时请提供 correlationId。"]}}
                """);
        when(capabilityMapper.selectTenantById(1L, 10L)).thenReturn(capability);
        when(versionMapper.selectVersion(1L, 10L, "1.0.0")).thenReturn(version);

        CapabilityOpenApiDocumentService.CapabilityMarkdownDocument document =
                service.generateMarkdown(1L, 10L);
        String markdown = new String(document.content(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(document.filename())
                .isEqualTo("system.flow.process.start.invoice_approval-1.0.0-调用指南.md");
        assertThat(markdown)
                .contains("## 认证方式", "## 请求参数", "## 返回参数", "## 业务校验与权限")
                .contains("`body.businessKey`", "`body.variables.amount`", "发票金额，单位元")
                .contains("`data.processInstanceId`", "流程实例 ID")
                .contains("流程模型和部署信息在发布时固定。")
                .contains("ai:capability:flow-action:invoke", "ai:businessFlow:start")
                .contains("urn:ietf:params:oauth:grant-type:token-exchange")
                .contains("<CLIENT_SECRET>", "<TRUSTED_OIDC_JWT>")
                .contains("UNAUTHORIZED", "SCHEMA_INVALID", "CONFLICT")
                .doesNotContain("body.idempotencyKey", "真实-client-secret", "Bearer eyJ");
    }

    @Configuration(proxyBeanMethods = false)
    @Import(CapabilityOpenApiDocumentService.class)
    static class DocumentServiceConfiguration {
    }
}
