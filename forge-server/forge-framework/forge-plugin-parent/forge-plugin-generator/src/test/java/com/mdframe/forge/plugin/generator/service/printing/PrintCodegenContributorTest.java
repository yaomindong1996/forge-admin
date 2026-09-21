package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.*;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeProtocolSnapshotBuilder;
import com.mdframe.forge.plugin.print.entity.*;
import com.mdframe.forge.plugin.print.mapper.*;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.*;

class PrintCodegenContributorTest {
    final PrintIdentity identity = mock(PrintIdentity.class);
    final PrintApplicationAccessAdapter access = mock(PrintApplicationAccessAdapter.class);
    final BusinessApplicationVersionMapper applications = mock(BusinessApplicationVersionMapper.class);
    final PrintTemplateMapper templates = mock(PrintTemplateMapper.class);
    final PrintTemplateVersionMapper versions = mock(PrintTemplateVersionMapper.class);
    ValidatorFactory factory; PrintCodegenContributor service; AiCrudConfig config; PrintTemplateVersion version;
    @BeforeEach void setup() throws Exception {
        factory = Validation.buildDefaultValidatorFactory(); when(identity.current()).thenReturn(ACTOR);
        var root = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(PrintLowcodeTestData.snapshot());
        root.set("printing", JSON.readTree(PrintApplicationTestData.snapshot(binding(10, true))).path("printing"));
        var app = new AiBusinessApplicationVersion(); app.setId(100L); app.setApplicationId(2L); app.setVersionNo(1); app.setSnapshotJson(root.toString());
        when(applications.selectPublishedPrintSources(1L, "purchase", null)).thenReturn(List.of(app));
        var template = new PrintTemplate(); template.setApplicationId(2L); template.setSourceType("LOWCODE"); template.setPageId("page_purchase");
        template.setObjectCode("purchase"); template.setSourceKey(SOURCE.key()); template.setStatus(1); template.setTemplateCode("purchase_print"); template.setTemplateName("合成采购单");
        template.setPublishedVersionId(999L); template.setDraftSchema("新草稿不能进入导出");
        when(templates.selectScoped(1L, 10L)).thenReturn(template);
        version = new PrintTemplateVersion(); version.setId(20L); version.setVersionNo(1); version.setSchemaJson(SCHEMA); version.setSchemaHash(HASH);
        when(versions.selectScoped(1L, 10L, 20L)).thenReturn(version);
        config = new AiCrudConfig(); config.setConfigKey("generated_purchase"); config.setBuildMode("LOWCODE");
        config.setModelSchema("{\"fields\":[]}"); config.setPageSchema("{}"); config.setApiConfig("{}");
        config.setOptions("{\"codegen\":{\"sourceConfigKey\":\"purchase\"}}");
        service = new PrintCodegenContributor(identity, access, applications, new PrintApplicationSnapshotCodec(factory.getValidator()),
                new LowcodePrintSourceResolver(), templates, versions, new PrintProtocolValidator(), JSON);
    }
    @AfterEach void close() { factory.close(); }
    @Test void exportsPinnedDefinitionAndStringIdsThroughSharedProtocolBuilder() throws Exception {
        var artifacts = new LowcodeProtocolSnapshotBuilder(JSON, service).build(config);
        var frontend = JSON.readTree(artifacts.frontendRuntimeConfig()); var app = frontend.at("/printing/applications/0");
        assertThat(app.path("applicationVersionId").asText()).isEqualTo("100");
        assertThat(app.at("/bindings/0/templateVersionId").isTextual()).isTrue();
        assertThat(app.at("/templates/0/templateVersionId").asText()).isEqualTo("20");
        assertThat(app.at("/templates/0/schema/body/0/binding/value").asText()).isEqualTo("合成单据");
        assertThat(JSON.readTree(artifacts.protocolSnapshot()).at("/runtimeConfig/printing")).isEqualTo(frontend.path("printing"));
        assertThat(artifacts.coverageReport()).contains("PINNED_PROTOCOL_EXPORTED", "REQUIRES_EXTENSION");
        verify(access).authorize(ACTOR, 2L, PrintDesignAction.VIEW);
        verify(versions, never()).selectScoped(1L, 10L, 999L);
    }
    @Test void missingVersionOrChangedHashBlocksEntireExport() {
        version.setSchemaHash("0".repeat(64)); assertThatThrownBy(() -> service.contribute(config)).hasMessageContaining("不完整");
        when(versions.selectScoped(1L, 10L, 20L)).thenReturn(null);
        assertThatThrownBy(() -> service.contribute(config)).hasMessageContaining("不完整");
    }
    @Test void storedHashCannotHideCorruptedSchema() {
        version.setSchemaJson(SCHEMA.replace("合成单据", "内容损坏"));
        assertThatThrownBy(() -> service.contribute(config)).hasMessageContaining("不完整");
    }
    @Test void deniedApplicationCannotLeakTemplateDefinitions() {
        doThrow(new com.mdframe.forge.starter.core.exception.BusinessException("拒绝")).when(access).authorize(ACTOR, 2L, PrintDesignAction.VIEW);
        assertThatThrownBy(() -> service.contribute(config)).hasMessageContaining("拒绝");
        verifyNoInteractions(templates, versions);
    }
    @Test void applicationContextNarrowsQueryAndLegacyNoBindingsExportsExplicitEmptyManifest() {
        config.setOptions("{\"codegen\":{\"sourceConfigKey\":\"purchase\",\"printApplicationId\":\"2\"}}");
        var result = service.contribute(config);
        assertThat((List<?>) result.get("applications")).isEmpty();
        verify(applications).selectPublishedPrintSources(1L, "purchase", 2L);
        verifyNoInteractions(access, templates, versions);
    }
    @Test void malformedApplicationIdentityFailsBeforeLookup() {
        config.setOptions("{\"codegen\":{\"printApplicationId\":\"900719925474099399999999999\"}}");
        assertThatThrownBy(() -> service.contribute(config)).hasMessageContaining("不完整");
        verifyNoInteractions(applications);
    }
    @Test void actualVelocityPackageContainsSamePrintingProtocolAndSharedFrontend() throws Exception {
        config.setTableName("test_purchase"); config.setTableComment("合成采购单"); config.setLayoutType("simple-crud");
        config.setPrimaryKeyField("id"); config.setPrimaryKeyColumn("id");
        config.setModelSchema("{\"fields\":[{\"field\":\"id\",\"columnName\":\"id\",\"dataType\":\"bigint\",\"primaryKey\":true}]}");
        config.setOptions("{\"codegen\":{\"sourceConfigKey\":\"purchase\",\"businessApiBase\":\"/sample/purchase\"},\"includeBackend\":false,\"includeSql\":false}");
        var builder = new LowcodeProtocolSnapshotBuilder(JSON, service);
        var strategy = new com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy(
                mock(com.mdframe.forge.plugin.generator.mapper.GenTableColumnMapper.class), JSON,
                new com.mdframe.forge.plugin.generator.service.lowcode.GeneratedLowcodeRuntimeConfigBuilder(JSON), builder,
                mock(org.springframework.beans.factory.ObjectProvider.class));
        var files = strategy.generate(config, null);
        var runtime = files.entrySet().stream().filter(e -> e.getKey().endsWith("/runtime-config.json")).findFirst().orElseThrow().getValue();
        var manifest = files.entrySet().stream().filter(e -> e.getKey().endsWith("-printing.json")).findFirst().orElseThrow().getValue();
        assertThat(JSON.readTree(manifest)).isEqualTo(JSON.readTree(runtime).path("printing"));
        assertThat(files.get("PRINTING.md")).contains("PrintDataProvider", "文件", "固定绑定");
        assertThat(files.values().stream().filter(v -> v.contains("LowcodeRuntimePage")).findFirst().orElseThrow()).contains("runtime-config.json");
        assertThat(files.values()).noneMatch(value -> value.contains("/ai/crud/"));
    }

