package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/** 所有下载入口共同调用；缺失定义/权限/hash 时阻断，不能静默丢失打印协议。 */
@Component
@RequiredArgsConstructor
public class PrintCodegenContributor {
    private final PrintIdentity identity;
    private final PrintApplicationAccessAdapter access;
    private final BusinessApplicationVersionMapper applications;
    private final PrintApplicationSnapshotCodec snapshots;
    private final LowcodePrintSourceResolver sources;
    private final PrintTemplateMapper templates;
    private final PrintTemplateVersionMapper versions;
    private final PrintProtocolValidator protocol;
    private final ObjectMapper json;

    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Map<String, Object> contribute(AiCrudConfig config) {
        var actor = identity.current();
        var options = parse(config.getOptions() == null ? "{}" : config.getOptions());
        var codegen = options.path("codegen");
        String sourceKey = codegen.path("sourceConfigKey").asText(config.getConfigKey());
        Long applicationId = null;
        if (codegen.has("printApplicationId")) {
            String value = codegen.path("printApplicationId").asText();
            if (!value.matches("[1-9][0-9]*")) throw invalid();
            try { applicationId = Long.valueOf(value); } catch (NumberFormatException ex) { throw invalid(); }
        }
        List<Map<String, Object>> exported = new ArrayList<>();
        for (var application : applications.selectPublishedPrintSources(actor.tenantId(), sourceKey, applicationId)) {
            var root = parse(application.getSnapshotJson());
            List<PrintApplicationSnapshotCodec.Binding> bindings = new ArrayList<>();
            for (var binding : snapshots.read(application.getSnapshotJson(), application.getApplicationId())) {
                var object = sources.object(root, binding.source());
                if (sourceKey.equals(object.path("configKey").asText())) bindings.add(binding);
            }
            if (bindings.isEmpty()) continue;
            access.authorize(actor, application.getApplicationId(), PrintDesignAction.VIEW);
            Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
            List<Map<String, Object>> exportedBindings = new ArrayList<>();
            for (var binding : bindings) {
                var template = templates.selectScoped(actor.tenantId(), binding.templateId());
                if (template == null || !EnableStatus.ENABLED.matches(template.getStatus())
                        || !binding.source().equals(PrintSourceRequest.from(template))
                        || !binding.source().key().equals(template.getSourceKey())) throw invalid();
                var version = versions.selectScoped(actor.tenantId(), binding.templateId(), binding.templateVersionId());
                if (version == null || !binding.schemaHash().equals(version.getSchemaHash())) throw invalid();
                var validated = protocol.validate(version.getSchemaJson());
                if (!binding.schemaHash().equals(validated.schemaHash())) throw invalid();
                String key = binding.templateId() + ":" + binding.templateVersionId();
                definitions.putIfAbsent(key, Map.of("templateId", String.valueOf(binding.templateId()),
                        "templateVersionId", String.valueOf(binding.templateVersionId()),
                        "templateCode", template.getTemplateCode(), "templateName", template.getTemplateName(),
                        "versionNo", version.getVersionNo(), "schemaHash", binding.schemaHash(),
                        "schema", parse(validated.canonicalJson())));
                Map<String, Object> source = new LinkedHashMap<>();
                source.put("applicationId", String.valueOf(binding.source().applicationId()));
                source.put("sourceType", binding.source().sourceType().name());
                source.put("pageId", binding.source().pageId()); source.put("formKey", binding.source().formKey());
                source.put("objectCode", binding.source().objectCode());
                exportedBindings.add(Map.of("source", source, "scene", binding.scene().name(),
                        "templateId", String.valueOf(binding.templateId()), "templateVersionId", String.valueOf(binding.templateVersionId()),
                        "schemaHash", binding.schemaHash(), "isDefault", binding.isDefault(), "sortOrder", binding.sortOrder()));
            }
            exported.add(Map.of("applicationId", String.valueOf(application.getApplicationId()),
                    "applicationVersionId", String.valueOf(application.getId()), "versionNo", application.getVersionNo(),
                    "bindings", exportedBindings, "templates", List.copyOf(definitions.values())));
        }
        return Map.of("schemaVersion", 1, "protocol", "forge-print-export", "applications", exported,
                "runtime", Map.of("protocol", "forge-print", "schemaVersion", 1,
                        "frontendComponent", "@/components/print/runtime/PrintTemplatePicker.vue",
                        "previewRoute", "/print/preview", "backendArtifact", "forge-plugin-print",
                        "providerInterface", "com.mdframe.forge.plugin.print.spi.PrintDataProvider",
                        "dataProviderStatus", "REQUIRES_TARGET_ADAPTER", "resourcePolicy", "AUTHORIZED_FILE_ID"));
    }

    /** 应用级包覆盖聚合子对象，不能只依赖实际生成独立页面的对象。 */
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Map<String, Object> contributeApplication(Long applicationId, List<String> configKeys) {
        List<Map<String, Object>> objects = new ArrayList<>();
        for (String configKey : new LinkedHashSet<>(configKeys)) {
            if (configKey == null || configKey.isBlank()) throw invalid();
            var config = new AiCrudConfig();
            config.setConfigKey(configKey);
            config.setOptions(json.createObjectNode().set("codegen", json.createObjectNode()
                    .put("printApplicationId", String.valueOf(applicationId))).toString());
            objects.add(Map.of("sourceConfigKey", configKey, "printing", contribute(config)));
        }
        return Map.of("protocol", "forge-application-print-export", "schemaVersion", 1,
                "applicationId", String.valueOf(applicationId), "objects", objects);
    }

    private com.fasterxml.jackson.databind.JsonNode parse(String value) {
        try {
            var result = json.readTree(value);
            if (result == null || !result.isObject()) throw invalid();
            return result;
        } catch (Exception ex) { throw invalid(); }
    }

    private BusinessException invalid() { return new BusinessException("打印导出引用或模板内容不完整，请检查应用发布版本"); }
}