    @Test void currentSnapshotDiscoverySqlBindsTenantConfigAndOptionalApplication() throws Exception {
        var configuration = new org.apache.ibatis.session.Configuration();
        try (var input = getClass().getResourceAsStream("/mapper/BusinessApplicationVersionMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, configuration, "print-export", configuration.getSqlFragments()).parse();
        }
        var parameters = new HashMap<String, Object>(); parameters.put("tenantId", 1L); parameters.put("configKey", "purchase"); parameters.put("applicationId", 2L);
        var bound = configuration.getMappedStatement(BusinessApplicationVersionMapper.class.getName() + ".selectPublishedPrintSources").getBoundSql(parameters);
        String sql = bound.getSql().replaceAll("\\s+", " ");
        assertThat(net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql)).isNotNull();
        assertThat(sql).contains("v.tenant_id = ?", "a.tenant_id = v.tenant_id", "v.del_flag = '0'", "a.del_flag = '0'", "v.version_no = a.last_publish_version", "JSON_OBJECT('configKey', ?)", "AND a.id = ?");
        assertThat(bound.getParameterMappings()).extracting(org.apache.ibatis.mapping.ParameterMapping::getProperty).containsExactly("tenantId", "configKey", "applicationId");
    }

    @Test void applicationManifestRetainsTemplatesForAggregatedChildObjects() throws Exception {
        var application = applications.selectPublishedPrintSources(1L, "purchase", null).get(0);
        var root = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(application.getSnapshotJson());
        ((com.fasterxml.jackson.databind.node.ArrayNode) root.at("/application/options/inAppBuilder/nodes")).addObject()
                .put("id", "page_item").put("type", "page").putObject("objectRef")
                .put("objectId", "4").put("objectCode", "item").put("configKey", "item");
        var source = new com.mdframe.forge.plugin.print.spi.PrintSourceRequest(2L, com.mdframe.forge.plugin.print.enums.PrintSourceType.LOWCODE, "page_item", null, "item");
        var binding = new PrintApplicationSnapshotCodec.Binding(source, com.mdframe.forge.plugin.print.enums.PrintScene.DETAIL, 11L, 21L, HASH, true, 0);
        ((com.fasterxml.jackson.databind.node.ArrayNode) root.at("/printing/bindings")).add(JSON.valueToTree(binding));
        application.setSnapshotJson(root.toString());
        when(applications.selectPublishedPrintSources(1L, "purchase", 2L)).thenReturn(List.of(application));
        when(applications.selectPublishedPrintSources(1L, "item", 2L)).thenReturn(List.of(application));
        var child = new PrintTemplate(); child.setApplicationId(2L); child.setSourceType("LOWCODE"); child.setPageId("page_item");
        child.setObjectCode("item"); child.setSourceKey(source.key()); child.setStatus(1); child.setTemplateCode("item_print"); child.setTemplateName("合成子对象");
        when(templates.selectScoped(1L, 11L)).thenReturn(child); when(versions.selectScoped(1L, 11L, 21L)).thenReturn(version);
        var result = JSON.valueToTree(service.contributeApplication(2L, List.of("purchase", "item")));
        assertThat(result.path("objects").size()).isEqualTo(2);
        assertThat(result.at("/objects/1/printing/applications/0/bindings/0/source/objectCode").asText()).isEqualTo("item");
        assertThat(result.at("/objects/1/printing/applications/0/templates/0/templateId").asText()).isEqualTo("11");
    }

}
